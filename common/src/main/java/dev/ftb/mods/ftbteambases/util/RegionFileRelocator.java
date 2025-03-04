package dev.ftb.mods.ftbteambases.util;

import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.construction.RelocatorTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RegionFileRelocator {
    private static final int MAX_THREADS = 9;
    public static final Path PREGEN_PATH = Path.of(FTBTeamBases.MOD_ID, "pregen");

    private final Map<Path, RelocationData> relocationData = new HashMap<>();
    private final CommandSourceStack source;
    private final boolean force;
    private final Path destDir;
    private final RegionStorageInfo storageInfo;
    private final int totalChunks;
    private final AtomicInteger chunkProgress;
    private final UUID relocatorId;
    @Nullable
    private final UUID playerId;
    private boolean started = false;

    public static RegionFileRelocator create(CommandSourceStack source, String templateId, ResourceKey<Level> dimensionKey, RelocatorTracker.Ticker ticker, XZ regionOffset, boolean force) throws IOException {
        return RelocatorTracker.INSTANCE.add(new RegionFileRelocator(source, templateId, dimensionKey, regionOffset, force), ticker);
    }

    public RegionFileRelocator(CommandSourceStack source, String templateId, ResourceKey<Level> dimensionKey, XZ regionOffset, boolean force) throws IOException {
        this.source = source;
        this.force = force;

        Path pregenPath = RegionFileUtil.getPregenPath(templateId, source.getServer(), "region");

        destDir = RegionFileUtil.getPathForDimension(source.getServer(), dimensionKey, "region");

        storageInfo = new RegionStorageInfo(source.getServer().storageSource.getLevelId(), dimensionKey, "region");

        try (Stream<Path> s = Files.walk(pregenPath).filter(f -> f.getFileName().toString().endsWith(".mca"))) {
            s.forEach(file -> RegionFileUtil.getRegionCoords(file).ifPresent(oldRegionPos ->
                    relocationData.put(file, new RelocationData(oldRegionPos, regionOffset))));
        }

        // a region is 32*32 chunks = 1024 chunks per region to be relocated
        totalChunks = relocationData.size() * 1024;
        chunkProgress = new AtomicInteger(0);
        relocatorId = UUID.randomUUID();
        playerId = source.getPlayer() == null ? null : source.getPlayer().getUUID();
    }

    public CommandSourceStack getSource() {
        return source;
    }

    public UUID getRelocatorId() {
        return relocatorId;
    }

    public @Nullable UUID getPlayerId() {
        return playerId;
    }

    public float getProgress() {
        return totalChunks > 0 ? chunkProgress.floatValue() / totalChunks : 0f;
    }

    public boolean isStarted() {
        return started;
    }

    public Map<Path, RelocationData> getRelocationData() {
        return relocationData;
    }

    public void start(BooleanConsumer onCompleted) {
        if (started) {
            throw new IllegalStateException("relocator already started!");
        }

        // called from main thread
        Path workDir = destDir.resolve("worktmp-" + Thread.currentThread().threadId());
        try {
            if (!force) {
                // ensure none of the dest region MCA files exist yet
                relocationData.values().forEach(data -> {
                    Path destFile = destDir.resolve(data.orig.offsetBy(data.regionOffset).filename());
                    if (Files.exists(destFile)) {
                        throw new IllegalStateException("won't overwrite dest MCA file " + destFile);
                    }
                });
            }
            started = true;
            Files.createDirectories(workDir);
            FTBTeamBases.LOGGER.debug("created work dir {}", workDir);
            CompletableFuture.supplyAsync(() -> runRelocation(workDir)).thenAccept(result -> {
                        FTBTeamBases.LOGGER.debug("finished relocation!");
                        try {
                            FileUtils.deleteDirectory(workDir.toFile());
                        } catch (IOException e) {
                            logError(e, "Can't delete work dir: {}", e.getMessage());
                        }
                        RelocatorTracker.INSTANCE.remove(this);
                        // callback gets run in the main thread via server.tell()
                        source.getServer().tell(
                                new TickTask(source.getServer().getTickCount() + 1, () -> onCompleted.accept(result))
                        );
                    }
            );
        } catch (IOException e) {
            logError(e, "can't create work dir: {}", e.getMessage());
        }
    }

    private boolean runRelocation(Path workDir) {
        // runs in new thread
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        Executor executor = Executors.newFixedThreadPool(MAX_THREADS);

        // each individual region relocation happens in its own thread (subject to MAX_THREADS limit)
        relocationData.forEach((srcFile, data) ->
                futures.add(CompletableFuture.supplyAsync(() -> relocateOneRegion(srcFile, workDir, data), executor))
        );

        // wait till all region relocations are done, then collect success/failure data
        CompletableFuture<List<Boolean>> allRegionsFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());

        try {
            return allRegionsFuture.get().stream().allMatch(x -> x);
        } catch (InterruptedException | ExecutionException e) {
            logError(e, "unexpected concurrency problem: {}", e.getMessage());
            return false;
        }
    }

    private boolean relocateOneRegion(Path fromFile, Path workDir, RelocationData data) {
        RegionCoords newCoords = data.orig.offsetBy(data.regionOffset);
        Path workFile = workDir.resolve(newCoords.filename());
        Path destFile = destDir.resolve(newCoords.filename());

        FTBTeamBases.LOGGER.debug("starting relocation for {} -> {}", fromFile, workFile);

        if (!force && Files.exists(destFile)) {
            FTBTeamBases.LOGGER.error("Not overwriting region file: {}", destFile);
            return false;
        }

        try (RegionFileStorage storage = new RegionFileStorage(storageInfo, workDir, true)) {
            Files.copy(fromFile, workFile);
            FTBTeamBases.LOGGER.debug("copied {} to {}", fromFile, workFile);
            if (updateRegionChunkData(storage, newCoords, data.regionOffset.x(), data.regionOffset.z())) {
                Files.move(workFile, destFile, StandardCopyOption.REPLACE_EXISTING);
                FTBTeamBases.LOGGER.debug("moved {} to {}", workFile, destFile);
                return true;
            }
        } catch (IOException e) {
            logError(e, "IO exception caught: {}", e.getMessage());
        }
        return false;
    }

    private boolean updateRegionChunkData(RegionFileStorage storage, RegionCoords r, int xOff, int zOff) {
        if (xOff == 0 && zOff == 0) {
            return true; // trivial case
        }

        for (int cx = 0; cx < 32; cx++) {
            for (int cz = 0; cz < 32; cz++) {
                ChunkPos newChunkPos = new ChunkPos(r.x() * 32 + cx, r.z() * 32 + cz);
                try {
                    CompoundTag chunkData = storage.read(newChunkPos);
                    if (chunkData != null) {
                        // primary x/z chunkpos
                        chunkData.putInt("xPos", newChunkPos.x);
                        chunkData.putInt("zPos", newChunkPos.z);

                        // structure references
                        CompoundTag s = chunkData.getCompound("structures").getCompound("References");
                        for (String key : s.getAllKeys()) {
                            if (s.get(key) instanceof LongArrayTag a) {
                                ListTag l2 = new ListTag();
                                a.forEach(tag -> {
                                    ChunkPos oldChunkPos = new ChunkPos(tag.getAsLong());
                                    l2.add(LongTag.valueOf(new ChunkPos(oldChunkPos.x + xOff * 32, oldChunkPos.z + zOff * 32).toLong()));
                                });
                                s.put(key, l2);
                            }
                        }

                        // block entities
                        chunkData.getList("block_entities", Tag.TAG_COMPOUND).forEach(tag -> {
                            if (tag instanceof CompoundTag c) {
                                // all block entities
                                updateIfPresent(c, "x", xOff);
                                updateIfPresent(c, "z", zOff);
                                // structure block
                                updateIfPresent(c, "posX", xOff);
                                updateIfPresent(c, "posZ", zOff);
                                // beehive
                                updateIfPresent(c.getCompound("FlowerPos"), "X", xOff);
                                updateIfPresent(c.getCompound("FlowerPos"), "Z", zOff);
                                // end gateway
                                updateIfPresent(c.getCompound("ExitPortal"), "X", xOff);
                                updateIfPresent(c.getCompound("ExitPortal"), "Z", zOff);
                            }
                        });

                        // pending block & fluid ticks (flowing water, fire, leaf decay, etc.)
                        List.of("block_ticks", "fluid_ticks")
                                .forEach(what -> chunkData.getList(what, Tag.TAG_COMPOUND).forEach(tag -> {
                                            if (tag instanceof CompoundTag c) {
                                                updateIfPresent(c, "x", xOff);
                                                updateIfPresent(c, "z", zOff);
                                            }
                                        })
                                );

                        storage.write(newChunkPos, chunkData);
                    }
                    chunkProgress.getAndIncrement();
                } catch (IOException e) {
                    logError(e,"Can't update chunk pos data for region {}: {}", r, e.getMessage());
                    return false;
                }
            }
        }
        FTBTeamBases.LOGGER.debug("updated chunk NBT for region {}", r);
        return true;
    }

    private static void updateIfPresent(CompoundTag tag, String key, int offset) {
        // 512: region offset -> block offset
        if (tag.contains(key, Tag.TAG_INT)) tag.putInt(key, tag.getInt(key) + offset * 512);
    }

    private static void logError(Exception e, String msg, Object... args) {
        FTBTeamBases.LOGGER.error("{}: " + msg, e.getClass().getSimpleName(), args);
    }

    public record RelocationData(RegionCoords orig, XZ regionOffset) {
    }
}

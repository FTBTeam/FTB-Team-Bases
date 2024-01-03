package dev.ftb.mods.ftbteambases.util;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import joptsimple.internal.Strings;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RegionFileRelocator {
    public static final Path PREGEN_PATH = Path.of(FTBTeamBases.MOD_ID, "pregen");

    private static final Pattern MCA_FILE = Pattern.compile("^r\\.(\\d+)\\.(\\d+)\\.mca$", Pattern.CASE_INSENSITIVE);

    public static boolean relocateRegionTemplate(MinecraftServer server, ResourceLocation templateId, ResourceKey<Level> dimensionKey, int xOff, int zOff, boolean force) throws IOException {
        Path rootDir = server.getServerDirectory().toPath();
        Path resDir = Path.of(templateId.getNamespace(), templateId.getPath().split("/"));
        Path pregenDir = rootDir.resolve(PREGEN_PATH).resolve(resDir);

        Path destDir = levelKeyToPath(server.getWorldPath(LevelResource.ROOT), dimensionKey, "region");
        Path workDir = destDir.resolve("worktmp");

        // Copy MCA files from the template dir to the temp workdir, with updated filenames
        //   and rewrite chunk pos data within the region file to match the new region
        Files.createDirectories(workDir);
        FTBTeamBases.LOGGER.debug("created work dir {}", workDir);

        MutableInt good = new MutableInt(0);
        MutableInt bad = new MutableInt(0);

        try (RegionFileStorage storage = new RegionFileStorage(workDir, true)) {
            try (Stream<Path> s = Files.walk(pregenDir).filter(f -> f.getFileName().toString().endsWith(".mca"))) {
                s.forEach(file -> getRegionCoords(file).ifPresent(oldRegionPos -> {
                    if (copyAndRename(file, oldRegionPos, workDir, xOff, zOff)
                            && performRelocation(oldRegionPos.offsetBy(xOff, zOff), storage, xOff, zOff)) {
                        good.increment();
                    } else {
                        bad.increment();
                    }
                }));
            }
        }

        if (bad.toInteger() > 0) return false;

        Map<Path,Path> toMove = new HashMap<>();
        Set<Path> exists = new HashSet<>();
        try (Stream<Path> s = Files.walk(workDir).filter(f -> f.getFileName().toString().endsWith(".mca"))) {
            s.forEach(file -> {
                Path destFile = destDir.resolve(file.getFileName());
                if (!force && Files.exists(destFile)) {
                    exists.add(destFile);
                }
                toMove.put(file, destFile);
            });
        }

        if (!force && !exists.isEmpty()) {
            FTBTeamBases.LOGGER.error("Not overwriting {} region file(s): {}",
                    exists.size(), Strings.join(exists.stream().map(Path::toString).toList(), ","));
            bad.increment();
        } else {
            toMove.forEach((from, to) -> {
                try {
                    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                    FTBTeamBases.LOGGER.debug("moved {} to {}", from, to);
                } catch (IOException e) {
                    logError(e, "can't move {} to {}: {}", from, to, e);
                    bad.increment();
                }
            });
        }

        FileUtils.deleteDirectory(workDir.toFile());
        FTBTeamBases.LOGGER.debug("deleted work dir {}", workDir);

        return good.toInteger() > 0 && bad.toInteger() == 0;
    }

    private static boolean copyAndRename(Path file, RegionCoords r, Path workDir, int xOff, int zOff) {
        try {
            Path dest = workDir.resolve(String.format("r.%d.%d.mca", r.x() + xOff, r.z() + zOff));
            Files.copy(file, dest);
            FTBTeamBases.LOGGER.debug("copied {} to {}", file, dest);
            return true;
        } catch (IOException e) {
            logError(e,"Can't copy {} to {}: {}", file, workDir, e.getMessage());
            return false;
        }
    }

    private static boolean performRelocation(RegionCoords r, RegionFileStorage storage, int xOff, int zOff) {
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

    private static Optional<RegionCoords> getRegionCoords(Path file) {
        Matcher m = MCA_FILE.matcher(file.getFileName().toString());
        if (m.matches()) {
            try {
                int rx0 = Integer.parseInt(m.group(1));
                int rz0 = Integer.parseInt(m.group(2));
                return Optional.of(new RegionCoords(rx0, rz0));
            } catch (NumberFormatException ignored) {
            }
        }
        return Optional.empty();
    }

    private static Path levelKeyToPath(Path levelDataDir, ResourceKey<Level> levelKey, String subDirectory) {
        if (Level.OVERWORLD.equals(levelKey)) {
            return levelDataDir.resolve(subDirectory);
        } else if (Level.NETHER.equals(levelKey)) {
            return levelDataDir.resolve("DIM-1").resolve(subDirectory);
        } else if (Level.END.equals(levelKey)) {
            return levelDataDir.resolve("DIM1").resolve(subDirectory);
        } else {
            return levelDataDir.resolve("dimensions").resolve(levelKey.location().getNamespace()).resolve(levelKey.location().getPath()).resolve(subDirectory);
        }
    }

    private record RegionCoords(int x, int z) {
        public RegionCoords offsetBy(int xOff, int zOff) {
            return new RegionCoords(x + xOff, z + zOff);
        }
    }

    private static void logError(Exception e, String msg, Object... args) {
        FTBTeamBases.LOGGER.error("{}: " + msg, e.getClass().getSimpleName(), args);
    }
}

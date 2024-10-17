package dev.ftb.mods.ftbteambases.data.purging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.bases.ArchivedBaseDetails;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import dev.ftb.mods.ftbteambases.util.RegionFileUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record PendingPurgeData(Map<String,PurgeRecord> pending) {
    private static final Path PENDING_PATH = Path.of(FTBTeamBases.MOD_ID, "pending_purge.json");

    private static final Codec<Map<String,PurgeRecord>> MAP_CODEC
            = Codec.unboundedMap(Codec.STRING, PurgeRecord.CODEC).xmap(HashMap::new, Map::copyOf);

    private static final Codec<PendingPurgeData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            MAP_CODEC.fieldOf("pending").forGetter(PendingPurgeData::pending)
    ).apply(inst, PendingPurgeData::new));

    private static PendingPurgeData empty() { return new PendingPurgeData(new HashMap<>()); }

    @NotNull
    private static Path getPendingFilePath(MinecraftServer server) {
        return server.getServerDirectory().resolve(PENDING_PATH);
    }

    public static PendingPurgeData readFromFile(MinecraftServer server) {
        Path srcPath = getPendingFilePath(server);
        if (!Files.exists(srcPath)) {
            return empty();
        }

        try (Stream<String> str = Files.lines(srcPath)) {
            JsonElement json = JsonParser.parseString(str.collect(Collectors.joining("\n")));
            return CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> FTBTeamBases.LOGGER.error("Can't parse {}: {}", PENDING_PATH, err))
                    .orElse(empty());
        } catch (IOException e) {
            FTBTeamBases.LOGGER.error("Can't read {}: {}", PENDING_PATH, e.getMessage());
            return empty();
        }
    }

    boolean writeToFile(MinecraftServer server) {
        MutableBoolean res = new MutableBoolean(false);

        CODEC.encodeStart(JsonOps.INSTANCE, this)
                .resultOrPartial(err -> FTBTeamBases.LOGGER.error("Can't encode pending data: {}", err))
                .ifPresent(jsonElement -> {
                    try {
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        Files.writeString(getPendingFilePath(server), gson.toJson(jsonElement),
                                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        res.setTrue();
                    } catch (IOException e) {
                        FTBTeamBases.LOGGER.error("Can't write {}: {}", PENDING_PATH, e.getMessage());
                    }
                });
        return res.booleanValue();
    }

    PendingPurgeData add(Collection<ArchivedBaseDetails> details) {
        details.forEach(a -> pending.put(a.archiveId(), PurgeRecord.of(a)));
        return this;
    }

    PendingPurgeData remove(Collection<ArchivedBaseDetails> details) {
        details.forEach(a -> pending.remove(a.archiveId()));
        return this;
    }

    public PendingPurgeData remove(String id) {
        pending.remove(id);
        return this;
    }

    PendingPurgeData clearPending() {
        pending.clear();
        return this;
    }

    record PurgeRecord(ResourceLocation dimensionId, RegionExtents extents) {
        public static final Codec<PurgeRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("dimensionId").forGetter(PurgeRecord::dimensionId),
                RegionExtents.CODEC.fieldOf("extents").forGetter(PurgeRecord::extents)).apply(inst, PurgeRecord::new));

        private static PurgeRecord of(ArchivedBaseDetails details) {
            return new PurgeRecord(details.dimension().location(), details.extents());
        }

        void doPurge(MinecraftServer server, boolean simulate) {
            Path path = RegionFileUtil.getPathForDimension(server, ResourceKey.create(Registries.DIMENSION, dimensionId));

            if (DimensionUtils.isPrivateTeamDimension(dimensionId)) {
                // private dimension: just delete the dimension
                //   (and mark it for unregistration; it's also saved in level.dat)
                try {
                    if (!simulate) {
                        FileUtils.deleteDirectory(path.toFile());
                    }
                    FTBTeamBases.LOGGER.info("purged archived base dimension dir: {}", path);
                } catch (IOException e) {
                    FTBTeamBases.LOGGER.error("can't purge archived base dimension dir {}", path);
                }
            } else if (dimensionId.getNamespace().equals(FTBTeamBases.MOD_ID)) {
                // shared dimension: only delete the MCA files for the base extents
                List.of("region", "entities", "poi").forEach(subDir ->
                        extents.files(path.resolve(subDir)).forEach(file -> {
                            try {
                                if (!simulate) {
                                    Files.deleteIfExists(file);
                                    FTBTeamBases.LOGGER.info("purged archived base MCA file: {}", file);
                                }
                            } catch (IOException e) {
                                FTBTeamBases.LOGGER.error("can't purge archived base MCA file {}", file);
                            }
                        }));
            }
        }
    }

}

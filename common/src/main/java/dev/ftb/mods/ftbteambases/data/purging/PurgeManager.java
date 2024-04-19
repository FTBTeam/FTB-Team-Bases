package dev.ftb.mods.ftbteambases.data.purging;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.command.CommandUtils;
import dev.ftb.mods.ftbteambases.data.bases.ArchivedBaseDetails;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum PurgeManager {
    INSTANCE;

    private PendingPurgeData data;
    private MinecraftServer server;
    private final List<String> purgedArchiveIds = new ArrayList<>();

    private PendingPurgeData getData(MinecraftServer server) {
        if (data == null) {
            data = PendingPurgeData.readFromFile(server);
            this.server = server;
        }
        return data;
    }

    public void onInit(MinecraftServer server) {
        this.server = server;
    }

    public void onShutdown() {
        data = null;
        server = null;
    }

    public boolean clearPending() {
        return getData(server).clearPending().writeToFile(server);
    }

    public void checkForPurges(MinecraftServer server) {
        purgedArchiveIds.clear();

        List<ResourceLocation> purgedDims = new ArrayList<>();

        getData(this.server).pending().forEach((id, purgeRecord) -> {
            purgeRecord.doPurge(this.server, false);
            purgedArchiveIds.add(id);
            if (DimensionUtils.isPrivateTeamDimension(purgeRecord.dimensionId())) {
                purgedDims.add(purgeRecord.dimensionId());
            }
        });

        if (!purgedDims.isEmpty()) {
            removeLevelsFromLevelDat(server, purgedDims);
        }

        clearPending();
    }

    public boolean addPending(Collection<ArchivedBaseDetails> details) {
        return getData(server).add(details).writeToFile(server);
    }

    public boolean removePending(Collection<ArchivedBaseDetails> details) {
        return getData(server).remove(details).writeToFile(server);
    }

    public boolean removePending(String id) throws CommandSyntaxException {
        if (!getData(server).pending().containsKey(id)) {
            throw CommandUtils.PURGE_NOT_FOUND.create(id);
        }
        return getData(server).remove(id).writeToFile(server);
    }

    public Collection<String> getPendingIds() {
        return getData(server).pending().keySet();
    }

    public void cleanUpPurgedArchives(BaseInstanceManager res) {
        if (!purgedArchiveIds.isEmpty()) {
            purgedArchiveIds.forEach(res::removeArchivedBase);
            purgedArchiveIds.clear();
        }
    }

    private static void removeLevelsFromLevelDat(MinecraftServer server, List<ResourceLocation> ids) {
        // Dimension data is stored in level.dat as well as directories on disk,
        //   so it's not enough just to delete the dimension directory;
        //   the dimension must also be removed from level.dat
        // NBT structure: Data -> WorldGenSettings -> dimensions -> <dimension_ids>
        Path worldDir = server.getWorldPath(LevelResource.LEVEL_DATA_FILE);
        File datFile = worldDir.toFile();

        try {
            CompoundTag tag = NbtIo.readCompressed(datFile);

            CompoundTag tag1 = tag.getCompound("Data").getCompound("WorldGenSettings").getCompound("dimensions");

            ids.forEach(id -> tag1.remove(id.toString()));

            File tempFile = File.createTempFile("tmp-level", ".dat", worldDir.toFile());
            NbtIo.writeCompressed(tag, tempFile);
            File backupFile = new File(server.getServerDirectory(), "level.dat_old");
            Util.safeReplaceFile(datFile, tempFile, backupFile);

            FTBTeamBases.LOGGER.info("removed {} purged base dimension(s) from level.dat", ids.size());
        } catch (IOException e) {
            FTBTeamBases.LOGGER.error("can't update level.dat to remove purged ids: {} / {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}

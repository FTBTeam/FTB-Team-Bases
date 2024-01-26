package dev.ftb.mods.ftbteambases.util;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class InitialPregen {
    private static final Path PREGEN_INITIAL_PATH = Path.of(FTBTeamBases.MOD_ID, "pregen_initial");

    private static final List<String> INITIAL_SUBDIRS = List.of("region", "entities", "poi", "DIM1", "DIM-1");

    public static boolean maybeDoInitialPregen(MinecraftServer server) {
        Path initialPath = server.getServerDirectory().toPath().resolve(PREGEN_INITIAL_PATH);
        Path worldPath = server.getWorldPath(LevelResource.ROOT);
        if (Files.isDirectory(initialPath) && !Files.isDirectory(worldPath.resolve("region"))) {
            // looks like a brand-new world, just created - copy over any pregen MCA files for overworld/nether/end if they exist
            for (String subDir : INITIAL_SUBDIRS) {
                Path srcDir = initialPath.resolve(subDir);
                Path destDir = worldPath.resolve(subDir);
                if (Files.isDirectory(srcDir) && !Files.isDirectory(destDir)) {
                    try {
                        FileUtils.copyDirectory(srcDir.toFile(), destDir.toFile());
                        int[] pos = ServerConfig.LOBBY_SPAWN.get();
                        if (pos.length == 3) {
                            BaseInstanceManager.get(server).setLobbySpawnPos(new BlockPos(pos[0], pos[1], pos[2]));
                        } else {
                            FTBTeamBases.LOGGER.error("invalid lobby spawn pos! expected 3 integers, got {}", pos.length);
                        }
                        FTBTeamBases.LOGGER.info("Copied initial pregen MCA files from {} to {}", srcDir, destDir);
                    } catch (IOException e) {
                        FTBTeamBases.LOGGER.error("Failed to copy initial MCA files from {} to {}: {}", srcDir, destDir, e.getMessage());
                    }
                }
            }
            return true;
        }
        return false;
    }
}

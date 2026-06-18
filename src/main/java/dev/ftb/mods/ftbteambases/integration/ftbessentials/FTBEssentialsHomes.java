package dev.ftb.mods.ftbteambases.integration.ftbessentials;

import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.SavedTeleportManager;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class FTBEssentialsHomes {
    static void addHome(ServerPlayer player, ResourceKey<Level> dimension, BlockPos blockPos) {
        String homeName = ServerConfig.BASE_HOME_NAME.get();
        if (!homeName.isEmpty()) {
            FTBEPlayerData.getOrCreate(player).ifPresent(data -> {
                try {
                    data.homeManager().addDestination(homeName, new TeleportPos(dimension, blockPos), player);
                } catch (SavedTeleportManager.TooManyDestinationsException e) {
                    // shouldn't happen!
                    FTBTeamBases.LOGGER.error("couldn't add ftb essentials home for {}: {}", player.getUUID(), e.getMessage());
                }
            });
        }
    }

    static void delHome(ServerPlayer player) {
        String homeName = ServerConfig.BASE_HOME_NAME.get();
        if (!homeName.isEmpty()) {
            FTBEPlayerData.getOrCreate(player).ifPresent(data ->
                    data.homeManager().deleteDestination(homeName)
            );
        }
    }
}

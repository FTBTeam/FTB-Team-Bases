package dev.ftb.mods.ftbteambases.integration;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

public class FTBChunksIntegration {
    public static void maybeAutoClaimLobby(ServerLevel level) {
        if (ModList.get().isLoaded("ftbchunks")) {
            AutoClaiming.handleLobbyAutoclaiming(level);
        }
    }
}

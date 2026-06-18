package dev.ftb.mods.ftbteambases.integration.ftbessentials;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

public class FTBEssentialsIntegration {
    private static boolean ftbEssentialsLoaded;

    public static void init() {
        ftbEssentialsLoaded = ModList.get().isLoaded("ftbessentials");
    }

    public static void addFTBEssentialsHome(ServerPlayer player, ResourceKey<Level> dimension, BlockPos blockPos) {
        if (ftbEssentialsLoaded) {
            FTBEssentialsHomes.addHome(player, dimension, blockPos);
        }
    }

    public static void deleteFTBEssentialsHome(ServerPlayer player) {
        if (ftbEssentialsLoaded) {
            FTBEssentialsHomes.delHome(player);
        }
    }
}

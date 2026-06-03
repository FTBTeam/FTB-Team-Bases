package dev.ftb.mods.ftbteambases.util;

import com.google.common.math.Stats;
import dev.ftb.mods.ftbteambases.integration.CuriosIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.ClockAdjustment;
import org.jspecify.annotations.Nullable;

public class MiscUtil {
    public static String blockPosStr(BlockPos pos) {
        return String.format("[%d,%d,%d]", pos.getX(), pos.getY(), pos.getZ());
    }

    public static void setOverworldTime(MinecraftServer server, ClockAdjustment adjustment) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            overworld.dimensionType().defaultClock().ifPresent(worldClockHolder ->
                    adjustment.apply(overworld.getServer().clockManager(), worldClockHolder));

            if (overworld.getGameRules().get(GameRules.ADVANCE_WEATHER) && overworld.isRaining()) {
                overworld.resetWeatherCycle();
            }
        }
    }

    public static double getTickTime(MinecraftServer server, ResourceKey<Level> key) {
        long[] times = server.getTickTime(key);
        if (times == null) {
            times = new long[] { 0L };
        }
        return Stats.meanOf(times) * 1.0E-6;
    }

    public static void clearPlayerInventory(ServerPlayer serverPlayer) {
        serverPlayer.getInventory().clearOrCountMatchingItems(arg -> true, -1, serverPlayer.inventoryMenu.getCraftSlots());
        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.slotsChanged(serverPlayer.getInventory());
        if (ModList.get().isLoaded("curios")) {
            CuriosIntegration.clearCurios(serverPlayer);
        }
    }

    @Nullable
    public static BlockPos getRespawnPosition(ServerPlayer player) {
        return player.getRespawnConfig() == null ? null : player.getRespawnConfig().respawnData().pos();
    }

    public static ResourceKey<Level> getRespawnDimension(ServerPlayer player) {
        return player.getRespawnConfig() == null ? Level.OVERWORLD : player.getRespawnConfig().respawnData().dimension();
    }
}

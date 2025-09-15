package dev.ftb.mods.ftbteambases.util.neoforge;

import dev.ftb.mods.ftbteambases.neoforge.TeamBasesPortalEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

public class MiscUtilImpl {
    public static double getTickTime(MinecraftServer server, ResourceKey<Level> key) {
        long[] times = server.getTickTime(key);
        if (times == null) times = new long[] { 0L };
        return (double) mean(times) * 1.0E-6;
    }

    private static long mean(long[] values) {
        long sum = 0L;
        for (long v : values) {
            sum += v;
        }
        return sum / (long)values.length;
    }

    public static Component postPortalEvent(ServerPlayer player) {
        TeamBasesPortalEvent event = new TeamBasesPortalEvent(player);
        return NeoForge.EVENT_BUS.post(event).isCanceled() ? event.getCancellationReason() : null;
    }
}

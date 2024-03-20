package dev.ftb.mods.ftbteambases.data.construction;

import dev.ftb.mods.ftbteambases.util.RegionFileRelocator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public enum RelocatorTracker {
    INSTANCE;

    private final Map<UUID,Entry> map = new ConcurrentHashMap<>();

    public RegionFileRelocator add(RegionFileRelocator relocator, Ticker ticker) {
        if (map.containsKey(relocator.getRelocatorId())) {
            throw new IllegalStateException("relocator id already added! " + relocator.getRelocatorId());
        }
        map.put(relocator.getRelocatorId(), new Entry(relocator, ticker));
        return relocator;
    }

    public void remove(RegionFileRelocator relocator) {
        map.remove(relocator.getRelocatorId());
    }

    public void tick(MinecraftServer server) {
        if (!map.isEmpty() && server.getTickCount() % 5 == 0) {
            map.forEach((id, entry) -> {
                if (entry.relocator().isStarted()) {
                    UUID playerId = entry.relocator().getPlayerId();
                    ServerPlayer player = playerId == null ? null : server.getPlayerList().getPlayer(playerId);
                    entry.ticker().tick(player, entry.relocator());
                }
            });
        }
    }

    private record Entry(RegionFileRelocator relocator, Ticker ticker) {
    }

    @FunctionalInterface
    public interface Ticker {
        void tick(@Nullable ServerPlayer player, RegionFileRelocator relocator);
    }
}

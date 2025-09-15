package dev.ftb.mods.ftbteambases.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;

public class MiscUtil {
    public static String blockPosStr(BlockPos pos) {
        return String.format("[%d,%d,%d]", pos.getX(), pos.getY(), pos.getZ());
    }

    public static void setOverworldTime(MinecraftServer server, long newTime) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            overworld.setDayTime(newTime);
            if (overworld.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && overworld.isRaining()) {
                if (overworld.getLevelData() instanceof ServerLevelData data) {
                    data.setRainTime(0);
                    data.setRaining(false);
                    data.setThunderTime(0);
                    data.setThundering(false);
                }
            }
        }
    }

    @ExpectPlatform
    public static double getTickTime(MinecraftServer server, ResourceKey<Level> key) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static Component postPortalEvent(ServerPlayer player) {
        throw new AssertionError();
    }
}

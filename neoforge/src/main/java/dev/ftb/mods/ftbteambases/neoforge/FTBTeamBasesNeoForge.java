package dev.ftb.mods.ftbteambases.neoforge;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.command.arguments.BaseDefinitionArgument;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.util.MiscUtil;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FTBTeamBases.MOD_ID)
public class FTBTeamBasesNeoForge {
    public static final DeferredRegister<ArgumentTypeInfo<?,?>> ARGUMENT_TYPES
            = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, FTBTeamBases.MOD_ID);

    private static final DeferredHolder<ArgumentTypeInfo<?,?>, SingletonArgumentInfo<BaseDefinitionArgument>> PREBUILT_COMMAND
            = ARGUMENT_TYPES.register("prebuilt", () -> ArgumentTypeInfos.registerByClass(
            BaseDefinitionArgument.class, SingletonArgumentInfo.contextFree(BaseDefinitionArgument::create)));

    public FTBTeamBasesNeoForge(IEventBus modEventBus) {
        ARGUMENT_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onSleepFinished);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onPlayerRespawn);

        FTBTeamBases.init();
    }

    private void onSleepFinished(final SleepFinishedTimeEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().location().getNamespace().equals(FTBTeamBases.MOD_ID)) {
            // player has slept in a dynamic dimension
            // sleeping in dynamic dimensions doesn't work in general: https://bugs.mojang.com/browse/MC-188578
            // best we can do here is advance the overworld time
            MiscUtil.setOverworldTime(level.getServer(), event.getNewTime());
        }
    }

    private void onPlayerRespawn(PlayerRespawnPositionEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server != null) {
            BaseInstanceManager mgr = BaseInstanceManager.get(server);
            ServerLevel lobbyLvl = server.getLevel(ServerConfig.lobbyDimension().orElse(Level.OVERWORLD));
            if (lobbyLvl != null) {
                event.setDimensionTransition(new DimensionTransition(
                        lobbyLvl, Vec3.atCenterOf(mgr.getLobbySpawnPos()), Vec3.ZERO,
                        ServerConfig.LOBBY_PLAYER_YAW.get().floatValue(), 0f, DimensionTransition.DO_NOTHING
                ));
            }
        }
    }
}

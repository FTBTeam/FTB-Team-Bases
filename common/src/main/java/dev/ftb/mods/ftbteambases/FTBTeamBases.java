package dev.ftb.mods.ftbteambases;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil;
import dev.ftb.mods.ftbteambases.command.*;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.construction.BaseConstructionManager;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.construction.RelocatorTracker;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import dev.ftb.mods.ftbteambases.data.purging.PurgeManager;
import dev.ftb.mods.ftbteambases.net.FTBTeamBasesNet;
import dev.ftb.mods.ftbteambases.net.SyncBaseTemplatesMessage;
import dev.ftb.mods.ftbteambases.net.VoidTeamDimensionMessage;
import dev.ftb.mods.ftbteambases.registry.ModBlocks;
import dev.ftb.mods.ftbteambases.registry.ModWorldGen;
import dev.ftb.mods.ftbteambases.util.*;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

public class FTBTeamBases {
    public static final String MOD_ID = "ftbteambases";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final ResourceLocation NO_TEMPLATE_ID = rl("none");
    public static final ResourceLocation SHARED_DIMENSION_ID = rl("bases");

    public static void init() {
        try {
            Files.createDirectories(RegionFileRelocator.PREGEN_PATH);
        } catch (IOException e) {
            LOGGER.error("can't create {}: {}", RegionFileRelocator.PREGEN_PATH, e.getMessage());
        }

        ModWorldGen.init();
        ModBlocks.init();

        FTBTeamBasesNet.init();

        LifecycleEvent.SERVER_BEFORE_START.register(FTBTeamBases::serverBeforeStart);
        LifecycleEvent.SERVER_STARTING.register(FTBTeamBases::serverStarting);
        LifecycleEvent.SERVER_STARTED.register(FTBTeamBases::serverStarted);
        LifecycleEvent.SERVER_STOPPING.register(FTBTeamBases::serverStopping);
        LifecycleEvent.SERVER_LEVEL_LOAD.register(FTBTeamBases::onLevelLoad);

        TickEvent.SERVER_POST.register(FTBTeamBases::onServerTick);

        CommandRegistrationEvent.EVENT.register(CommandUtils::registerCommands);

        TeamEvent.PLAYER_JOINED_PARTY.register(TeamEventListener::teamPlayerJoin);
        TeamEvent.PLAYER_LEFT_PARTY.register(TeamEventListener::teamPlayerLeftParty);

        EntityEvent.ADD.register(FTBTeamBases::playerJoinLevel);

        PlayerEvent.PLAYER_JOIN.register(FTBTeamBases::playerEnterServer);
        PlayerEvent.CHANGE_DIMENSION.register(FTBTeamBases::playerChangedDimension);

        ReloadListenerRegistry.register(PackType.SERVER_DATA, new BaseDefinitionManager.ReloadListener());
    }

    private static void onServerTick(MinecraftServer server) {
        RelocatorTracker.INSTANCE.tick(server);
        BaseConstructionManager.INSTANCE.tick(server);
        DynamicDimensionManager.unregisterScheduledDimensions(server);
    }

    private static void serverBeforeStart(MinecraftServer server) {
        var configPath = server.getWorldPath(ConfigUtil.SERVER_CONFIG_DIR);
        ConfigUtil.loadDefaulted(ServerConfig.CONFIG, configPath, FTBTeamBases.MOD_ID);

        PurgeManager.INSTANCE.onInit(server);
        PurgeManager.INSTANCE.checkForPurges(server);
    }

    private static void serverStarting(MinecraftServer server) {
        FTBTeamsAPI.api().setPartyCreationFromAPIOnly(true);
    }

    private static void serverStarted(MinecraftServer server) {
        ServerLevel level = server.getLevel(OVERWORLD);
        if (level == null) {
            LOGGER.warn("Missed spawn reset event due to overworld being null?!");
            return;
        }

        BaseInstanceManager mgr = BaseInstanceManager.get(server);
        if (mgr.isLobbyCreated() && !level.getSharedSpawnPos().equals(mgr.getLobbySpawnPos())) {
            level.setDefaultSpawnPos(mgr.getLobbySpawnPos(), 180F);
            LOGGER.info("Updating overworld spawn pos to the lobby location: " + mgr.getLobbySpawnPos());
        }
    }

    private static void serverStopping(MinecraftServer server) {
        PurgeManager.INSTANCE.onShutdown();
    }

    private static void onLevelLoad(ServerLevel serverLevel) {
        if (serverLevel.dimension() == OVERWORLD) {
            if (!InitialPregen.maybeDoInitialPregen(serverLevel.getServer())) {
                handleLobbySetup(serverLevel);
            }
        }
    }

    private static void playerEnterServer(ServerPlayer player) {
        SyncBaseTemplatesMessage.syncTo(player);
    }

    private static EventResult playerJoinLevel(Entity entity, Level level) {
        if (entity instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension().equals(OVERWORLD) && player.getRespawnDimension().equals(OVERWORLD)) {
                // Assume this is their first time joining the world; otherwise their respawn dimension would be their own dimension
                // Send them to the lobby, and set their respawn position there
                BlockPos lobbySpawnPos = BaseInstanceManager.get(player.server).getLobbySpawnPos();
                if (player.getRespawnPosition() == null || !player.getRespawnPosition().equals(lobbySpawnPos)) {
                    player.setRespawnPosition(serverLevel.dimension(), lobbySpawnPos, -180, true, false);
                    player.teleportTo(serverLevel, lobbySpawnPos.getX(), lobbySpawnPos.getY(), lobbySpawnPos.getZ(), -180F, -10F);
                }
            }

            if (DimensionUtils.isVoidChunkGen(serverLevel.getChunkSource().getGenerator())) {
                VoidTeamDimensionMessage.syncTo(player);
            }

            switchGameMode(player, player.level().dimension());
        }
        return EventResult.pass();
    }

    private static void playerChangedDimension(ServerPlayer player, ResourceKey<Level> oldLevel, ResourceKey<Level> newLevel) {
        switchGameMode(player, newLevel);

        handleNetherTravel(player, oldLevel, newLevel);
    }

    private static void switchGameMode(ServerPlayer player, ResourceKey<Level> newLevel) {
        GameType lobbyGameMode = ServerConfig.LOBBY_GAME_MODE.get();

        if (newLevel.equals(OVERWORLD) && player.gameMode.getGameModeForPlayer() != lobbyGameMode && player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
            player.setGameMode(lobbyGameMode);
        } else if (!newLevel.equals(OVERWORLD) && player.gameMode.getGameModeForPlayer() == lobbyGameMode) {
            player.setGameMode(GameType.SURVIVAL);
        }
    }

    private static void handleNetherTravel(ServerPlayer player, ResourceKey<Level> oldLevel, ResourceKey<Level> newLevel) {
        if (player.isOnPortalCooldown()) {
            var mgr = BaseInstanceManager.get(player.server);

            if (newLevel.equals(NETHER)) {
                // travelling to the Nether: if from our team dimension, store the player's location (in the from-dimension!) to later return there
                BlockPos portalPos = oldLevel.location().getNamespace().equals(FTBTeamBases.MOD_ID) ?
                        BlockPos.containing(player.xOld, player.yOld, player.zOld) :
                        null;
                mgr.setPlayerNetherPortalLoc(player, portalPos);
            } else if (oldLevel.equals(NETHER) && newLevel.equals(OVERWORLD)) {
                // returning from the Nether: intercept this and send the player to their base portal instead
                //   (or the base spawn point if for some reason we don't have their portal return point stored)
                mgr.getBaseForPlayer(player).ifPresentOrElse(base -> {
                    ResourceKey<Level> teamDim = base.dimension();
                    BlockPos portalPos = mgr.getPlayerNetherPortalLoc(player).orElse(base.spawnPos());
                    DimensionUtils.teleport(player, teamDim, portalPos);
                }, () -> mgr.teleportToLobby(player));
            }
        }
    }

    private static void handleLobbySetup(ServerLevel serverLevel) {
        BaseInstanceManager mgr = BaseInstanceManager.get(serverLevel.getServer());
        if (!mgr.isLobbyCreated()) {
            try {
                ResourceLocation lobbyLocation = ServerConfig.lobbyLocation();

                // paste the lobby structure into the overworld
                StructureTemplate lobby = serverLevel.getStructureManager().getOrCreate(lobbyLocation);
                StructurePlaceSettings placeSettings = DimensionUtils.makePlacementSettings(lobby);
                BlockPos spawnPos = DimensionUtils.locateSpawn(lobby).orElse(BlockPos.ZERO);
                BlockPos lobbyLoc = BlockPos.ZERO.offset(-(lobby.getSize().getX() / 2), ServerConfig.LOBBY_Y_POS.get(), -(lobby.getSize().getZ() / 2));
                BlockPos playerSpawn = spawnPos.offset(lobbyLoc.getX(), lobbyLoc.getY(), lobbyLoc.getZ());

                lobby.placeInWorld(serverLevel, lobbyLoc, lobbyLoc, placeSettings, serverLevel.random, Block.UPDATE_ALL);

                mgr.setLobbySpawnPos(playerSpawn);
                mgr.setLobbyCreated(true);

                serverLevel.removeBlock(playerSpawn, false);

                LOGGER.info("Spawned lobby structure @ {}", lobbyLoc);
            } catch (ResourceLocationException e) {
                LOGGER.error("invalid lobby resource location: " + e.getMessage());
            }
        }
    }

    public static ResourceLocation rl(String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}


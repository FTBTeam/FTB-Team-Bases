package dev.ftb.mods.ftbteambases;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil;
import dev.ftb.mods.ftbteambases.command.CommandUtils;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.construction.BaseConstructionManager;
import dev.ftb.mods.ftbteambases.data.construction.RelocatorTracker;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import dev.ftb.mods.ftbteambases.data.purging.PurgeManager;
import dev.ftb.mods.ftbteambases.net.FTBTeamBasesNet;
import dev.ftb.mods.ftbteambases.net.SyncBaseTemplatesMessage;
import dev.ftb.mods.ftbteambases.net.VoidTeamDimensionMessage;
import dev.ftb.mods.ftbteambases.registry.ModBlocks;
import dev.ftb.mods.ftbteambases.registry.ModSounds;
import dev.ftb.mods.ftbteambases.registry.ModWorldGen;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteambases.util.DynamicDimensionManager;
import dev.ftb.mods.ftbteambases.util.LobbyPregen;
import dev.ftb.mods.ftbteambases.util.RegionFileRelocator;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
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
import org.jetbrains.annotations.Nullable;

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
        ModSounds.init();

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
        TeamEvent.DELETED.register(TeamEventListener::teamDeleted);

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
        ServerConfig.lobbyDimension().ifPresent(dim -> {
            // only override overworld default spawn pos if the lobby is actually in the overworld
            if (dim.equals(OVERWORLD)) {
                ServerLevel level = server.getLevel(OVERWORLD);
                if (level == null) {
                    LOGGER.error("Missed spawn reset event due to overworld being null?!");
                    return;
                }

                BaseInstanceManager mgr = BaseInstanceManager.get(server);
                if (mgr.isLobbyCreated() && !level.getSharedSpawnPos().equals(mgr.getLobbySpawnPos())) {
                    level.setDefaultSpawnPos(mgr.getLobbySpawnPos(), 180F);
                    LOGGER.info("Updating overworld spawn pos to the lobby spawn pos: {}", mgr.getLobbySpawnPos());
                }
            }
        });
    }

    private static void serverStopping(MinecraftServer server) {
        PurgeManager.INSTANCE.onShutdown();
    }

    private static void onLevelLoad(ServerLevel serverLevel) {
        if (serverLevel.dimension() == OVERWORLD) {
            if (LobbyPregen.maybePregenLobby(serverLevel.getServer())) {
                return;
            }
        }

        ServerConfig.lobbyDimension().ifPresent(rl -> {
            if (serverLevel.dimension().equals(rl)) {
                maybeCreateLobbyFromStructure(serverLevel);
            }
        });
    }

    private static void playerEnterServer(ServerPlayer player) {
        SyncBaseTemplatesMessage.syncTo(player);
        BaseInstanceManager.get().checkForOrphanedPlayer(player);
    }

    private static EventResult playerJoinLevel(Entity entity, Level level) {
        if (entity instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
            if (isFirstTimeConnecting(player, serverLevel)) {
                ServerLevel destLevel = ServerConfig.lobbyDimension()
                        .map(dim -> serverLevel.getServer().getLevel(dim))
                        .orElse(serverLevel);

                // Send new players to the lobby. Note that respawn position is handled by
                // the FTBTeamBasesNeoForge#PlayerRespawnPositionEvent handler
                // TODO drop Fabric support entirely!
                BlockPos lobbySpawnPos = BaseInstanceManager.get(player.server).getLobbySpawnPos();
                player.teleportTo(destLevel, lobbySpawnPos.getX(), lobbySpawnPos.getY(), lobbySpawnPos.getZ(),
                        ServerConfig.LOBBY_PLAYER_YAW.get().floatValue(), -10F);
                BaseInstanceManager.get().addKnownPlayer(player);
            }

            if (player.level() instanceof ServerLevel s) {  // should always be the case
                if (DimensionUtils.isVoidChunkGen(s.getChunkSource().getGenerator())) {
                    VoidTeamDimensionMessage.syncTo(player);
                }
                switchGameMode(player, null, s.dimension());
            }
        }
        return EventResult.pass();
    }

    private static boolean isFirstTimeConnecting(ServerPlayer player, ServerLevel level) {
        return level.dimension().equals(OVERWORLD)
                && player.getRespawnDimension().equals(OVERWORLD)
                && !BaseInstanceManager.get(player.server).isPlayerKnown(player);
    }

    private static void playerChangedDimension(ServerPlayer player, ResourceKey<Level> oldDim, ResourceKey<Level> newDim) {
        switchGameMode(player, oldDim, newDim);

        handleNetherTravel(player, oldDim, newDim);
    }

    private static void switchGameMode(ServerPlayer player, @Nullable ResourceKey<Level> oldDim, ResourceKey<Level> newDim) {
        GameType lobbyGameMode = ServerConfig.LOBBY_GAME_MODE.get();
        ResourceKey<Level> lobby = ServerConfig.lobbyDimension().orElse(OVERWORLD);

        if (newDim.equals(lobby) && player.gameMode.getGameModeForPlayer() != lobbyGameMode && player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
            player.setGameMode(lobbyGameMode);
        } else if (lobby.equals(oldDim) && !newDim.equals(lobby) && player.gameMode.getGameModeForPlayer() == lobbyGameMode) {
            player.setGameMode(GameType.SURVIVAL);
        }
    }

    private static void handleNetherTravel(ServerPlayer player, ResourceKey<Level> oldDim, ResourceKey<Level> newDim) {
        if (player.isOnPortalCooldown()) {
            var mgr = BaseInstanceManager.get(player.server);

            if (newDim.equals(NETHER)) {
                // travelling to the Nether: if from our team dimension, store the player's location (in the from-dimension!) to later return there
                BlockPos portalPos = mgr.getBaseForPlayer(player)
                        .filter(base -> oldDim.equals(base.dimension()))
                        .map(base -> BlockPos.containing(player.xOld, player.yOld, player.zOld))
                        .orElse(null);
                mgr.setPlayerNetherPortalLoc(player, portalPos);
            } else if (oldDim.equals(NETHER) && newDim.equals(OVERWORLD)) {
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

    private static void maybeCreateLobbyFromStructure(ServerLevel serverLevel) {
        BaseInstanceManager mgr = BaseInstanceManager.get(serverLevel.getServer());
        if (!mgr.isLobbyCreated()) {
            ServerConfig.lobbyLocation().ifPresent(lobbyLocation -> {
                // paste the lobby structure into the lobby level (typically the overworld, but can be changed in config)
                StructureTemplate lobby = serverLevel.getStructureManager().getOrCreate(lobbyLocation);
                StructurePlaceSettings placeSettings = DimensionUtils.makePlacementSettings(lobby);

                BlockPos lobbyPos = BlockPos.ZERO.offset(-(lobby.getSize().getX() / 2), ServerConfig.LOBBY_Y_POS.get(), -(lobby.getSize().getZ() / 2));
                lobby.placeInWorld(serverLevel, lobbyPos, lobbyPos, placeSettings, serverLevel.random, Block.UPDATE_ALL);

                BlockPos relativePos = DimensionUtils.locateSpawn(lobby).orElse(BlockPos.ZERO);
                BlockPos playerSpawn = lobbyPos.offset(relativePos.getX(), relativePos.getY(), relativePos.getZ());

                mgr.setLobbySpawnPos(playerSpawn, false);
                serverLevel.removeBlock(playerSpawn, false);
                serverLevel.setDefaultSpawnPos(playerSpawn, ServerConfig.LOBBY_PLAYER_YAW.get().floatValue());

                mgr.setLobbyCreated(true);
                mgr.forceSave(serverLevel.getServer());

                LOGGER.info("Spawned lobby structure @ {} / {}", serverLevel.dimension().location(), lobbyPos);
            });
        }
    }

    public static ResourceLocation rl(String id) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
    }
}


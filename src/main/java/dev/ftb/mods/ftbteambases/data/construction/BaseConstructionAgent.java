package dev.ftb.mods.ftbteambases.data.construction;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.events.BaseCreatedEvent;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.UUID;

public class BaseConstructionAgent {
    private final BaseDefinition baseDefinition;
    private final UUID playerId;
    private final ConstructionWorker constructionWorker;
    private boolean done = false;
    private final MinecraftServer server;

    public BaseConstructionAgent(ServerPlayer player, BaseDefinition baseDefinition) {
        this.baseDefinition = baseDefinition;

        playerId = player.getUUID();
        server = player.getServer();
        try {
            constructionWorker = baseDefinition.createConstructionWorker(player);
            constructionWorker.startConstruction(this::onCompleted);
        } catch (IOException e) {
            throw new FTBTeamBasesException("failed to create construction worker", e);
        }
    }

    private void onCompleted(boolean success) {
        done = true;

        if (server != null && success) {
            Level destLevel = server.getLevel(constructionWorker.getDimension());
            if (destLevel == null) {
                // shouldn't happen!
                FTBTeamBases.LOGGER.error("dest dimension {} doesn't exist?!", constructionWorker.getDimension());
                return;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            // create a party team for the player (who could be offline by now...)
            FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerId).ifPresent(team -> {
                if (team instanceof PlayerTeam playerTeam) {
                    try {
                        Team party = playerTeam.createParty("", null);
                        BaseInstanceManager.get(server).addNewBase(party.getId(), constructionWorker.makeLiveBaseDetails(destLevel, baseDefinition));
                        BaseInstanceManager.get(server).forceSave(server);
                        if (player != null) {
                            // teleport player to newly-created base
                            BaseInstanceManager.get(server).teleportToBaseSpawn(player, party.getId());
                        }
                        FTBTeamBases.LOGGER.info("team base created for player id {}, party id = {}, dim id = {}, type = {}",
                                playerId, party.getId(), destLevel.dimension().location(), baseDefinition.id());

                        BaseCreatedEvent.CREATED.invoker().created(BaseInstanceManager.get(server), player, party);
                    } catch (IllegalStateException e) {
                        if (player != null) {
                            player.displayClientMessage(Component.literal("can't create party team for you! " + e.getMessage()), false);
                        }
                        FTBTeamBases.LOGGER.error("can't create party team for player {}: {}", playerId, e.getMessage());
                    }
                }
            });
        }
    }

    public boolean isDone() {
        return done;
    }

    public void tick() {
        constructionWorker.tick();
    }
}

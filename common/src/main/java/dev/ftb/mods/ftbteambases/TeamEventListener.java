package dev.ftb.mods.ftbteambases;

import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerJoinedPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLeftPartyTeamEvent;
import net.minecraft.server.level.ServerPlayer;

public class TeamEventListener {
    static void teamPlayerJoin(PlayerJoinedPartyTeamEvent event) {
        Team team = event.getTeam();
        ServerPlayer serverPlayer = event.getPlayer();
        if (!team.isPartyTeam() || team.getOwner() == serverPlayer.getUUID()) {
            return;
        }

        if (ServerConfig.CLEAR_PLAYER_INV_ON_JOIN.get()) {
            DimensionUtils.clearPlayerInventory(serverPlayer);
        }
        BaseInstanceManager.get().teleportToSpawn(serverPlayer, team.getTeamId(), true);
    }

    static void teamPlayerLeftParty(PlayerLeftPartyTeamEvent event) {
        ServerPlayer serverPlayer = event.getPlayer();
        if (serverPlayer != null) {
            var baseManager = BaseInstanceManager.get();
            baseManager.getBaseForTeam(event.getTeam()).ifPresent(base -> {
                if (ServerConfig.CLEAR_PLAYER_INV_ON_LEAVE.get()) {
                    DimensionUtils.clearPlayerInventory(serverPlayer);
                }
                if (event.getTeamDeleted()) {
                    baseManager.deleteAndArchive(event.getTeam());
                }
                baseManager.teleportToLobby(serverPlayer);
            });
        }
    }
}

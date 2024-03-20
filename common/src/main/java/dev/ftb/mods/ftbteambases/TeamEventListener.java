package dev.ftb.mods.ftbteambases;

import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerJoinedPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLeftPartyTeamEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

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
        if (ServerConfig.HEAL_PLAYER_ON_JOIN.get()) {
            serverPlayer.heal(serverPlayer.getMaxHealth());
            FoodData foodData = serverPlayer.getFoodData();
            foodData.setExhaustion(0);
            foodData.setFoodLevel(20);
            foodData.setSaturation(5.0f);
        }

        BaseInstanceManager.get(serverPlayer.getServer()).teleportToBaseSpawn(serverPlayer, team.getTeamId(), true);
    }

    static void teamPlayerLeftParty(PlayerLeftPartyTeamEvent event) {
        ServerPlayer serverPlayer = event.getPlayer();
        if (serverPlayer != null) {
            var baseManager = BaseInstanceManager.get(serverPlayer.getServer());
            baseManager.getBaseForTeam(event.getTeam()).ifPresent(base -> {
                if (ServerConfig.CLEAR_PLAYER_INV_ON_LEAVE.get()) {
                    DimensionUtils.clearPlayerInventory(serverPlayer);
                }
                if (event.getTeamDeleted()) {
                    baseManager.deleteAndArchive(serverPlayer.getServer(), event.getTeam());
                }
                baseManager.teleportToLobby(serverPlayer);
            });
        }
    }
}

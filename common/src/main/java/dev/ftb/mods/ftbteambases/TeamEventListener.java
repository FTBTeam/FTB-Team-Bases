package dev.ftb.mods.ftbteambases;

import dev.architectury.utils.GameInstance;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerJoinedPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLeftPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.server.MinecraftServer;
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

        // note: this is a no-op for the player who creates the team initially (base doesn't exist yet)
        //   but is necessary for any players who subsequently join the team
        BaseInstanceManager.get(serverPlayer.getServer()).teleportToBaseSpawn(serverPlayer, team.getTeamId());
    }

    static void teamPlayerLeftParty(PlayerLeftPartyTeamEvent event) {
        MinecraftServer server = event.getPlayer() == null ? GameInstance.getServer() : event.getPlayer().getServer();
        if (server != null) {
            BaseInstanceManager.get(server).onPlayerLeaveTeam(event.getPlayer(), event.getPlayerId());
        }
    }

    public static void teamDeleted(TeamEvent event) {
        MinecraftServer server = GameInstance.getServer();
        if (server != null) {
            BaseInstanceManager.get(server).deleteAndArchive(server, event.getTeam());
        }
    }
}

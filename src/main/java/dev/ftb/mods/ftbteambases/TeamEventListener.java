package dev.ftb.mods.ftbteambases;

import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.mixin.FoodDataAccess;
import dev.ftb.mods.ftbteambases.util.MiscUtil;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class TeamEventListener {
    static void teamPlayerJoin(FTBTeamsEvent.PlayerJoinedPartyTeam event) {
        Team team = event.getEventData().team();
        ServerPlayer serverPlayer = event.getEventData().player();
        if (!team.isPartyTeam() || team.getOwner() == serverPlayer.getUUID()) {
            return;
        }

        if (ServerConfig.CLEAR_PLAYER_INV_ON_JOIN.get()) {
            MiscUtil.clearPlayerInventory(serverPlayer);
        }
        if (ServerConfig.HEAL_PLAYER_ON_JOIN.get()) {
            serverPlayer.heal(serverPlayer.getMaxHealth());
            FoodData foodData = serverPlayer.getFoodData();
            ((FoodDataAccess) foodData).setExhaustionLevel(0f);
            foodData.setFoodLevel(20);
            foodData.setSaturation(5.0f);
        }

        // note: this is a no-op for the player who creates the team initially (base doesn't exist yet)
        //   but is necessary for any players who subsequently join the team
        BaseInstanceManager.get(serverPlayer.level().getServer()).teleportToBaseSpawn(serverPlayer, team.getTeamId());
    }

    static void teamPlayerLeftParty(FTBTeamsEvent.PlayerLeftPartyTeam event) {
        var player = event.getEventData().player();
        MinecraftServer server = player == null ? ServerLifecycleHooks.getCurrentServer() : player.level().getServer();
        if (server != null) {
            BaseInstanceManager.get(server).onPlayerLeaveTeam(player, event.getEventData().playerId());
        }
    }

    public static void teamDeleted(FTBTeamsEvent.TeamDeleted event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            BaseInstanceManager.get(server).deleteAndArchive(server, event.getEventData().team());
        }
    }
}

package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public class HomeCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("home")
                .executes(ctx -> doGoHome(ctx.getSource()));
    }

    private static int doGoHome(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        Team team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getUUID()));
        if (team.isPartyTeam()) {
            if (!BaseInstanceManager.get(source.getServer()).teleportToBaseSpawn(player, team.getId())) {
                throw CommandUtils.CANT_TELEPORT.create(team.getShortName());
            }
        } else {
            throw TeamArgument.NOT_IN_PARTY.create();
        }

        return 1;
    }
}

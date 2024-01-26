package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class VisitCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("visit")
                .requires(ctx -> ctx.hasPermission(2))
                .then(argument("name", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> CommandUtils.suggestLiveBases(builder))
                        .executes(ctx -> doVisit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                );
    }

    private static int doVisit(CommandSourceStack source, String name) throws CommandSyntaxException {
        Team team = FTBTeamsAPI.api().getManager().getTeamByName(name)
                .orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(name));

        if (!BaseInstanceManager.get().teleportToSpawn(source.getPlayerOrException(), team.getTeamId())) {
            throw CommandUtils.CANT_TELEPORT.create(name);
        }

        return 1;
    }
}

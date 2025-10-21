package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.bases.LiveBaseDetails;
import dev.ftb.mods.ftbteambases.util.MiscUtil;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static dev.ftb.mods.ftbteambases.command.CommandUtils.colorize;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ShowCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("show")
                .requires(ctx -> ctx.hasPermission(2))
                .then(argument("name", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> CommandUtils.suggestLiveBases(builder))
                        .executes(ctx -> doShow(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                );
    }

    private static int doShow(CommandSourceStack source, String name) throws CommandSyntaxException {
        Team team = FTBTeamsAPI.api().getManager().getTeamByName(name)
                .orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(name));
        LiveBaseDetails base = BaseInstanceManager.get(source.getServer()).getBaseForTeam(team)
                .orElseThrow(() -> CommandUtils.BASE_NOT_FOUND.create(team.getShortName()));

        source.sendSuccess(() -> Component.translatable("ftbteambases.message.show_base_header",
                team.getShortName()).withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE), false);
        source.sendSuccess(Component::empty, false);
        source.sendSuccess(() -> Component.translatable("ftbteams.info.id",
                colorize(team.getTeamId(), ChatFormatting.YELLOW)), false);
        source.sendSuccess(() -> Component.translatable("ftbteambases.message.base_dimension",
                colorize(base.dimension().location(), ChatFormatting.YELLOW)), false);
        source.sendSuccess(() -> Component.translatable("ftbteambases.message.base_extents_block",
                colorize(base.extents().asBlockPosString(), ChatFormatting.YELLOW)), false);
        source.sendSuccess(() -> Component.translatable("ftbteambases.message.base_extents",
                colorize(base.extents(), ChatFormatting.YELLOW)), false);
        source.sendSuccess(() -> Component.translatable("ftbteambases.message.base_spawn_pos",
                colorize(MiscUtil.blockPosStr(base.spawnPos()), ChatFormatting.YELLOW)), false);

        return 1;
    }
}

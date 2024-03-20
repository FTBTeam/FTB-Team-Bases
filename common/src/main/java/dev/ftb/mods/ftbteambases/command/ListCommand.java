package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.bases.LiveBaseDetails;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;


public class ListCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("list")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(ctx -> doList(ctx.getSource()));
    }

    private static int doList(CommandSourceStack source) {
        Map<UUID,LiveBaseDetails> bases = BaseInstanceManager.get(source.getServer()).allLiveBases();

        if (bases.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("ftbteambases.message.no_bases")
                    .withStyle(ChatFormatting.GOLD), false);
        } else {
            source.sendSuccess(() -> Component.translatable("ftbteambases.message.bases", bases.size())
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE), false);
            source.sendSuccess(Component::empty, false);
            var mgr = FTBTeamsAPI.api().getManager();
            bases.forEach((id, base) -> mgr.getTeamByID(id).ifPresent(team -> {
                var msg = Component.literal("• ")
                        .append(CommandUtils.makeTooltipComponent(Component.literal(team.getShortName()), ChatFormatting.YELLOW, team.getTeamId().toString()))
                        .append(" ")
                        .append(CommandUtils.makeCommandClicky("ftbteambases.gui.show", ChatFormatting.GREEN, "/ftbteambases show " + team.getShortName()))
                        .append(" ")
                        .append(CommandUtils.makeCommandClicky("ftbteambases.gui.visit", ChatFormatting.GREEN, "/ftbteambases visit " + team.getShortName()));
                source.sendSuccess(() -> msg, false);
            }));
        }

        return 1;
    }

}

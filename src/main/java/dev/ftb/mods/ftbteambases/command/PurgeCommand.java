package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.data.bases.ArchivedBaseDetails;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.purging.PurgeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PurgeCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("purge")
                .requires(ctx -> ctx.hasPermission(2))
                .then(literal("id")
                        .then(argument("id", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> CommandUtils.suggestArchivedBases(builder))
                                .executes(ctx -> addById(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                        )
                )
                .then(literal("older")
                        .then(argument("days", IntegerArgumentType.integer(1))
                                .executes(ctx -> addByAge(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "days")))
                        )
                )
                .then(literal("cancel")
                        .then(argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> CommandUtils.suggestPendingPurges(builder))
                                .executes(ctx -> cancelPurge(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                        )
                )
                .then(literal("cancel_all")
                        .executes(ctx -> cancelAll(ctx.getSource()))
                );
    }

    private static int addById(CommandSourceStack source, String ids) {
        BaseInstanceManager mgr = BaseInstanceManager.get(source.getServer());

        List<ArchivedBaseDetails> details = new ArrayList<>();
        for (String id : ids.split("\\s+")) {
            mgr.getArchivedBase(id).ifPresentOrElse(
                    details::add,
                    () -> source.sendFailure(Component.literal("Unknown archived base ID " + id).withStyle(ChatFormatting.RED))
            );
        }

        return schedulePurge(source, details);
    }

    private static int addByAge(CommandSourceStack source, int minDays) {
        BaseInstanceManager mgr = BaseInstanceManager.get(source.getServer());
        long now = System.currentTimeMillis();
        long delta = minDays * 86400L * 1000L;

        List<ArchivedBaseDetails> details = mgr.getArchivedBases().stream().filter(base -> now - base.archiveTime() > delta).toList();

        return schedulePurge(source, details);
    }

    private static int cancelAll(CommandSourceStack source) {
        if (PurgeManager.INSTANCE.clearPending()) {
            source.sendSuccess(() -> Component.literal("All pending purges cancelled"), false);
            return 1;
        } else {
            CommandUtils.error(source, Component.literal("Failed to updated pending purge file, check server log"));
            return 0;
        }
    }

    private static int cancelPurge(CommandSourceStack source, String id) throws CommandSyntaxException {
        if (PurgeManager.INSTANCE.removePending(id)) {
            source.sendSuccess(() -> Component.literal("Cancelled purge for archived base: " + id), false);
            return 1;
        } else {
            CommandUtils.error(source, Component.literal("Failed to updated pending purge file, check server log"));
            return 0;
        }
    }

    private static int schedulePurge(CommandSourceStack source, List<ArchivedBaseDetails> details) {
        if (details.isEmpty()) {
            return 0;
        } else {
            if (PurgeManager.INSTANCE.addPending(details)) {
                source.sendSuccess(() -> Component.literal("Scheduled " + details.size() + " base(s) for permanent purge on next server restart"), false);
                source.sendSuccess(() -> Component.literal("Use '/ftbteambases purge cancel_all' to cancel all pending purges"), false);
                return 1;
            } else {
                CommandUtils.error(source, Component.literal("Can't schedule base(s) for purge"));
                return 0;
            }
        }
    }
}

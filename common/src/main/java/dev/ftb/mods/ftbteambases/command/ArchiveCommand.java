package dev.ftb.mods.ftbteambases.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import dev.ftb.mods.ftbteambases.data.bases.ArchivedBaseDetails;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.bases.LiveBaseDetails;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ArchiveCommand {
    private static final GameProfile UNKNOWN = new GameProfile(Util.NIL_UUID, "???");

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("archive")
                .requires(ctx -> ctx.hasPermission(2))
                .then(literal("list")
                        .executes(ctx -> doListArchive(ctx.getSource()))
                )
                .then(literal("restore")
                        .then(argument("owner_id", UuidArgument.uuid())
                                .suggests((ctx, builder) -> CommandUtils.suggestArchivedBases(builder))
                                .executes(ctx -> restoreArchive(ctx.getSource(), UuidArgument.getUuid(ctx, "owner_id")))
                        )
                )
                .then(literal("purge")
                        .then(argument("min_age_in_days", IntegerArgumentType.integer(0))
                                .executes(ctx -> doPurge(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "min_age_in_days")))
                        )
                );
    }

    private static int doListArchive(CommandSourceStack source) {
        Collection<ArchivedBaseDetails> bases = BaseInstanceManager.get().getArchivedBases();

        if (bases.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("ftbteambases.message.no_archived_bases")
                    .withStyle(ChatFormatting.GOLD), false);
        }

        source.sendSuccess(() -> Component.translatable("ftbteambases.message.archived_bases", bases.size())
                .withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE), false);
        bases.forEach(base -> {
            String playerName = source.getServer().getProfileCache().get(base.ownerId()).orElse(UNKNOWN).getName();
            String when = DateFormat.getDateTimeInstance().format(Date.from(Instant.ofEpochMilli(base.archiveTime())));
            source.sendSuccess(() -> Component.literal("• ").append(
                    CommandUtils.makeTooltipComponent(Component.literal(playerName), ChatFormatting.YELLOW, base.ownerId().toString())
            ).append(" - ").append(Component.literal(when)), false);
        });

        return 1;
    }

    private static int restoreArchive(CommandSourceStack source, UUID ownerId) {
        source.sendSuccess(() -> Component.literal("not implemented yet!"), false);
        return 0;
    }

    private static int doPurge(CommandSourceStack source, int minAgeInDays) {
        source.sendSuccess(() -> Component.literal("not implemented yet!"), false);
        return 0;
    }
}

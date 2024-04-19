package dev.ftb.mods.ftbteambases.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.data.bases.ArchivedBaseDetails;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;

import java.text.DateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ArchiveCommand {
    private static final GameProfile UNKNOWN = new GameProfile(Util.NIL_UUID, "???");

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("archive")
                .requires(ctx -> ctx.hasPermission(2))
                .then(literal("list")
                        .executes(ctx -> doListArchive(ctx.getSource(), base -> true))
                )
                .then(literal("list_for")
                        .then(argument("owner", GameProfileArgument.gameProfile())
                                .suggests(
                                        (ctx, builder) -> {
                                            PlayerList playerlist = ctx.getSource().getServer().getPlayerList();
                                            return SharedSuggestionProvider.suggest(
                                                    playerlist.getPlayers()
                                                            .stream()
                                                            .filter(p -> !playerlist.isOp(p.getGameProfile()))
                                                            .map(p -> p.getGameProfile().getName()),
                                                    builder
                                            );
                                        }
                                )
                                .executes(ctx -> doListArchive(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "owner")))
                        )
                )
                .then(literal("restore")
                        .then(argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> CommandUtils.suggestArchivedBases(builder))
                                .executes(ctx -> doRestoreArchive(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                        )
                )
                .then(literal("purge")
                        .then(literal("-older")
                                .then(argument("min_age_in_days", IntegerArgumentType.integer(0))
                                        .executes(ctx -> doPurgeArchive(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "min_age_in_days")))
                                )
                        )
                        .then(literal("-id")
                                .then(argument("id", StringArgumentType.string())
                                        .suggests((ctx, builder) -> CommandUtils.suggestArchivedBases(builder))
                                        .executes(ctx -> doPurgeArchive(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                                )
                        )
                );
    }

    private static int doListArchive(CommandSourceStack source, Predicate<ArchivedBaseDetails> pred) {
        Collection<ArchivedBaseDetails> bases = BaseInstanceManager.get(source.getServer()).getArchivedBases().stream()
                .filter(pred)
                .sorted(Comparator.comparingLong(ArchivedBaseDetails::archiveTime))
                .toList();

        if (bases.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("ftbteambases.message.no_archived_bases")
                    .withStyle(ChatFormatting.GOLD), false);
        } else {
            source.sendSuccess(() -> Component.translatable("ftbteambases.message.archived_bases", bases.size())
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE), false);
            source.sendSuccess(Component::empty, false);
            bases.forEach(base -> {
                String playerName = source.getServer().getProfileCache().get(base.ownerId()).orElse(UNKNOWN).getName();
                String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date.from(Instant.ofEpochMilli(base.archiveTime())));

                boolean isOwnerInParty = FTBTeamsAPI.api().getManager().getTeamForPlayerID(base.ownerId()).map(Team::isPlayerTeam).orElse(false);

                Component player = CommandUtils.makeTooltipComponent(Component.literal(playerName), ChatFormatting.YELLOW, base.ownerId().toString());
                Component restore = CommandUtils.makeCommandClicky("ftbteambases.gui.restore",
                        isOwnerInParty ? ChatFormatting.GREEN : ChatFormatting.GRAY, "/ftbteambases archive restore " + base.archiveId());
                Component purge = CommandUtils.makeCommandClicky("ftbteambases.gui.purge",
                        ChatFormatting.RED, "/ftbteambases purge id " + base.archiveId(), true);

                source.sendSuccess(() -> Component.literal("• ")
                                .append(Component.literal(base.archiveId()).withStyle(ChatFormatting.AQUA)).append(" : ")
                                .append(player)
                                .append(" (").append(Component.literal(when)).append(")")
                                .append("  ").append(restore)
                                .append("  ").append(purge),
                        false);
            });
        }

        return 1;
    }

    private static int doListArchive(CommandSourceStack source, Collection<GameProfile> profiles) {
        Set<UUID> ids = profiles.stream().map(GameProfile::getId).collect(Collectors.toSet());
        return doListArchive(source, base -> ids.contains(base.ownerId()));
    }

    private static int doRestoreArchive(CommandSourceStack source, String archiveName) throws CommandSyntaxException {
        BaseInstanceManager mgr = BaseInstanceManager.get(source.getServer());

        ArchivedBaseDetails archivedBase = mgr.getArchivedBase(archiveName)
                .orElseThrow(() -> CommandUtils.ARCHIVE_NOT_FOUND.create(archiveName));

        mgr.unarchiveBase(source.getServer(), archivedBase);

        source.sendSuccess(() -> Component.translatable("ftbteambases.message.restored", archivedBase.archiveId()), false);

        return 1;
    }

    private static int doPurgeArchive(CommandSourceStack source, int minAgeInDays) {
        source.sendSuccess(() -> Component.literal("not implemented yet!"), false);
        return 0;
    }

    private static int doPurgeArchive(CommandSourceStack source, String baseId) {
        source.sendSuccess(() -> Component.literal("not implemented yet!"), false);
        return 0;
    }
}

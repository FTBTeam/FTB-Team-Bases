package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.bases.ArchivedBaseDetails;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import dev.ftb.mods.ftbteambases.data.purging.PurgeManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandUtils {
    public static final DynamicCommandExceptionType BASE_NOT_FOUND
            = new DynamicCommandExceptionType(object ->
            Component.translatable("ftbteambases.message.base_not_found", object));
    public static final DynamicCommandExceptionType CANT_TELEPORT
            = new DynamicCommandExceptionType(object ->
            Component.translatable("ftbteambases.message.could_not_teleport", object));
    public static final DynamicCommandExceptionType CONSTRUCTION_IN_PROGRESS
            = new DynamicCommandExceptionType(object ->
            Component.translatable("ftbteambases.message.construction_in_progress", object));
    public static final DynamicCommandExceptionType PLAYER_IN_PARTY = new DynamicCommandExceptionType(object ->
            Component.translatable("ftbteambases.message.player_in_party", object));
    public static final DynamicCommandExceptionType ARCHIVE_NOT_FOUND = new DynamicCommandExceptionType(object ->
            Component.translatable("ftbteambases.message.archived_base_not_found", object));
    public static final DynamicCommandExceptionType PURGE_NOT_FOUND = new DynamicCommandExceptionType(object ->
            Component.translatable("ftbteambases.message.purge_not_found", object));
    public static final DynamicCommandExceptionType DIM_MISSING = new DynamicCommandExceptionType(object ->
            Component.translatable("ftbteambases.message.missing_dimension", object));
    public static final SimpleCommandExceptionType NOT_TEAM_NETHER = new SimpleCommandExceptionType(
            Component.translatable("ftbteambases.message.not_nether_team_base"));

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(FTBTeamBases.MOD_ID)
                .then(RelocateCommand.register())
                .then(CreateBaseCommand.register())
                .then(HomeCommand.register())
                .then(LobbyCommand.register())
                .then(ListCommand.register())
                .then(ShowCommand.register())
                .then(VisitCommand.register())
                .then(VisitCommand.registerNether())
                .then(ArchiveCommand.register())
                .then(PurgeCommand.register())
                .then(SetLobbyPosCommand.register())
        );
    }

    static CompletableFuture<Suggestions> suggestDefinitions(SuggestionsBuilder builder) {
        List<String> ids = BaseDefinitionManager.getServerInstance().getTemplateIds().stream()
                .map(ResourceLocation::toString)
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    static CompletableFuture<Suggestions> suggestLiveBases(SuggestionsBuilder builder) {
        List<String> ids = FTBTeamsAPI.api().getManager().getTeams().stream()
                .filter(team -> team.isPartyTeam() && BaseInstanceManager.get().getBaseForTeam(team).isPresent())
                .map(Team::getShortName)
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    static CompletableFuture<Suggestions> suggestArchivedBases(SuggestionsBuilder builder) {
        List<String> ids = BaseInstanceManager.get().getArchivedBases().stream()
                .map(ArchivedBaseDetails::archiveId)
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    static CompletableFuture<Suggestions> suggestPendingPurges(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(PurgeManager.INSTANCE.getPendingIds(), builder);
    }

    public static Component makeCommandClicky(String translationKey, ChatFormatting color, String command) {
        return makeCommandClicky(translationKey, color, command, false);
    }

    public static Component makeCommandClicky(String translationKey, ChatFormatting color, String command, boolean suggestOnly) {
        ClickEvent.Action action = suggestOnly ? ClickEvent.Action.SUGGEST_COMMAND : ClickEvent.Action.RUN_COMMAND;
        return Component.literal("[")
                .append(Component.translatable(translationKey)
                        .withStyle(Style.EMPTY.withColor(color)
                        .withClickEvent(new ClickEvent(action, command))))
                .append("]");
    }

    public static Component makeTooltipComponent(Component text, ChatFormatting color, String tooltip) {
        return text.copy().withStyle(Style.EMPTY.withColor(color).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tooltip))));
    }

    public static void message(CommandSourceStack source, ChatFormatting color, String translationKey, Object... params) {
        source.sendSuccess(() -> Component.translatable(translationKey, params).withStyle(color), false);
    }

    public static void error(CommandSourceStack source, MutableComponent msg) {
        source.sendFailure(msg.withStyle(ChatFormatting.RED));
    }

    public static MutableComponent colorize(Object o, ChatFormatting... colors) {
        return Component.literal(o.toString()).withStyle(colors);
    }
}

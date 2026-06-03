package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.bases.ArchivedBaseDetails;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

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
                .then(RedoAutoclaimCommand.register())
        );
    }

    static CompletableFuture<Suggestions> suggestDefinitions(SuggestionsBuilder builder) {
        List<String> ids = BaseDefinitionManager.getServerInstance().getTemplateIds().stream()
                .map(Identifier::toString)
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    static CompletableFuture<Suggestions> suggestLiveBases(MinecraftServer server, SuggestionsBuilder builder) {
        List<String> ids = FTBTeamsAPI.api().getManager().getTeams().stream()
                .filter(team -> team.isPartyTeam() && BaseInstanceManager.get(server).getBaseForTeam(team).isPresent())
                .map(Team::getShortName)
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    static CompletableFuture<Suggestions> suggestArchivedBases(MinecraftServer server, SuggestionsBuilder builder) {
        List<String> ids = BaseInstanceManager.get(server).getArchivedBases().stream()
                .map(ArchivedBaseDetails::archiveId)
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    static CompletableFuture<Suggestions> suggestPendingPurges(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(FTBTeamBases.getInstance().getPurgeManager().getPendingIds(), builder);
    }

    public static Component makeCommandClicky(String translationKey, ChatFormatting color, String command) {
        return makeCommandClicky(translationKey, color, command, false);
    }

    public static Component makeCommandClicky(String translationKey, ChatFormatting color, String command, boolean suggestOnly) {
        var action = suggestOnly ? new ClickEvent.SuggestCommand(command) : new ClickEvent.RunCommand(command);
        return Component.literal("[")
                .append(Component.translatable(translationKey)
                        .withStyle(Style.EMPTY.withColor(color)
                        .withClickEvent(action)))
                .append("]");
    }

    public static Component makeTooltipComponent(Component text, ChatFormatting color, String tooltip) {
        return text.copy().withStyle(Style.EMPTY.withColor(color).withHoverEvent(new HoverEvent.ShowText(Component.literal(tooltip))));
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

    public static Predicate<CommandSourceStack> requiresGameMaster() {
        return ctx -> ctx.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}

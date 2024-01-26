package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandUtils {
    public static final DynamicCommandExceptionType BASE_NOT_FOUND
            = new DynamicCommandExceptionType(object -> Component.translatable("ftbteambases.message.base_not_found", object));
    public static final DynamicCommandExceptionType CANT_TELEPORT
            = new DynamicCommandExceptionType(object -> Component.translatable("ftbteambases.message.could_not_teleport", object));
    public static final DynamicCommandExceptionType CONSTRUCTION_IN_PROGRESS
            = new DynamicCommandExceptionType(object -> Component.translatable("ftbteambases.message.construction_in_progress", object));

    static CompletableFuture<Suggestions> suggestDefinitions(SuggestionsBuilder builder) {
        List<String> ids = BaseDefinitionManager.getServerInstance().getTemplateIds().stream()
                .map(ResourceLocation::toString)
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    static CompletableFuture<Suggestions> suggestLiveBases(SuggestionsBuilder builder) {
        List<String> ids = FTBTeamsAPI.api().getManager().getTeams().stream()
                .filter(team -> BaseInstanceManager.get().getBaseForTeam(team).isPresent())
                .map(Team::getShortName)
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    static CompletableFuture<Suggestions> suggestArchivedBases(SuggestionsBuilder builder) {
        List<String> ids = BaseInstanceManager.get().getArchivedBases().stream()
                .map(base -> base.ownerId().toString())
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    public static Component makeCommandClicky(String translationKey, ChatFormatting color, String command) {
        return Component.literal("[")
                .append(Component.translatable(translationKey).withStyle(Style.EMPTY.withColor(color)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))))
                .append("]");
    }

    public static Component makeTooltipComponent(Component text, ChatFormatting color, String tooltip) {
        return text.copy().withStyle(Style.EMPTY.withColor(color).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tooltip))));
    }

    public static void message(CommandSourceStack source, ChatFormatting color, String translationKey, Object... params) {
        source.sendSuccess(() -> Component.translatable(translationKey, params).withStyle(color), false);
    }
}

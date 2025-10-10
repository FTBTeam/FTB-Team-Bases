package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.command.arguments.BaseDefinitionArgument;
import dev.ftb.mods.ftbteambases.data.construction.BaseConstructionManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CreateBaseCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("create")
                .requires(ctx -> ctx.hasPermission(2))
                .then(argument("template", BaseDefinitionArgument.create())
                        .suggests((ctx, builder) -> CommandUtils.suggestDefinitions(builder))
                        .executes(ctx -> doCreateBase(ctx.getSource(), BaseDefinitionArgument.get(ctx, "template")))
                );
    }

    private static int doCreateBase(CommandSourceStack source, BaseDefinition baseDefinition) throws CommandSyntaxException {
        BaseConstructionManager.INSTANCE.begin(source.getPlayerOrException(), baseDefinition);

        return 1;
    }

}
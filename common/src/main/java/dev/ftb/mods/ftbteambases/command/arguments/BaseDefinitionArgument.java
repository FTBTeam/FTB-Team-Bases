package dev.ftb.mods.ftbteambases.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BaseDefinitionArgument implements ArgumentType<BaseDefinition> {
    private static final DynamicCommandExceptionType NOT_FOUND = new DynamicCommandExceptionType(
            object -> Component.translatable("ftbteambases.message.missing_definition", object));

    public static BaseDefinitionArgument create() {
        return new BaseDefinitionArgument();
    }

    public static BaseDefinition get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, BaseDefinition.class);
    }

    private BaseDefinitionArgument() {
    }

    @Override
    public BaseDefinition parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();

        while (reader.canRead() && ResourceLocation.isAllowedInResourceLocation(reader.peek())) {
            reader.skip();
        }

        String s = reader.getString().substring(i, reader.getCursor());

        return BaseDefinitionManager.getServerInstance().getBaseDefinition(new ResourceLocation(s))
                .orElseThrow(() -> NOT_FOUND.createWithContext(reader, s));
    }

}

package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RedoAutoclaimCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("redo_autoclaim")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(ctx -> {
                    BaseInstanceManager.get(ctx.getSource().getServer()).setAutoclaimNeeded(true);
                    ctx.getSource().sendSuccess(() -> Component.literal("Lobby autoclaiming will be redone on next lobby level load"), false);
                    return Command.SINGLE_SUCCESS;
                });
    }
}

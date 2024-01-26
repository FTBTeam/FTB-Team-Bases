package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public class LobbyCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("lobby")
                .executes(ctx -> doGoHome(ctx.getSource()));
    }

    private static int doGoHome(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        if (!BaseInstanceManager.get().teleportToLobby(player)) {
            throw CommandUtils.CANT_TELEPORT.create("<lobby>");
        }

        return 1;
    }
}

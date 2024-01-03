package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.util.RegionFileRelocator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ModCommands {
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        dispatcher.register(literal(FTBTeamBases.MOD_ID)
                .then(literal("relocate")
                        .then(argument("template", ResourceLocationArgument.id())
                                .then(argument("region_x", IntegerArgumentType.integer())
                                        .then(argument("region_z", IntegerArgumentType.integer())
                                                .then(literal("force")
                                                        .executes(ctx -> doRelocate(ctx.getSource(), ResourceLocationArgument.getId(ctx, "template"), IntegerArgumentType.getInteger(ctx, "region_x"), IntegerArgumentType.getInteger(ctx, "region_z"), true))
                                                )
                                                .executes(ctx -> doRelocate(ctx.getSource(), ResourceLocationArgument.getId(ctx, "template"), IntegerArgumentType.getInteger(ctx, "region_x"), IntegerArgumentType.getInteger(ctx, "region_z"), false))
                                        )
                                )
                        )
                )
        );
    }

    private static int doRelocate(CommandSourceStack source, ResourceLocation template, int x, int z, boolean force) {
        source.sendSuccess(() -> Component.literal("relocation started, stand by...").withStyle(ChatFormatting.YELLOW), false);

        MinecraftServer server = source.getServer();
        CompletableFuture.supplyAsync(() -> {
            try {
                return RegionFileRelocator.relocateRegionTemplate(server, template, source.getLevel().dimension(), x, z, force);
            } catch (IOException e) {
                return false;
            }
        }).thenAccept(result -> server.tell(new TickTask(server.getTickCount() + 2,
                () -> handleRelocationResult(source, x, z, result)))
        );

        return 1;
    }

    private static void handleRelocationResult(CommandSourceStack source, int x, int z, boolean success) {
        if (success) {
            source.sendSuccess(() -> Component.literal("relocation complete! teleporting...").withStyle(ChatFormatting.GREEN), false);
            if (source.getPlayer() != null) {
                int xPos = x * 512 + 8;
                int zPos = z * 512 + 8;
                source.getLevel().getChunkAt(new BlockPos(xPos, 0, zPos));
                int y = source.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, xPos, zPos);
                source.getPlayer().teleportTo(source.getLevel(), xPos, y, zPos, 0, 0);
            }
        } else {
            source.sendFailure(Component.literal("relocation failed, check server log").withStyle(ChatFormatting.RED));
        }
    }
}

package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteambases.util.RegionFileRelocator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.IOException;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class RelocateCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("relocate")
                .requires(ctx -> ctx.hasPermission(2))
                .then(argument("template", StringArgumentType.word())
                        .then(argument("region_x", IntegerArgumentType.integer())
                                .then(argument("region_z", IntegerArgumentType.integer())
                                        .then(literal("force")
                                                .executes(ctx -> doRelocate(ctx.getSource(), StringArgumentType.getString(ctx, "template"), IntegerArgumentType.getInteger(ctx, "region_x"), IntegerArgumentType.getInteger(ctx, "region_z"), true))
                                        )
                                        .executes(ctx -> doRelocate(ctx.getSource(), StringArgumentType.getString(ctx, "template"), IntegerArgumentType.getInteger(ctx, "region_x"), IntegerArgumentType.getInteger(ctx, "region_z"), false))
                                )
                        )
                );
    }



    private static int doRelocate(CommandSourceStack source, String templateId, int x, int z, boolean force) {
        try {
            RegionFileRelocator relocator = RegionFileRelocator.create(source, templateId,
                    source.getLevel().dimension(),
                    RelocateCommand::onRelocationTick,
                    XZ.of(x, z), force);
            relocator.start(success -> onRelocationComplete(source, relocator, x, z, success));
            source.sendSuccess(() -> Component.literal("relocation started, stand by...").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("could not start region relocation: " + e.getMessage()));
            return 0;
        }
    }

    private static void onRelocationTick(ServerPlayer player, RegionFileRelocator relocator) {
        if (player != null) {
            int pct = (int)(100 * relocator.getProgress());
            player.displayClientMessage(Component.literal("progress " + pct + "%"), true);
        }
    }

    private static void onRelocationComplete(CommandSourceStack source, RegionFileRelocator relocator, int x, int z, boolean success) {
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

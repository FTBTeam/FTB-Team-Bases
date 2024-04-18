package dev.ftb.mods.ftbteambases.data.construction.workers;

import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.JigsawParams;
import dev.ftb.mods.ftbteambases.util.ProgressiveJigsawPlacer;
import dev.ftb.mods.ftbteambases.util.RegionCoords;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Used for jigsaw-based generation done over multiple ticks on the main thread
 */
public class JigsawWorker extends AbstractStructureWorker {
    private final ProgressiveJigsawPlacer placer;
    private BooleanConsumer onCompleted;

    public JigsawWorker(ServerPlayer player, BaseDefinition baseDefinition, JigsawParams jigsawParams, boolean privateDimension) {
        super(player, baseDefinition, privateDimension);

        RegionCoords r = getRegionExtents().start();
        BlockPos startPos = new BlockPos(r.x() * 512 + 256, jigsawParams.yPos(), r.z() * 512 + 256);

        placer = new ProgressiveJigsawPlacer(player.createCommandSourceStack(), jigsawParams, startPos);
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        super.startConstruction(onCompleted);

        ServerLevel level = getOrCreateLevel(placer.getSource().getServer());
        if (level == null) {
            throw new FTBTeamBasesException("Jigsaw Worker: can't get/create dimension " + getDimension().location());
        }

        placer.start(level);
    }

    @Override
    public void tick() {
        boolean done = placer.tick();

        ServerPlayer player = placer.getSource().getPlayer();
        if (player != null) {
            int pct = (int)(100 * placer.getProgress());
            player.displayClientMessage(Component.literal("Progress: " + pct + "%"), true);
        }

        if (done) {
            onCompleted.accept(true);
        }
    }
}

package dev.ftb.mods.ftbteambases.data.construction.workers;

import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.JigsawParams;
import dev.ftb.mods.ftbteambases.util.ProgressiveJigsawPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Used for jigsaw-based generation done over multiple ticks on the main thread
 */
public class JigsawWorker extends AbstractStructureWorker {
    private final ProgressiveJigsawPlacer placer;

    public JigsawWorker(ServerPlayer player, BaseDefinition baseDefinition, JigsawParams jigsawParams, boolean privateDimension) {
        super(player, baseDefinition, privateDimension);

        ServerLevel serverLevel = getOrCreateLevel(player.getServer());
        BlockPos origin = getPlacementOrigin(serverLevel, getSpawnXZ(), jigsawParams.yPos())
                .offset(jigsawParams.generationOffset().orElse(BlockPos.ZERO));

        placer = new ProgressiveJigsawPlacer(player.createCommandSourceStack(), jigsawParams, origin);
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

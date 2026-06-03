package dev.ftb.mods.ftbteambases.data.construction.workers;

import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.JigsawParams;
import dev.ftb.mods.ftbteambases.util.ProgressiveJigsawPlacer;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
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

        ServerLevel serverLevel = getOrCreateLevel(player.level().getServer());
        BlockPos origin = getPlacementOrigin(serverLevel, getSpawnXZ(), jigsawParams.yPos())
                .offset(jigsawParams.generationOffset().orElse(BlockPos.ZERO));

        placer = new ProgressiveJigsawPlacer(player.createCommandSourceStack(), jigsawParams, origin);
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        super.startConstruction(onCompleted);

        placer.start(getOrCreateLevel(placer.getSource().getServer()));
    }

    @Override
    public void tick() {
        boolean done = placer.tick();

        ServerPlayer player = placer.getSource().getPlayer();
        if (player != null) {
            int pct = (int)(100 * placer.getProgress());
            player.sendOverlayMessage(Component.literal("Progress: " + pct + "%"));
        }

        if (done) {
            onCompleted.accept(true);
        }
    }
}

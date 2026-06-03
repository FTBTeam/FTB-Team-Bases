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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;

/**
 * Used for jigsaw-based generation done over multiple ticks on the main thread
 */
public class JigsawWorker extends AbstractStructureWorker {
    private final Lazy<ProgressiveJigsawPlacer> placer;

    public JigsawWorker(ServerPlayer player, BaseDefinition baseDefinition, JigsawParams jigsawParams, boolean privateDimension) {
        super(player, baseDefinition, privateDimension);

        var source = player.createCommandSourceStack();
        var spawnXZ = getSpawnXZ();

        getOrCreateLevel(player.getServer());

        this.placer = Lazy.of(() -> {
            ServerLevel level = getOrCreateLevel(source.getServer());
            if (level == null) {
                throw new FTBTeamBasesException("Jigsaw Worker: can't get/create dimension " + getDimension().location());
            }
            BlockPos origin = getPlacementOrigin(level, spawnXZ, jigsawParams.yPos())
                    .offset(jigsawParams.generationOffset().orElse(BlockPos.ZERO));

            ProgressiveJigsawPlacer p = new ProgressiveJigsawPlacer(source, jigsawParams, origin);
            p.start(level);
            return p;
        });
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        super.startConstruction(onCompleted);
        placer.get();
    }

    @Override
    public void tick() {
        ProgressiveJigsawPlacer p = placer.get();
        boolean done = p.tick();

        ServerPlayer player = p.getSource().getPlayer();
        if (player != null && !done) {
            int pct = (int)(100 * p.getProgress());
            player.displayClientMessage(Component.literal("Progress: " + pct + "%"), true);
        }

        if (done) {
            onCompleted.accept(p.hasWorkData());
        }
    }

    @Override
    public BlockPos getInitialSpawnPos(Level destLevel, BaseDefinition baseDefinition) {
        BlockPos offset = baseDefinition.spawnOffset();
        return placer.get().getStartPos().offset(offset);
    }
}

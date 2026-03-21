package dev.ftb.mods.ftbteambases.data.construction.workers;

import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.JigsawParams;
import dev.ftb.mods.ftbteambases.util.ProgressiveJigsawPlacer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Used for jigsaw-based generation done over multiple ticks on the main thread
 */
public class JigsawWorker extends AbstractStructureWorker {
    private final JigsawParams jigsawParams;
    private final CommandSourceStack source;
    private final XZ spawnXZ;
    private ProgressiveJigsawPlacer placer;
    private BlockPos origin;

    public JigsawWorker(ServerPlayer player, BaseDefinition baseDefinition, JigsawParams jigsawParams, boolean privateDimension) {
        super(player, baseDefinition, privateDimension);

        this.jigsawParams = jigsawParams;
        this.source = player.createCommandSourceStack();
        this.spawnXZ = getSpawnXZ();

        getOrCreateLevel(player.getServer());
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        super.startConstruction(onCompleted);

        ServerLevel level = getOrCreateLevel(source.getServer());
        if (level == null) {
            throw new FTBTeamBasesException("Jigsaw Worker: can't get/create dimension " + getDimension().location());
        }
        level.getChunk(spawnXZ.x() >> 4, spawnXZ.z() >> 4, ChunkStatus.FULL, true);
        origin = getPlacementOrigin(level, spawnXZ, jigsawParams.yPos())
                .offset(jigsawParams.generationOffset().orElse(BlockPos.ZERO));

        placer = new ProgressiveJigsawPlacer(source, jigsawParams, origin);
        placer.start(level);
    }

    @Override
    public void tick() {
        boolean done = placer.tick();

        ServerPlayer player = placer.getSource().getPlayer();
        if (player != null && !done) {
            int pct = (int)(100 * placer.getProgress());
            player.displayClientMessage(Component.literal("Progress: " + pct + "%"), true);
        }

        if (done) {
            onCompleted.accept(placer.hasWorkData());
        }
    }

    @Override
    public BlockPos getInitialSpawnPos(Level destLevel, BaseDefinition baseDefinition) {
        BlockPos offset = baseDefinition.spawnOffset();
        return origin.offset(offset);
    }
}

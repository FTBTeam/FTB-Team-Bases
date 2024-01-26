package dev.ftb.mods.ftbteambases.data.workers;

import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.JigsawParams;
import dev.ftb.mods.ftbteambases.util.ProgressiveJigsawPlacer;
import dev.ftb.mods.ftbteambases.util.RegionCoords;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Used for jigsaw-based generation done over multiple ticks on the main thread
 */
public class JigsawWorker implements ConstructionWorker {
    private final ProgressiveJigsawPlacer placer;
    private final ResourceKey<Level> dimensionKey;
    private final RegionCoords startRegion;
    private BooleanConsumer onCompleted;

    public JigsawWorker(ServerPlayer player, BaseDefinition baseDefinition, JigsawParams jigsawParams) {
        CommandSourceStack source = player.createCommandSourceStack();
        MinecraftServer server = source.getServer();

        dimensionKey = ResourceKey.create(Registries.DIMENSION, baseDefinition.dimensionSettings().dimensionId().orElse(FTBTeamBases.SHARED_DIMENSION_ID));
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            throw new FTBTeamBasesException("Jigsaw Worker: unknown dimension " + dimensionKey.location());
        }

        startRegion = BaseInstanceManager.get(server).nextGenerationPos(server, baseDefinition, dimensionKey.location(), XZ.of(1, 1));

        BlockPos startPos = new BlockPos(startRegion.x() * 512 + 256, jigsawParams.yPos(), startRegion.z() * 512 + 256);

        placer = new ProgressiveJigsawPlacer(source, jigsawParams, level, startPos);
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        this.onCompleted = onCompleted;

        placer.start();
    }

    @Override
    public RegionExtents getRegionExtents() {
        return new RegionExtents(startRegion, startRegion);
    }

    @Override
    public XZ getSpawnXZ() {
        return XZ.of(startRegion.x() + 512 * 256, startRegion.z() + 512 * 256);
    }

    @Override
    public ResourceKey<Level> getDimension() {
        return dimensionKey;
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

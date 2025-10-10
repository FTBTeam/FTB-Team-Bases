package dev.ftb.mods.ftbteambases.data.construction.workers;

import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.construction.ConstructionWorker;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.util.DynamicDimensionManager;
import dev.ftb.mods.ftbteambases.util.RegionCoords;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Objects;
import java.util.Optional;

public abstract class AbstractStructureWorker implements ConstructionWorker {
    protected final BaseDefinition baseDefinition;
    protected final boolean privateDimension;
    private final ResourceKey<Level> dimensionKey;
    protected BooleanConsumer onCompleted;
    private final RegionExtents extents;

    protected AbstractStructureWorker(ServerPlayer player, BaseDefinition baseDefinition, boolean privateDimension) {
        this.baseDefinition = baseDefinition;
        this.privateDimension = privateDimension;

        dimensionKey = privateDimension ?
                ConstructionWorker.makePrivateDimensionKeyFor(player.getGameProfile().getName().toLowerCase()) :
                ResourceKey.create(Registries.DIMENSION, baseDefinition.dimensionSettings().dimensionId().orElse(FTBTeamBases.SHARED_DIMENSION_ID));

        MinecraftServer server = Objects.requireNonNull(player.getServer());
        RegionCoords startRegion = BaseInstanceManager.get(server).nextGenerationPos(server, baseDefinition, getDimension().location(), baseDefinition.extents());
        extents = new RegionExtents(
            startRegion,
            startRegion.offsetBy(baseDefinition.extents().x() - 1, baseDefinition.extents().z() - 1)
        );
    }

    protected final ServerLevel getOrCreateLevel(MinecraftServer server) {
        return privateDimension ?
                DynamicDimensionManager.create(server, dimensionKey, baseDefinition) :
                server.getLevel(dimensionKey);
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        this.onCompleted = onCompleted;
    }

    @Override
    public ResourceKey<Level> getDimension() {
        return dimensionKey;
    }

    @Override
    public RegionExtents getRegionExtents() {
        return extents;
    }

    /**
     * Get the blockpos where placement of the structure/jigsaw should begin.
     *
     * @param level the level
     * @param xz the X/Z position where generation should happen
     * @param yPos an option Y position; if present, use it;
     *            if absent, use the surface height of the level at this X/Z position
     * @return the placement origin
     */
    protected final BlockPos getPlacementOrigin(ServerLevel level, XZ xz, Optional<Integer> yPos) {
        int x = xz.x();
        int z = xz.z();
        return yPos
                .map(y -> new BlockPos(x, y, z))
                .orElse(new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z));
    }
}

package dev.ftb.mods.ftbteambases.data.construction;

import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.data.bases.LiveBaseDetails;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public interface ConstructionWorker {
    void startConstruction(BooleanConsumer onCompleted);

    RegionExtents getRegionExtents();

    XZ getSpawnXZ();

    ResourceKey<Level> getDimension();

    void tick();

    /**
     * Determine the default position for players to spawn for the given base type. This default implementation puts
     * the player at the X/Z position returned by {@link #getSpawnXZ()}, offset by the spawn offset defined in the base
     * definition, and at a Y position of the surface at that X/Z position, also offset by the base definition's offset.
     *
     * @param destLevel the level being spawned in
     * @param baseDefinition the base definition
     * @return a position where players should be spawned
     */
    default BlockPos getInitialSpawnPos(Level destLevel, BaseDefinition baseDefinition) {
        BlockPos offset = baseDefinition.spawnOffset();
        XZ spawnXZ = getSpawnXZ().offset(offset.getX(), offset.getZ());
        destLevel.getChunk(spawnXZ.x() >> 4, spawnXZ.z() >> 4);
        int yPos = destLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, spawnXZ.x(), spawnXZ.z());
        return new BlockPos(spawnXZ.x(), yPos, spawnXZ.z()).above(offset.getY());
    }

    default LiveBaseDetails makeLiveBaseDetails(Level destLevel, BaseDefinition baseDefinition) {
        return new LiveBaseDetails(getRegionExtents(), getDimension(), getInitialSpawnPos(destLevel, baseDefinition));
    }
}

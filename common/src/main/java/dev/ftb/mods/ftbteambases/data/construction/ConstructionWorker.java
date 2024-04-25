package dev.ftb.mods.ftbteambases.data.construction;

import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.bases.LiveBaseDetails;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public interface ConstructionWorker {
    void startConstruction(BooleanConsumer onCompleted);

    RegionExtents getRegionExtents();

    ResourceKey<Level> getDimension();

    void tick();

    default XZ getSpawnXZ() {
        RegionExtents extents = getRegionExtents();

        // default spawn is centre of overall region extents
        int x = Mth.lerpInt(0.5f, extents.start().x() * 512, extents.end().x() * 512 + 511);
        int z = Mth.lerpInt(0.5f, extents.start().z() * 512, extents.end().z() * 512 + 511);
        return XZ.of(x, z);
    }

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
        int yPos = destLevel.getHeight(Heightmap.Types.WORLD_SURFACE, spawnXZ.x(), spawnXZ.z());

        if (yPos > destLevel.getMinBuildHeight()) {
            return new BlockPos(spawnXZ.x(), yPos, spawnXZ.z()).above(offset.getY());
        } else {
            return findSafeSpawn(destLevel, baseDefinition, spawnXZ);
        }
    }

    private static @NotNull BlockPos findSafeSpawn(Level destLevel, BaseDefinition baseDefinition, XZ spawnXZ) {
        // oops, this spot's over the void. try to find a safe spot nearby to avoid pain and suffering
        BlockPos start = new BlockPos(spawnXZ.x() - 8, 0, spawnXZ.z() - 8);
        for (BlockPos.MutableBlockPos pos : BlockPos.spiralAround(start, 16, Direction.EAST, Direction.SOUTH)) {
            int y = destLevel.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
            if (y > destLevel.getMinBuildHeight()) {
                return new BlockPos(pos.getX(), y + 1, pos.getZ());
            }
        }
        // still nothing :(  create an emergency block for the player to stand on
        BlockPos fallbackPos = new BlockPos(spawnXZ.x(), 64, spawnXZ.z());
        FTBTeamBases.LOGGER.warn("can't find safe player spawn for base {}, creating emergency block at {}", baseDefinition.id(), fallbackPos);
        destLevel.setBlock(fallbackPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        return fallbackPos.above();
    }

    default LiveBaseDetails makeLiveBaseDetails(Level destLevel, BaseDefinition baseDefinition) {
        return new LiveBaseDetails(getRegionExtents(), getDimension(), getInitialSpawnPos(destLevel, baseDefinition));
    }

    static ResourceKey<Level> makePrivateDimensionKeyFor(String playerName) {
        return ResourceKey.create(Registries.DIMENSION, FTBTeamBases.rl(
                DimensionUtils.PRIVATE_DIM_PREFIX + playerName + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
        ));
    }
}

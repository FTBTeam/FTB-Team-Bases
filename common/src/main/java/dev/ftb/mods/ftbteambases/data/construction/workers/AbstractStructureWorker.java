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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public abstract class AbstractStructureWorker implements ConstructionWorker {
    protected final BaseDefinition baseDefinition;
    protected final boolean privateDimension;
    private final ResourceKey<Level> dimensionKey;
    private final RegionCoords startRegion;
    protected BooleanConsumer onCompleted;

    protected AbstractStructureWorker(ServerPlayer player, BaseDefinition baseDefinition, boolean privateDimension) {
        this.baseDefinition = baseDefinition;
        this.privateDimension = privateDimension;

        if (privateDimension) {
            String dimName = DynamicDimensionWorker.makeDimName(player.getGameProfile().getName().toLowerCase());
            dimensionKey = ResourceKey.create(Registries.DIMENSION, FTBTeamBases.rl(dimName));
        } else {
            dimensionKey = ResourceKey.create(Registries.DIMENSION, baseDefinition.dimensionSettings().dimensionId()
                    .orElse(FTBTeamBases.SHARED_DIMENSION_ID));
        }

        MinecraftServer server = player.getServer();
        startRegion = BaseInstanceManager.get(server).nextGenerationPos(server, baseDefinition, getDimension().location(), XZ.of(1, 1));
    }

    protected final ServerLevel getOrCreateLevel(MinecraftServer server) {
        if (privateDimension) {
            return DynamicDimensionManager.create(server, dimensionKey, baseDefinition);
        } else {
            return server.getLevel(dimensionKey);
        }
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
        return new RegionExtents(startRegion, startRegion);
    }

    @Override
    public XZ getSpawnXZ() {
        return XZ.of(startRegion.x() * 512 + 256, startRegion.z() * 512 + 256);
    }

}

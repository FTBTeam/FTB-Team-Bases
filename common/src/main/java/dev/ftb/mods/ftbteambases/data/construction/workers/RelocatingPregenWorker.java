package dev.ftb.mods.ftbteambases.data.construction.workers;

import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.construction.ConstructionWorker;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.Pregen;
import dev.ftb.mods.ftbteambases.util.RegionCoords;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import dev.ftb.mods.ftbteambases.util.RegionFileRelocator;
import dev.ftb.mods.ftbteambases.util.RegionFileUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Used when a set of pregenerated region files needs to be copied and relocated from the region template dir into an
 * existing shared dimension.
 */
public class RelocatingPregenWorker implements ConstructionWorker {
    private final RegionFileRelocator relocator;
    private final RegionExtents extents;
    private final ResourceKey<Level> dimensionKey;
    private final XZ regionOffset;

    public RelocatingPregenWorker(ServerPlayer player, BaseDefinition baseDefinition, Pregen pregen) throws IOException {
        CommandSourceStack source = player.createCommandSourceStack();
        MinecraftServer server = source.getServer();

        Path pregenDir = RegionFileUtil.getPregenPath(pregen.templateId(), server, "region");

        extents = RegionFileUtil.getRegionExtents(pregenDir)
                .orElseThrow(() -> new FTBTeamBasesException("no region files in " + pregenDir));

        dimensionKey = ResourceKey.create(Registries.DIMENSION, baseDefinition.dimensionSettings().dimensionId().orElse(FTBTeamBases.SHARED_DIMENSION_ID));
        RegionCoords startRegion = BaseInstanceManager.get(server).nextGenerationPos(server, baseDefinition, dimensionKey.location(), extents.getSize());

        // this canonicalises the pregen region coords, so they effectively start at (0,0)
        //   so will be copied to exactly the place we expect in the target dimension
        regionOffset = XZ.of(startRegion.x() - extents.start().x(), startRegion.z() - extents.start().z());

        relocator = new RegionFileRelocator(source, pregen.templateId(), dimensionKey, regionOffset, false);
    }

    @Override
    public void tick() {
        ServerPlayer player = relocator.getSource().getPlayer();
        if (player != null) {
            int pct = (int)(100 * relocator.getProgress());
            player.displayClientMessage(Component.literal("Progress: " + pct + "%"), true);
        }
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        relocator.start(onCompleted);
    }

    @Override
    public RegionExtents getRegionExtents() {
        return new RegionExtents(extents.start().offsetBy(regionOffset), extents.end().offsetBy(regionOffset));
    }

    @Override
    public XZ getSpawnXZ() {
        // default spawn pos is centre of the entire region extent
        int x1 = (extents.start().x() + regionOffset.x()) * 512;
        int z1 = (extents.start().z() + regionOffset.z()) * 512;
        int x2 = (extents.end().x() + regionOffset.x()) * 512 + 511;
        int z2 = (extents.end().z() + regionOffset.z()) * 512 + 511;

        return XZ.of(x1 + (x2 - x1) / 2, z1 + (z2 - z1) / 2);
    }

    @Override
    public ResourceKey<Level> getDimension() {
        return dimensionKey;
    }
}

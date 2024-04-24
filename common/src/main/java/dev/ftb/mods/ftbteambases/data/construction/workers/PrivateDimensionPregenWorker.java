package dev.ftb.mods.ftbteambases.data.construction.workers;

import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.construction.ConstructionWorker;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.Pregen;
import dev.ftb.mods.ftbteambases.util.DynamicDimensionManager;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import dev.ftb.mods.ftbteambases.util.RegionFileUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Used to create a dynamic private dimension for the base from pregenerated region files.
 */
public class PrivateDimensionPregenWorker implements ConstructionWorker {
    private final ResourceKey<Level> dimensionKey;
    private final ServerPlayer player;
    private final BaseDefinition baseDefinition;
    private final RegionExtents extents;
    private final Path pregenDir;
    private BooleanConsumer onCompleted;
    private int delayTick = 2;

    public PrivateDimensionPregenWorker(ServerPlayer player, BaseDefinition baseDefinition, Pregen pregen) {
        this.player = player;
        this.baseDefinition = baseDefinition;

        dimensionKey = ConstructionWorker.makePrivateDimensionKeyFor(player.getGameProfile().getName().toLowerCase());

        pregenDir = RegionFileUtil.getPregenPath(pregen.templateId(), Objects.requireNonNull(player.getServer()), null);
        extents = RegionFileUtil.getRegionExtents(pregenDir.resolve("region"))
                .orElseThrow(() -> new FTBTeamBasesException("no region files in " + pregenDir));
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        this.onCompleted = onCompleted;

        MinecraftServer server = Objects.requireNonNull(player.getServer());

        // Check for pregen'd MCA files and copy them into where the dimension will be generated
        RegionFileUtil.copyIfExists(player.server, pregenDir, dimensionKey);

        // Create the dimension
        DynamicDimensionManager.create(server, dimensionKey, baseDefinition);
    }

    @Override
    public RegionExtents getRegionExtents() {
        return extents;
    }

    @Override
    public ResourceKey<Level> getDimension() {
        return dimensionKey;
    }

    @Override
    public void tick() {
        // give it a couple of ticks to settle in
        if (--delayTick <= 0) {
            onCompleted.accept(true);
        }
    }

}

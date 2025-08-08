package dev.ftb.mods.ftbteambases.worldgen.chunkgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.mixin.ChunkGeneratorAccess;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionProvider;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Basically the vanilla NoiseBasedChunkGenerator, with biome distribution but no actual blocks
 * and with an optional initial start structure.
 * Note: it is very important that this class extends NoiseBasedChunkGenerator and not just ChunkGenerator - vanilla does
 * specific instanceof checks during chunk gen which require this to be a type of NoiseBasedChunkGenerator
 */
public class VoidChunkGenerator extends NoiseBasedChunkGenerator implements BaseDefinitionProvider {
    public static final MapCodec<VoidChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings),
            ResourceLocation.CODEC.optionalFieldOf("prebuilt_structure_id", FTBTeamBases.NO_TEMPLATE_ID).forGetter(VoidChunkGenerator::getBaseDefinitionId)
    ).apply(instance, instance.stable(VoidChunkGenerator::new)));

    private final ResourceLocation baseDefinitionId;

    public static VoidChunkGenerator create(MinecraftServer server, RegistryAccess registryAccess, ResourceLocation prebuiltStructureId) {
        Holder<MultiNoiseBiomeSourceParameterList> preset = registryAccess.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).orElseThrow()
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromPreset(preset);

        Holder<NoiseGeneratorSettings> settings = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        VoidChunkGenerator gen = new VoidChunkGenerator(biomeSource, settings, prebuiltStructureId);

        if (!ServerConfig.FEATURE_GEN.get().shouldGenerate(true)) {
            //noinspection ConstantConditions
            ((ChunkGeneratorAccess) gen).setFeaturesPerStep(List::of);
        }

        return gen;
    }

    private VoidChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, ResourceLocation baseDefinitionId) {
        super(biomeSource, settings);

        this.baseDefinitionId = baseDefinitionId;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> holderLookup, RandomState randomState, long seed) {
        return ChunkGeneratorStructureState.createForFlat(
                randomState,
                seed,
                this.biomeSource,
                DimensionUtils.possibleStructures(holderLookup, baseDefinitionId)
        );
    }


    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // no-op
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // no-op
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // no-op
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        return new NoiseColumn(height.getMinBuildHeight(), new BlockState[0]);
    }

    @Override
    public ResourceLocation getBaseDefinitionId() {
        return baseDefinitionId;
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel worldGenLevel, ChunkAccess chunkAccess, StructureManager structureManager) {
        // no-op
    }
}

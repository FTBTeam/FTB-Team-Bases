package dev.ftb.mods.ftbteambases.worldgen.chunkgen;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.utils.GameInstance;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.mixin.ChunkGeneratorAccess;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionProvider;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;
import java.util.Objects;

/**
 * Basically the vanilla NoiseBasedChunkGenerator, with an optional initial start structure.  Can be configured to use
 * either standard overworld biome noise source, or a single biome.
 * Note: it is very important that this class extends NoiseBasedChunkGenerator and not just ChunkGenerator - vanilla does
 * specific instanceof checks during chunk gen which require this to be a type of NoiseBasedChunkGenerator
 */
public class CustomChunkGenerator extends NoiseBasedChunkGenerator implements BaseDefinitionProvider {
    public static final Codec<CustomChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter((gen) -> gen.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings),
            ResourceLocation.CODEC.optionalFieldOf("prebuilt_structure_id", FTBTeamBases.NO_TEMPLATE_ID).forGetter(CustomChunkGenerator::getBaseDefinitionId)
    ).apply(instance, instance.stable(CustomChunkGenerator::new)));

    private final ResourceLocation baseTemplateId;

    public static CustomChunkGenerator create(RegistryAccess registryAccess, ResourceLocation prebuiltStructureId) {
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        BiomeSource biomeSource;
        if (!ServerConfig.SINGLE_BIOME_ID.get().isEmpty()) {
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(ServerConfig.SINGLE_BIOME_ID.get()));
            biomeSource = new FixedBiomeSource(biomeRegistry.getHolderOrThrow(biomeKey));
        } else if (!ServerConfig.COPY_BIOME_SOURCE_FROM_DIMENSION.get().isEmpty()) {
            biomeSource = getServerLevel(ServerConfig.COPY_BIOME_SOURCE_FROM_DIMENSION.get())
                    .getChunkSource().getGenerator().getBiomeSource();
        } else {
            Holder<MultiNoiseBiomeSourceParameterList> preset = registryAccess.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).orElseThrow()
                    .getOrThrow(getBiomeSourceKey());
            biomeSource = MultiNoiseBiomeSource.createFromPreset(preset);
        }

        ResourceKey<NoiseGeneratorSettings> noiseSettingsKey = ResourceKey.create(Registries.NOISE_SETTINGS,
                new ResourceLocation(ServerConfig.NOISE_SETTINGS.get()));
        Holder<NoiseGeneratorSettings> noiseSettings = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(noiseSettingsKey);

        CustomChunkGenerator gen = new CustomChunkGenerator(biomeSource, noiseSettings, prebuiltStructureId);

        if (!ServerConfig.FEATURE_GEN.get().shouldGenerate(false)) {
            //noinspection ConstantConditions
            ((ChunkGeneratorAccess) gen).setFeaturesPerStep(List::of);
        }

        return gen;
    }

    private static ServerLevel getServerLevel(String dimension) {
        MinecraftServer server = Objects.requireNonNull(GameInstance.getServer());
        try {
            ResourceLocation dimId = new ResourceLocation(dimension);
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
            if (level == null) {
                FTBTeamBases.LOGGER.error("invalid 'copy_biome_source_from_dimension' value '{}' (no such dimension), falling back to overworld", dimId);
                level = Objects.requireNonNull(server.getLevel(ServerLevel.OVERWORLD));
            }
            return level;
        } catch (ResourceLocationException ex) {
            FTBTeamBases.LOGGER.error("invalid 'copy_biome_source_from_dimension' value '{}' (bad resource location), falling back to overworld", dimension);
            return Objects.requireNonNull(server.getLevel(ServerLevel.OVERWORLD));
        }
    }

    private static ResourceKey<MultiNoiseBiomeSourceParameterList> getBiomeSourceKey() {
        String configVal = ServerConfig.CUSTOM_BIOME_PARAM_LIST.get();
        try {
            return ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, new ResourceLocation(configVal));
        } catch (ResourceLocationException ex) {
            FTBTeamBases.LOGGER.warn("invalid 'custom_biome_param_list' value '{}', falling back to overworld", configVal);
            return MultiNoiseBiomeSourceParameterLists.OVERWORLD;
        }
    }

    private CustomChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, ResourceLocation baseTemplateId) {
        super(biomeSource, settings);

        this.baseTemplateId = baseTemplateId;
    }

    @Override
    public ResourceLocation getBaseDefinitionId() {
        return baseTemplateId;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> holderLookup, RandomState randomState, long seed) {
        return ChunkGeneratorStructureState.createForFlat(randomState, seed, this.biomeSource,
                DimensionUtils.possibleStructures(holderLookup, baseTemplateId));
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

}

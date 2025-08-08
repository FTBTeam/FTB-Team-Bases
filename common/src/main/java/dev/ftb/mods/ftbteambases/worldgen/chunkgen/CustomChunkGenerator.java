package dev.ftb.mods.ftbteambases.worldgen.chunkgen;


import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.mixin.ChunkGeneratorAccess;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionProvider;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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
    public static final MapCodec<CustomChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter((gen) -> gen.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings),
            ResourceLocation.CODEC.optionalFieldOf("prebuilt_structure_id", FTBTeamBases.NO_TEMPLATE_ID).forGetter(CustomChunkGenerator::getBaseDefinitionId)
    ).apply(instance, instance.stable(CustomChunkGenerator::new)));

    private final ResourceLocation baseTemplateId;

    public static CustomChunkGenerator create(MinecraftServer server, RegistryAccess registryAccess, ResourceLocation prebuiltStructureId) {
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        BiomeSource biomeSource;
        if (!ServerConfig.SINGLE_BIOME_ID.get().isEmpty()) {
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME,
                    ResourceLocation.parse(ServerConfig.SINGLE_BIOME_ID.get()));
            biomeSource = new FixedBiomeSource(biomeRegistry.getHolderOrThrow(biomeKey));
        } else if (!ServerConfig.BIOME_SOURCE_FROM_DIMENSION.get().isEmpty()) {
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(ServerConfig.BIOME_SOURCE_FROM_DIMENSION.get()));
            ServerLevel level = server.getLevel(levelKey);
            if (level == null) {
                FTBTeamBases.LOGGER.error("unknown level {} in 'use_biome_source_from', falling back to overworld", levelKey.location());
                level = Objects.requireNonNull(server.getLevel(Level.OVERWORLD));
            }
            biomeSource = level.getChunkSource().getGenerator().getBiomeSource();
        } else {
            Holder<MultiNoiseBiomeSourceParameterList> preset = registryAccess.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).orElseThrow()
                    .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
            biomeSource = MultiNoiseBiomeSource.createFromPreset(preset);
        }

        ResourceKey<NoiseGeneratorSettings> noiseSettingsKey = ResourceKey.create(Registries.NOISE_SETTINGS,
                ResourceLocation.parse(ServerConfig.NOISE_SETTINGS.get()));
        Holder<NoiseGeneratorSettings> noiseSettings = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(noiseSettingsKey);

        CustomChunkGenerator gen = new CustomChunkGenerator(biomeSource, noiseSettings, prebuiltStructureId);

        if (!ServerConfig.FEATURE_GEN.get().shouldGenerate(false)) {
            //noinspection ConstantConditions
            ((ChunkGeneratorAccess) gen).setFeaturesPerStep(List::of);
        }

        return gen;
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
        return ChunkGeneratorStructureState.createForNormal(
                randomState,
                seed,
                this.biomeSource,
                holderLookup
        );
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

}

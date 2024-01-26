package dev.ftb.mods.ftbteambases.worldgen.chunkgen;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.function.BiFunction;

public enum ChunkGenerators {
    MULTI_BIOME_VOID("void", VoidChunkGenerator::create, VoidChunkGenerator.CODEC),
    CUSTOM("custom", CustomChunkGenerator::create, CustomChunkGenerator.CODEC);

    public static final NameMap<ChunkGenerators> NAME_MAP = NameMap.of(MULTI_BIOME_VOID, values()).create();

    private final ResourceLocation id;
    private final BiFunction<RegistryAccess, ResourceLocation, ChunkGenerator> factory;
    private final Codec<? extends ChunkGenerator> codec;

    ChunkGenerators(String id, BiFunction<RegistryAccess, ResourceLocation, ChunkGenerator> factory, Codec<? extends ChunkGenerator> codec) {
        this.id = FTBTeamBases.rl(id);
        this.factory = factory;
        this.codec = codec;
    }

    public static void register(DeferredRegister<Codec<? extends ChunkGenerator>> chunkGenerators) {
        for (ChunkGenerators gen : values()) {
            chunkGenerators.register(gen.id, () -> gen.codec);
        }
    }

    public ResourceLocation getId() {
        return id;
    }

    public Codec<? extends ChunkGenerator> getCodec() {
        return codec;
    }

    public ChunkGenerator makeGenerator(RegistryAccess registryAccess, ResourceLocation prebuiltStructureId) {
        return factory.apply(registryAccess, prebuiltStructureId);
    }
}

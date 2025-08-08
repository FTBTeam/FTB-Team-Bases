package dev.ftb.mods.ftbteambases.worldgen.chunkgen;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;

public enum ChunkGenerators {
    MULTI_BIOME_VOID("void", VoidChunkGenerator::create, VoidChunkGenerator.CODEC),
    CUSTOM("custom", CustomChunkGenerator::create, CustomChunkGenerator.CODEC);

    public static final NameMap<ChunkGenerators> NAME_MAP = NameMap.of(MULTI_BIOME_VOID, values()).create();

    private final ResourceLocation id;
    private final ChunkGeneratorProvider factory;
    private final MapCodec<? extends ChunkGenerator> codec;

    ChunkGenerators(String id, ChunkGeneratorProvider factory, MapCodec<? extends ChunkGenerator> codec) {
        this.id = FTBTeamBases.rl(id);
        this.factory = factory;
        this.codec = codec;
    }

    public static void register(DeferredRegister<MapCodec<? extends ChunkGenerator>> chunkGenerators) {
        for (ChunkGenerators gen : values()) {
            chunkGenerators.register(gen.id, () -> gen.codec);
        }
    }

    public ResourceLocation getId() {
        return id;
    }

    public MapCodec<? extends ChunkGenerator> getCodec() {
        return codec;
    }

    public ChunkGenerator makeGenerator(MinecraftServer server, RegistryAccess registryAccess, ResourceLocation prebuiltStructureId) {
        return factory.provide(server, registryAccess, prebuiltStructureId);
    }

    @FunctionalInterface
    public interface ChunkGeneratorProvider {
        ChunkGenerator provide(MinecraftServer server, RegistryAccess registryAccess, ResourceLocation prebuiltStructureId);
    }
}

package dev.ftb.mods.ftbteambases.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.worldgen.chunkgen.ChunkGenerators;
import dev.ftb.mods.ftbteambases.worldgen.placement.OneChunkOnlyPlacement;
import dev.ftb.mods.ftbteambases.worldgen.processor.WaterLoggingFixProcessor;
import dev.ftb.mods.ftbteambases.worldgen.structure.StartStructure;
import dev.ftb.mods.ftbteambases.worldgen.structure.StartStructurePiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public class ModWorldGen {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES
            = DeferredRegister.create(FTBTeamBases.MOD_ID, Registries.STRUCTURE_TYPE);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES
            = DeferredRegister.create(FTBTeamBases.MOD_ID, Registries.STRUCTURE_PIECE);
    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPES
            = DeferredRegister.create(FTBTeamBases.MOD_ID, Registries.STRUCTURE_PLACEMENT);
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS
            = DeferredRegister.create(FTBTeamBases.MOD_ID, Registries.STRUCTURE_PROCESSOR);
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS
            = DeferredRegister.create(FTBTeamBases.MOD_ID, Registries.CHUNK_GENERATOR);

    public static final RegistrySupplier<StructureType<StartStructure>> START_STRUCTURE
            = STRUCTURE_TYPES.register("start", () -> explicitStructureTypeTyping(StartStructure.CODEC));

    public static final RegistrySupplier<StructurePieceType.StructureTemplateType> START_STRUCTURE_PIECE
            = STRUCTURE_PIECE_TYPES.register("start", () -> StartStructurePiece::new);

    public static final RegistrySupplier<StructurePlacementType<OneChunkOnlyPlacement>> ONE_CHUNK_ONLY_PLACEMENT
            = STRUCTURE_PLACEMENT_TYPES.register("one_chunk_only", () -> explicitStructurePlacementTypeTyping(OneChunkOnlyPlacement.CODEC));

    public static final RegistrySupplier<StructureProcessorType<WaterLoggingFixProcessor>> WATER_LOGGING_FIX_PROCESSOR
            = STRUCTURE_PROCESSORS.register("waterlogging_fix_processor", () -> () -> WaterLoggingFixProcessor.CODEC);

    static {
        ChunkGenerators.register(CHUNK_GENERATORS);
    }

    private static <T extends Structure> StructureType<T> explicitStructureTypeTyping(MapCodec<T> codec) {
        return () -> codec;
    }

    private static <T extends StructurePlacement> StructurePlacementType<T> explicitStructurePlacementTypeTyping(MapCodec<T> codec) {
        return () -> codec;
    }

    public static void init() {
        STRUCTURE_TYPES.register();
        STRUCTURE_PIECE_TYPES.register();
        STRUCTURE_PLACEMENT_TYPES.register();
        STRUCTURE_PROCESSORS.register();
        CHUNK_GENERATORS.register();
    }
}

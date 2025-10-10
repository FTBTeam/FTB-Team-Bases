package dev.ftb.mods.ftbteambases.registry;

import com.mojang.serialization.MapCodec;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModWorldGen {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES
            = DeferredRegister.create(Registries.STRUCTURE_TYPE, FTBTeamBases.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES
            = DeferredRegister.create(Registries.STRUCTURE_PIECE, FTBTeamBases.MOD_ID);
    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPES
            = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, FTBTeamBases.MOD_ID);
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS
            = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, FTBTeamBases.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS
            = DeferredRegister.create(Registries.CHUNK_GENERATOR, FTBTeamBases.MOD_ID);

    public static final Supplier<StructureType<StartStructure>> START_STRUCTURE
            = STRUCTURE_TYPES.register("start", () -> explicitStructureTypeTyping(StartStructure.CODEC));

    public static final Supplier<StructurePieceType.StructureTemplateType> START_STRUCTURE_PIECE
            = STRUCTURE_PIECE_TYPES.register("start", () -> StartStructurePiece::new);

    public static final Supplier<StructurePlacementType<OneChunkOnlyPlacement>> ONE_CHUNK_ONLY_PLACEMENT
            = STRUCTURE_PLACEMENT_TYPES.register("one_chunk_only", () -> explicitStructurePlacementTypeTyping(OneChunkOnlyPlacement.CODEC));

    public static final Supplier<StructureProcessorType<WaterLoggingFixProcessor>> WATER_LOGGING_FIX_PROCESSOR
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

    public static void init(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        STRUCTURE_PIECE_TYPES.register(eventBus);
        STRUCTURE_PLACEMENT_TYPES.register(eventBus);
        STRUCTURE_PROCESSORS.register(eventBus);
        CHUNK_GENERATORS.register(eventBus);
    }
}

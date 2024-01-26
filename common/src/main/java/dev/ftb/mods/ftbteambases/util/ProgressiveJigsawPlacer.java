package dev.ftb.mods.ftbteambases.util;

import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.definition.JigsawParams;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class ProgressiveJigsawPlacer {
    private final CommandSourceStack source;
    private final JigsawParams jigsawParams;
    private final ServerLevel level;
    private final BlockPos startPos;
    private WorkData workData;

    public ProgressiveJigsawPlacer(CommandSourceStack source, JigsawParams jigsawParams, ServerLevel level, BlockPos startPos) {
        this.source = source;
        this.jigsawParams = jigsawParams;
        this.level = level;
        this.startPos = startPos;
    }

    public void start() {
        level.setBlock(startPos, Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, jigsawParams.jigsawOrientation()), 0);

        if (level.getBlockEntity(startPos) instanceof JigsawBlockEntity jbe) {
            jbe.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, jigsawParams.templatePool()));
            jbe.setTarget(jigsawParams.target());
            jbe.setJoint(jigsawParams.jointType());
            workData = setupPieceQueue(level, jbe, jigsawParams.maxGenerationDepth());
        } else {
            throw new FTBTeamBasesException("could not get jigsaw block entity at " + level.dimension().location() + " / " + startPos);
        }
    }

    private WorkData setupPieceQueue(ServerLevel level, JigsawBlockEntity jbe, int maxDepth) {
        ChunkGenerator chunkgenerator = level.getChunkSource().getGenerator();
        StructureTemplateManager structuretemplatemanager = level.getStructureManager();
        BlockPos origin = jbe.getBlockPos().relative(jbe.getBlockState().getValue(JigsawBlock.ORIENTATION).front());
        Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), chunkgenerator,
                chunkgenerator.getBiomeSource(), level.getChunkSource().randomState(), structuretemplatemanager,
                level.getSeed(), new ChunkPos(origin), level, biome -> true);
        Holder<StructureTemplatePool> holder = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL)
                .getHolderOrThrow(jbe.getPool());

        return JigsawPlacement.addPieces(context, holder, Optional.of(jbe.getTarget()), maxDepth, origin, false, Optional.empty(), 128)
                .map(stub -> {
                    StructurePiecesBuilder builder = stub.getPiecesBuilder();
                    ArrayDeque<WorkUnit> units = new ArrayDeque<>(builder.build().pieces().stream()
                            .filter(piece -> piece instanceof PoolElementStructurePiece)
                            .map(piece -> new WorkUnit(origin, (PoolElementStructurePiece) piece))
                            .toList());
                    return new WorkData(level, units.size(), units);
                }).orElse(null);
    }

    public float getProgress() {
        return 1f - ((float) workData.work().size() / workData.totalSize());
    }

    public boolean tick() {
        if (workData == null) {
            return true;
        }
        WorkUnit workUnit = workData.work().pollFirst();
        if (workUnit == null) {
            return true;  // no more work
        }
        ServerLevel level = workData.level();
        if (level != null) {
            StructureManager structureManager = level.structureManager();
            ChunkGenerator chunkgenerator = level.getChunkSource().getGenerator();
            RandomSource random = level.getRandom();

            workUnit.piece().place(level, structureManager, chunkgenerator, random, BoundingBox.infinite(), workUnit.pos(), false);

            return !workData.work().isEmpty();
        } else {
            // shouldn't get here, but just in case...
            return true;
        }
    }

    public CommandSourceStack getSource() {
        return source;
    }

    private record WorkData(ServerLevel level, int totalSize, Deque<WorkUnit> work) {
    }

    private record WorkUnit(BlockPos pos, PoolElementStructurePiece piece) {
    }
}

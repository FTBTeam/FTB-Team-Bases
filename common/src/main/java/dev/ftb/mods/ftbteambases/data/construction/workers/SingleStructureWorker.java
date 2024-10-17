package dev.ftb.mods.ftbteambases.data.construction.workers;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.SingleStructure;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SingleStructureWorker extends AbstractStructureWorker {
    private final ServerPlayer player;
    private final SingleStructure singleStructure;

    public SingleStructureWorker(ServerPlayer player, BaseDefinition baseDefinition, SingleStructure singleStructure, boolean privateDimension) {
        super(player, baseDefinition, privateDimension);

        this.player = player;
        this.singleStructure = singleStructure;
    }

    @Override
    public void startConstruction(BooleanConsumer onCompleted) {
        super.startConstruction(onCompleted);

        ServerLevel serverLevel = getOrCreateLevel(player.getServer());

        StructureTemplate template = serverLevel.getStructureManager().getOrCreate(singleStructure.structureLocation());
        StructurePlaceSettings placeSettings = DimensionUtils.makePlacementSettings(template, singleStructure.includeEntities());

        BlockPos origin = getPlacementOrigin(serverLevel, getSpawnXZ(), singleStructure.yPos());
        BlockPos templatePos = origin.offset(-(template.getSize().getX() / 2), 0, -(template.getSize().getZ() / 2));

        ChunkPos cp = new ChunkPos(templatePos);
        if (serverLevel.getChunkSource().getChunk(cp.x, cp.z, ChunkStatus.FULL, true) == null) {
            throw new FTBTeamBasesException("Single Structure Worker: can't load chunk at " + getDimension().location() + " / " + cp);
        }

        template.placeInWorld(serverLevel, templatePos, templatePos, placeSettings, serverLevel.random, Block.UPDATE_ALL);

        postProcess(template, templatePos, placeSettings, serverLevel);
    }

    private void postProcess(StructureTemplate template, BlockPos origin, StructurePlaceSettings placeSettings, ServerLevel serverLevel) {
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, placeSettings, Blocks.STRUCTURE_BLOCK)) {
            serverLevel.setBlock(info.pos().offset(origin), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, placeSettings, Blocks.JIGSAW)) {
            if (info.nbt() != null) {
                String stateName = info.nbt().getString("final_state");
                try {
                    BlockState state = BlockStateParser.parseForBlock(serverLevel.holderLookup(Registries.BLOCK), stateName, true).blockState();
                    serverLevel.setBlock(info.pos().offset(origin), state, Block.UPDATE_ALL);
                } catch (CommandSyntaxException e) {
                    FTBTeamBases.LOGGER.error("Error while parsing blockstate {} in jigsaw block @ {} : {}", stateName, info.pos(), e.getMessage());
                    serverLevel.setBlock(info.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    public void tick() {
        onCompleted.accept(true);
    }

//    private BlockPos originAtYpos(ServerLevel level, XZ xz) {
//        int x = xz.x();
//        int z = xz.z();
//        return singleStructure.yPos()
//                .map(y -> new BlockPos(x, y, z))
//                .orElse(new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z));
//    }
}

package dev.ftb.mods.ftbteambases.worldgen.structure;

import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.registry.ModWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class StartStructurePiece extends TemplateStructurePiece {
	public final ResourceLocation startId;

	public StartStructurePiece(StructureTemplateManager structureManager, ResourceLocation id, BlockPos pos, StructureTemplate template) {
		super(ModWorldGen.START_STRUCTURE_PIECE.get(), 0, structureManager, id, id.toString(), DimensionUtils.makePlacementSettings(template), pos);
		startId = id;
	}

	public StartStructurePiece(StructureTemplateManager structureManager, CompoundTag tag) {
		super(ModWorldGen.START_STRUCTURE_PIECE.get(), tag, structureManager, id -> DimensionUtils.makePlacementSettings(structureManager.getOrCreate(id)));
		startId = ResourceLocation.parse(tag.getString("Template"));
	}

	@Override
	protected void handleDataMarker(String id, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox boundingBox) {
		if (id.equalsIgnoreCase("spawn_point")) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			level.getLevel().getServer().getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, level.getLevel().getServer());

			FTBTeamBases.LOGGER.info("Found valid spawn marker at [{}] and setting for [{}]", pos, level.getLevel().dimension());
		} else {
			FTBTeamBases.LOGGER.warn("No spawn_point tag found on data marker");
		}
	}
}

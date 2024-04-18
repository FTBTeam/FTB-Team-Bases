package dev.ftb.mods.ftbteambases.util;

import com.google.common.collect.ImmutableList;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.data.bases.LiveBaseDetails;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import dev.ftb.mods.ftbteambases.worldgen.chunkgen.VoidChunkGenerator;
import dev.ftb.mods.ftbteambases.worldgen.processor.WaterLoggingFixProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class DimensionUtils {
    private static final BlockIgnoreProcessor IGNORE_PROCESSOR = new BlockIgnoreProcessor(ImmutableList.of(Blocks.STRUCTURE_VOID, Blocks.STRUCTURE_BLOCK));

    public static Optional<BlockPos> locateSpawn(StructureTemplate template) {
        StructurePlaceSettings placeSettings = makePlacementSettings(template);

        for (var info : template.filterBlocks(BlockPos.ZERO, placeSettings, Blocks.STRUCTURE_BLOCK)) {
            if (info.nbt() != null && StructureMode.valueOf(info.nbt().getString("mode")) == StructureMode.DATA) {
                FTBTeamBases.LOGGER.info("Found data block at [{}] with data [{}]", info.pos(), info.nbt().getString("metadata"));

                if (info.nbt().getString("metadata").equalsIgnoreCase("spawn_point")) {
                    return Optional.of(info.pos());
                }
            }
        }

        return Optional.empty();
    }

    public static StructurePlaceSettings makePlacementSettings(StructureTemplate template, boolean includeEntities) {
        Vec3i size = template.getSize();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setIgnoreEntities(!includeEntities);
        settings.addProcessor(IGNORE_PROCESSOR);
        settings.addProcessor(WaterLoggingFixProcessor.INSTANCE);
        settings.setRotationPivot(new BlockPos(size.getX() / 2, size.getY() / 2, size.getZ() / 2));
        settings.setRotation(Rotation.NONE);
        return settings;
    }

    public static StructurePlaceSettings makePlacementSettings(StructureTemplate template) {
        return makePlacementSettings(template, ServerConfig.ENTITIES_IN_START_STRUCTURE.get());
    }

    public static boolean isTeamDimension(Level level) {
        return level.dimension().location().getNamespace().equals(FTBTeamBases.MOD_ID);
    }

    public static boolean isPortalDimension(Level level) {
        return ServerConfig.ALLOW_NETHER_PORTALS.get() && isTeamDimension(level);
    }

    public static boolean isVoidChunkGen(ChunkGenerator chunkGenerator) {
        return chunkGenerator instanceof VoidChunkGenerator;
    }

    public static void clearPlayerInventory(ServerPlayer serverPlayer) {
        serverPlayer.getInventory().clearOrCountMatchingItems(arg -> true, -1, serverPlayer.inventoryMenu.getCraftSlots());
        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.slotsChanged(serverPlayer.getInventory());
    }

    @NotNull
    public static Stream<Holder<StructureSet>> possibleStructures(HolderLookup<StructureSet> holderLookup, ResourceLocation baseTemplateId) {
        return BaseDefinitionManager.getServerInstance().getBaseDefinition(baseTemplateId).map(baseTemplate ->
                baseTemplate.constructionType().prebuilt().map(prebuilt -> {
                    ResourceLocation setId = prebuilt.structureSetId().orElse(BaseDefinition.DEFAULT_STRUCTURE_SET);
                    return holderLookup.getOrThrow(TagKey.create(Registries.STRUCTURE_SET, setId)).stream();
                }).orElse(Stream.of())
        ).orElse(Stream.of());
    }

    public static boolean teleport(ServerPlayer player, ResourceKey<Level> key, BlockPos destPos) {
        ServerLevel level = player.server.getLevel(key);

        if (level != null) {
            if (key.equals(Level.OVERWORLD)) {
                BlockPos lobbySpawnPos = BaseInstanceManager.get(player.server).getLobbySpawnPos();
                BlockPos pos = Objects.requireNonNullElse(destPos, lobbySpawnPos);

                doTeleport(player, level, pos);
            } else {
                Vec3 vec;
                if (destPos == null) {
                    vec = new Vec3(0.5D, 1.1D, 0.5D);
                    BlockPos respawnPosition = player.getRespawnPosition();
                    if (player.getRespawnDimension().equals(key) && respawnPosition != null) {
                        vec = vec.add(new Vec3(respawnPosition.getX(), respawnPosition.getY(), respawnPosition.getZ()));
                    } else {
                        BlockPos levelSharedSpawn = BaseInstanceManager.get(player.server).getBaseForPlayer(player)
                                        .map(LiveBaseDetails::spawnPos).orElse(BlockPos.ZERO);
                        vec = vec.add(new Vec3(levelSharedSpawn.getX(), levelSharedSpawn.getY(), levelSharedSpawn.getZ()));
                    }
                } else {
                    vec = Vec3.atCenterOf(destPos);
                }

                doTeleport(player, level, BlockPos.containing(vec.x, vec.y, vec.z));
            }
            return true;
        } else {
            FTBTeamBases.LOGGER.error("Failed to teleport {} to {} (bad level key)", player.getScoreboardName(), key.location());
            return false;
        }
    }

    private static void doTeleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        ChunkPos chunkpos = new ChunkPos(pos);
        level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, player.getId());
        player.stopRiding();
        player.teleportTo(level, pos.getX() + .5D, pos.getY() + .01D, pos.getZ() + .5D, player.getYRot(), player.getXRot());

        FTBTeamBases.LOGGER.debug("teleported {} to {} in {}", player.getGameProfile().getName(), pos, level.dimension().location());
    }
}

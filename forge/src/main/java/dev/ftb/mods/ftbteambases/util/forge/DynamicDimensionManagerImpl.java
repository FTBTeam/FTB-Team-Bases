package dev.ftb.mods.ftbteambases.util.forge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Lifecycle;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.PrebuiltStructure;
import dev.ftb.mods.ftbteambases.net.UpdateDimensionsListMessage;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * Thanks to McJty and Commoble for providing this code.
 * See original DynamicDimensionManager in RF Tools Dimensions for comments and more generic example.
 */
public class DynamicDimensionManagerImpl {
    public static ServerLevel create(MinecraftServer server, ResourceKey<Level> worldKey, BaseDefinition baseDefinition) {
        @SuppressWarnings("deprecation")
        Map<ResourceKey<Level>, ServerLevel> map = server.forgeGetWorldMap();

        ServerLevel existingLevel = map.get(worldKey);

        if (existingLevel != null) {
            return existingLevel;
        }

        RegistryAccess registryAccess = server.registryAccess();
        ServerLevel overworld = Objects.requireNonNull(server.getLevel(Level.OVERWORLD));
        ResourceKey<LevelStem> dimensionKey = ResourceKey.create(Registries.LEVEL_STEM, worldKey.location());

        ResourceLocation dimensionTypeId = baseDefinition.dimensionSettings().dimensionType().orElse(BaseDefinition.DEFAULT_DIMENSION_TYPE);
        Holder<DimensionType> typeHolder = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(
                ResourceKey.create(Registries.DIMENSION_TYPE, dimensionTypeId)
        );

        ResourceLocation prebuiltStructureId = baseDefinition.constructionType().prebuilt()
                .map(PrebuiltStructure::startStructure).orElse(FTBTeamBases.NO_TEMPLATE_ID);
        ChunkGenerator chunkGenerator = ServerConfig.CHUNK_GENERATOR.get()
                .makeGenerator(registryAccess, prebuiltStructureId);

        LevelStem dimension = new LevelStem(typeHolder, chunkGenerator);

        final ChunkProgressListener chunkProgressListener = server.progressListenerFactory.create(11);
        final Executor executor = server.executor;
        final LevelStorageSource.LevelStorageAccess anvilConverter = server.storageSource;
        final WorldData worldData = server.getWorldData();
        final WorldOptions worldGenSettings = worldData.worldGenOptions();
        final DerivedLevelData derivedLevelData = new DerivedLevelData(worldData, worldData.overworldData());

        // now we have everything we need to create the dimension and the level
        // this is the same order server init creates levels:
        // the dimensions are already registered when levels are created, we'll do that first
        // then instantiate level, add border listener, add to map, fire world load event

        // register the actual dimension
        LayeredRegistryAccess<RegistryLayer> registries = server.registries();
        RegistryAccess.ImmutableRegistryAccess composite = (RegistryAccess.ImmutableRegistryAccess)registries.compositeAccess();

        Map<ResourceKey<? extends Registry<?>>, Registry<?>> regmap = new HashMap<>(composite.registries);
        ResourceKey<? extends Registry<?>> key = ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation("root")),new ResourceLocation("dimension"));
        MappedRegistry<LevelStem> oldRegistry = (MappedRegistry<LevelStem>) regmap.get(key);
        Lifecycle oldLifecycle = oldRegistry.registryLifecycle();

        final MappedRegistry<LevelStem> newRegistry = new MappedRegistry<>(Registries.LEVEL_STEM, oldLifecycle, false);
        for (var entry : oldRegistry.entrySet()) {
            final ResourceKey<LevelStem> oldKey = entry.getKey();
            final ResourceKey<Level> oldLevelKey = ResourceKey.create(Registries.DIMENSION, oldKey.location());
            final LevelStem dim = entry.getValue();
            if (dim != null && oldLevelKey != worldKey) {
                Registry.register(newRegistry, oldKey, dim);
            }
        }
        Registry.register(newRegistry, dimensionKey, dimension);
        regmap.replace(key, newRegistry);
        Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> newmap = (Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>>) regmap;
        composite.registries = newmap;

        // create the world instance
        final ServerLevel newWorld = new ServerLevel(
                server,
                executor,
                anvilConverter,
                derivedLevelData,
                worldKey,
                dimension,
                chunkProgressListener,
                false, // @todo 1.19.3 worldGenSettings.isDebug(),
                net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(worldGenSettings.seed()),
                ImmutableList.of(), // "special spawn list"
                // phantoms, travelling traders, patrolling/sieging raiders, and cats are overworld special spawns
                // this is always empty for non-overworld dimensions (including json dimensions)
                // these spawners are ticked when the world ticks to do their spawning logic,
                // mods that need "special spawns" for their own dimensions should implement them via tick events or other systems
                false, // "tick time", true for overworld, always false for nether, end, and json dimensions
                null // @todo 1.20, what is this?
        );

        // add world border listener, for parity with json dimensions
        // the vanilla behaviour is that world borders exist in every dimension simultaneously with the same size and position
        // these border listeners are automatically added to the overworld as worlds are loaded, so we should do that here too
        // TODO if world-specific world borders are ever added, change it here too
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(newWorld.getWorldBorder()));

        // register level
        map.put(worldKey, newWorld);

        // update forge's world cache so the new level can be ticked
        server.markWorldsDirty();

        // fire world load event
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(newWorld));

        // update clients' dimension lists
        new UpdateDimensionsListMessage(List.of(worldKey), true).sendToAll(server);

        return newWorld;
    }

    public static void destroy_Internal(final MinecraftServer server, Set<ResourceKey<Level>> keysToRemove) {
        // we need to remove the dimension/world form three places
        // the server's dimension registry, the server's world registry, and the overworld's world border listener
        // the world registry is just a simple map and the world border listener has a remove() method
        // the dimension registry has five sub-collections that need to be cleaned up
        // we should also eject players from the removed worlds or they could get stuck there

        final WorldOptions worldGenSettings = server.getWorldData().worldGenOptions();
        final Set<ResourceKey<Level>> removedLevelKeys = new HashSet<>();
        final ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        for (final ResourceKey<Level> levelKeyToRemove : keysToRemove) {
            ServerLevel removedLevel = server.forgeGetWorldMap().remove(levelKeyToRemove); // null if the specified key was not present
            if (removedLevel != null) {
                // if we removed the key from the map
                // eject players from dead world
                // iterate over a copy as the world will remove players from the original list
                for (final ServerPlayer player : Lists.newArrayList(removedLevel.players())) {
                    // send players to their respawn point
                    ResourceKey<Level> respawnKey = player.getRespawnDimension();
                    // if we're removing their respawn world then just send them to the overworld
                    if (keysToRemove.contains(respawnKey)) {
                        respawnKey = Level.OVERWORLD;
                        player.setRespawnPosition(Level.OVERWORLD, null, 0, false, false);
                    }
                    if (respawnKey == null) {
                        respawnKey = Level.OVERWORLD;
                    }
                    final ServerLevel destinationLevel = server.getLevel(respawnKey);
                    BlockPos destinationPos = player.getRespawnPosition();
                    if (destinationPos == null) {
                        destinationPos = destinationLevel.getSharedSpawnPos();
                    }
                    final float respawnAngle = player.getRespawnAngle();
                    // "respawning" the player via the player list schedules a task in the server to run after the post-server tick
                    // that causes some minor logspam due to the player's world no longer being loaded
                    // teleporting the player this way instead avoids this
                    player.teleportTo(destinationLevel, destinationPos.getX(), destinationPos.getY(), destinationPos.getZ(), respawnAngle, 0F);
                }
                // save the world now or it won't be saved later and data that may be wanted to be kept may be lost
                removedLevel.save(null, false, removedLevel.noSave());

                // fire world unload event -- when the server stops, this would fire after worlds get saved, so we'll do that here too
                MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(removedLevel));

                // remove the world border listener if possible
                final WorldBorder overworldBorder = overworld.getWorldBorder();
                final WorldBorder removedWorldBorder = removedLevel.getWorldBorder();
                final List<BorderChangeListener> listeners = overworldBorder.listeners;
                BorderChangeListener targetListener = null;
                for (BorderChangeListener listener : listeners) {
                    if (listener instanceof BorderChangeListener.DelegateBorderChangeListener && removedWorldBorder == ((BorderChangeListener.DelegateBorderChangeListener) listener).worldBorder) {
                        targetListener = listener;
                        break;
                    }
                }
                if (targetListener != null) {
                    overworldBorder.removeListener(targetListener);
                }

                // track the removed world
                removedLevelKeys.add(levelKeyToRemove);
            }
        }

        if (!removedLevelKeys.isEmpty()) {
            // replace the old dimension registry with a new one containing the dimensions that weren't removed, in the same order

            LayeredRegistryAccess<RegistryLayer> registries = server.registries();
            RegistryAccess.ImmutableRegistryAccess composite = (RegistryAccess.ImmutableRegistryAccess)registries.compositeAccess();

            // @todo 1.19.3
//            Map<? extends ResourceKey<?>,? extends Registry<?>> map = composite.registries;

            Map<ResourceKey<?>,Registry<?>> hashMap = new HashMap<>(); // @todo 1.19.3 map
            ResourceKey<?> key = ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation("root")),new ResourceLocation("dimension"));

            final Registry<LevelStem> oldRegistry = (Registry<LevelStem>) hashMap.get(key);
            Lifecycle oldLifecycle = null; // @todo 1.19.3 AT ((MappedRegistry<LevelStem>)oldRegistry).registryLifecycle;
            final Registry<LevelStem> newRegistry = new MappedRegistry<>(Registries.LEVEL_STEM, oldLifecycle, false);

            for (var entry : oldRegistry.entrySet()) {
                final ResourceKey<LevelStem> oldKey = entry.getKey();
                final ResourceKey<Level> oldLevelKey = ResourceKey.create(Registries.DIMENSION, oldKey.location());
                final LevelStem dimension = entry.getValue();
                if (oldKey != null && dimension != null && !removedLevelKeys.contains(oldLevelKey)) {
                    if (newRegistry instanceof MappedRegistry<LevelStem> mappedRegistry) {
                        mappedRegistry.unfreeze();
                    }
                    Registry.register(newRegistry, oldKey, dimension);   // @todo 1.18.2 is this right?
                }
            }
            hashMap.replace(key, newRegistry);

            // then replace the old registry with the new registry
            // @todo 1.19.3
//            composite.registries = hashMap;

            // update the server's levels so dead levels don't get ticked
            server.markWorldsDirty();
            // clients will need to be notified of the removed level for the dimension command suggester
            new UpdateDimensionsListMessage(removedLevelKeys, false).sendToAll(server);
        }
    }


}

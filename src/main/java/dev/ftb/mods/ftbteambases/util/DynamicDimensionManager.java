package dev.ftb.mods.ftbteambases.util;

import com.google.common.collect.ImmutableSet;
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
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * Thanks to McJty and Commoble for providing this code.
 * See original DynamicDimensionManager in RF Tools Dimensions for comments and more generic example.
 */
public class DynamicDimensionManager {
	private static final Set<ResourceKey<Level>> VANILLA_WORLDS = ImmutableSet.of(Level.OVERWORLD, Level.NETHER, Level.END);
	private static final RegistrationInfo DIMENSION_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.stable());
	private static Set<ResourceKey<Level>> pendingLevelsToUnregister = new HashSet<>();

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
				.makeGenerator(server, registryAccess, prebuiltStructureId);

		LevelStem dimension = new LevelStem(typeHolder, chunkGenerator);

		final ChunkProgressListener chunkProgressListener = server.progressListenerFactory.create(11);
		final Executor executor = server.executor;
		final LevelStorageSource.LevelStorageAccess anvilConverter = server.storageSource;
		final WorldData worldData = server.getWorldData();
		final DerivedLevelData derivedLevelData = new DerivedLevelData(worldData, worldData.overworldData());

		// now we have everything we need to create the dimension and the level
		// this is the same order server init creates levels:
		// the dimensions are already registered when levels are created, we'll do that first
		// then instantiate level, add border listener, add to map, fire world load event

		// register the actual dimension
		Registry<LevelStem> dimensionRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
		if (dimensionRegistry instanceof MappedRegistry<LevelStem> writableRegistry) {
			writableRegistry.unfreeze();
			writableRegistry.register(dimensionKey, dimension, DIMENSION_REGISTRATION_INFO);
		} else {
			throw new IllegalStateException(String.format("Unable to register dimension %s -- dimension registry not writable", dimensionKey.location()));
		}

		// create the level instance
		final ServerLevel newLevel = new ServerLevel(
				server,
				executor,
				anvilConverter,
				derivedLevelData,
				worldKey,
				dimension,
				chunkProgressListener,
				worldData.isDebugWorld(),
				overworld.getSeed(), // don't need to call BiomeManager#obfuscateSeed, overworld seed is already obfuscated
				List.of(), // "special spawn list"
				// phantoms, travelling traders, patrolling/sieging raiders, and cats are overworld special spawns
				// this is always empty for non-overworld dimensions (including json dimensions)
				// these spawners are ticked when the world ticks to do their spawning logic,
				// mods that need "special spawns" for their own dimensions should implement them via tick events or other systems
				false, // "tick time", true for overworld, always false for nether, end, and json dimensions
				null // as of 1.20.1 this argument is always null in vanilla, indicating the level should load the sequence from storage
		);

		// add world border listener, for parity with json dimensions
		// the vanilla behaviour is that world borders exist in every dimension simultaneously with the same size and position
		// these border listeners are automatically added to the overworld as worlds are loaded, so we should do that here too
		// TODO if world-specific world borders are ever added, change it here too
		overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(newLevel.getWorldBorder()));

		// register level
		map.put(worldKey, newLevel);

		// update forge's world cache so the new level can be ticked
		server.markWorldsDirty();

		// fire world load event
		NeoForge.EVENT_BUS.post(new LevelEvent.Load(newLevel));

		// update clients' dimension lists
		PacketDistributor.sendToAllPlayers(new UpdateDimensionsListMessage(List.of(worldKey), true));

		return newLevel;
	}

	/**
	 * Marks a level and its levelstem for unregistration. Unregistered levels will stop ticking,
	 * unregistered levelstems will not be loaded on server startup unless and until they are reregistered again.
	 * <p>
	 * Unregistration is delayed until the end of the server tick (just after the post-server-tick-event fires).
	 * <p>
	 * Players who are still in the given level at that time will be ejected to their respawn points.
	 * Players who have respawn points in levels being unloaded will have their spawn points reset to the overworld and respawned there.
	 * <p>
	 * Unregistering a level does not delete the region files or other data associated with the level's level folder.
	 * If a level is reregistered after unregistering it, the level will retain all prior data (unless manually deleted via server admin)
	 *
	 * @param levelToRemove The key for the level to schedule for unregistration. Vanilla dimensions are not removable as they are
	 *                      generally assumed to exist (especially the overworld)
	 * @apiNote Not intended for use with vanilla or json dimensions, doing so may cause strange problems.
	 * <p>
	 * However, if a vanilla or json dimension *is* removed, restarting the server will reconstitute it as
	 * vanilla automatically detects and registers these.
	 * <p>
	 * Mods whose dynamic dimensions require the ejection of players to somewhere other than their respawn point
	 * should teleport these worlds' players to appropriate locations before unregistering their dimensions.
	 */
	public static void markDimensionForUnregistration(final MinecraftServer server, final ResourceKey<Level> levelToRemove) {
		if (!VANILLA_WORLDS.contains(levelToRemove)) {
			pendingLevelsToUnregister.add(levelToRemove);
		}
	}

	/**
	 * @return an immutable view of the levels pending to be unregistered and unloaded at the end of the current server tick
	 */
	public static Set<ResourceKey<Level>> getWorldsPendingUnregistration() {
		return Collections.unmodifiableSet(pendingLevelsToUnregister);
	}

	/**
	 * called at the end of the server tick just before the post-server-tick-event
	 */
	public static void unregisterScheduledDimensions(MinecraftServer server) {
		if (!pendingLevelsToUnregister.isEmpty()) {
			final Set<ResourceKey<Level>> keysToRemove = DynamicDimensionManager.pendingLevelsToUnregister;
			pendingLevelsToUnregister = new HashSet<>();

			destroy_Internal(server, keysToRemove);
		}
	}

	public static void destroy_Internal(MinecraftServer server, Set<ResourceKey<Level>> keysToRemove) {
		// we need to remove the dimension/level from three places:
		// the server's dimension/levelstem registry, the server's level registry, and
		// the overworld's border listener
		// the level registry is just a simple map and the border listener has a remove() method
		// the dimension registry has five sub-collections that need to be cleaned up
		// we should also eject players from removed worlds so they don't get stuck there

		final Registry<LevelStem> oldRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
		if (!(oldRegistry instanceof MappedRegistry<LevelStem> oldMappedRegistry)) {
			FTBTeamBases.LOGGER.warn("Cannot unload dimensions: dimension registry not an instance of MappedRegistry.");
			return;
		}
		LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess = ReflectionBuddy.MinecraftServerAccess.registries.apply(server);
		RegistryAccess.Frozen composite = ReflectionBuddy.LayeredRegistryAccessAccess.composite.apply(layeredRegistryAccess);
		if (!(composite instanceof RegistryAccess.ImmutableRegistryAccess immutableRegistryAccess)) {
			FTBTeamBases.LOGGER.warn("Cannot unload dimensions: composite registry not an instance of ImmutableRegistryAccess.");
			return;
		}

		final Set<ResourceKey<Level>> removedLevelKeys = new HashSet<>();
		final ServerLevel overworld = server.getLevel(Level.OVERWORLD);

		for (final ResourceKey<Level> levelKeyToRemove : keysToRemove) {
			final @Nullable ServerLevel levelToRemove = server.getLevel(levelKeyToRemove);
			if (levelToRemove == null)
				continue;

//            UnregisterDimensionEvent unregisterDimensionEvent = new UnregisterDimensionEvent(levelToRemove);
//            NeoForge.EVENT_BUS.post(unregisterDimensionEvent);
//            if (unregisterDimensionEvent.isCanceled())
//                continue;

			// null if specified level not present
			final @Nullable ServerLevel removedLevel = server.forgeGetWorldMap().remove(levelKeyToRemove);

			if (removedLevel != null) // if we removed the key from the map
			{
				// eject players from dead world
				// iterate over a copy as the world will remove players from the original list
				for (final ServerPlayer player : Lists.newArrayList(removedLevel.players())) {
					// send players to their respawn point
					ResourceKey<Level> respawnKey = player.getRespawnDimension();
					// if we're removing their respawn world then just send them to the overworld
					if (keysToRemove.contains(respawnKey)) {
						respawnKey = Level.OVERWORLD;
						player.setRespawnPosition(respawnKey, null, 0, false, false);
					}
					if (respawnKey == null) {
						respawnKey = Level.OVERWORLD;
					}
					@Nullable ServerLevel destinationLevel = server.getLevel(respawnKey);
					if (destinationLevel == null) {
						destinationLevel = overworld;
					}

					@Nullable
					BlockPos destinationPos = player.getRespawnPosition();
					if (destinationPos == null) {
						destinationPos = destinationLevel.getSharedSpawnPos();
					}

					final float respawnAngle = player.getRespawnAngle();
					// "respawning" the player via the player list schedules a task in the server to
					// run after the post-server tick
					// that causes some minor logspam due to the player's world no longer being
					// loaded
					// teleporting the player via a teleport avoids this
					player.teleportTo(destinationLevel, destinationPos.getX(), destinationPos.getY(), destinationPos.getZ(), respawnAngle, 0F);
				}
				// save the world now or it won't be saved later and data that may be wanted to
				// be kept may be lost
				removedLevel.save(null, false, removedLevel.noSave());

				// fire world unload event -- when the server stops, this would fire after
				// worlds get saved, we'll do that here too
				NeoForge.EVENT_BUS.post(new LevelEvent.Unload(removedLevel));

				// remove the world border listener if possible
				final WorldBorder overworldBorder = overworld.getWorldBorder();
				final WorldBorder removedWorldBorder = removedLevel.getWorldBorder();
				final List<BorderChangeListener> listeners = ReflectionBuddy.WorldBorderAccess.listeners.apply(overworldBorder);
				BorderChangeListener targetListener = null;
				for (BorderChangeListener listener : listeners) {
					if (listener instanceof BorderChangeListener.DelegateBorderChangeListener delegate
							&& removedWorldBorder == ReflectionBuddy.DelegateBorderChangeListenerAccess.worldBorder.apply(delegate)) {
						targetListener = listener;
						break;
					}
				}
				if (targetListener != null) {
					overworldBorder.removeListener(targetListener);
				}

				// track the removed level
				removedLevelKeys.add(levelKeyToRemove);
			}
		}

		if (!removedLevelKeys.isEmpty()) {
			// replace the old dimension registry with a new one containing the dimensions
			// that weren't removed, in the same order
			final MappedRegistry<LevelStem> newRegistry = new MappedRegistry<>(Registries.LEVEL_STEM, oldMappedRegistry.registryLifecycle());

			for (final var entry : oldRegistry.entrySet()) {
				final ResourceKey<LevelStem> oldKey = entry.getKey();
				final ResourceKey<Level> oldLevelKey = ResourceKey.create(Registries.DIMENSION, oldKey.location());
				final LevelStem dimension = entry.getValue();
				if (oldKey != null && dimension != null && !removedLevelKeys.contains(oldLevelKey)) {
					newRegistry.register(oldKey, dimension, oldRegistry.registrationInfo(oldKey).orElse(DIMENSION_REGISTRATION_INFO));
				}
			}

			// then replace the old registry with the new registry
			// as of 1.20.1 the dimension registry is stored in the server's layered registryaccess
			// this has several immutable collections of sub-registryaccesses,
			// so we'll need to recreate each of them.

			// Each ServerLevel has a reference to the layered registry access's *composite* registry access
			// so we should edit the internal fields where possible (instead of reconstructing the registry accesses)

			List<RegistryAccess.Frozen> newRegistryAccessList = new ArrayList<>();
			for (RegistryLayer layer : RegistryLayer.values()) {
				if (layer == RegistryLayer.DIMENSIONS) {
					newRegistryAccessList.add(new RegistryAccess.ImmutableRegistryAccess(List.of(newRegistry)).freeze());
				} else {
					newRegistryAccessList.add(layeredRegistryAccess.getLayer(layer));
				}
			}
			Map<ResourceKey<? extends Registry<?>>, Registry<?>> newRegistryMap = new HashMap<>();
			for (var registryAccess : newRegistryAccessList) {
				var registries = registryAccess.registries().toList();
				for (var registryEntry : registries) {
					newRegistryMap.put(registryEntry.key(), registryEntry.value());
				}
			}
			ReflectionBuddy.LayeredRegistryAccessAccess.values.set(layeredRegistryAccess, List.copyOf(newRegistryAccessList));
			ReflectionBuddy.ImmutableRegistryAccessAccess.registries.set(immutableRegistryAccess, newRegistryMap);

			// update the server's levels so dead levels don't get ticked
			server.markWorldsDirty();

			// notify client of the removed levels
			PacketDistributor.sendToAllPlayers(new UpdateDimensionsListMessage(List.copyOf(removedLevelKeys), false));
		}
	}

}

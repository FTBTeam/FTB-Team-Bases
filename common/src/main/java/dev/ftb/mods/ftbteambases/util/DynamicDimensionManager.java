package dev.ftb.mods.ftbteambases.util;

import com.google.common.collect.ImmutableSet;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Thanks to McJty and Commoble for providing this code.
 * See original DynamicDimensionManager in RF Tools Dimensions for comments and more generic example.
 */
public class DynamicDimensionManager {
	private static final Set<ResourceKey<Level>> VANILLA_WORLDS = ImmutableSet.of(Level.OVERWORLD, Level.NETHER, Level.END);
	private static Set<ResourceKey<Level>> pendingLevelsToUnregister = new HashSet<>();

	@ExpectPlatform
	public static ServerLevel create(MinecraftServer server, ResourceKey<Level> key, BaseDefinition baseDefinition) {
		throw new AssertionError();
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

	@ExpectPlatform
	public static void
	destroy_Internal(MinecraftServer server, Set<ResourceKey<Level>> keysToRemove) {
		throw new AssertionError();
	}

}

package dev.ftb.mods.ftbteambases.util.fabric;

import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Set;

public class DynamicDimensionManagerImpl {
    public static ServerLevel create(MinecraftServer server, ResourceKey<Level> worldKey, BaseDefinition baseDefinition) {
        throw new UnsupportedOperationException("dynamic dimensions aren't implemented yet on Fabric");
    }

    public static void destroy_Internal(MinecraftServer server, Set<ResourceKey<Level>> keysToRemove) {
        throw new UnsupportedOperationException("dynamic dimensions aren't implemented yet on Fabric");
    }
}

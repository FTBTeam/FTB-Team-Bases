package dev.ftb.mods.ftbteambases.data.definition;

import net.minecraft.resources.ResourceLocation;

/**
 * Custom chunk generators which support a prebuilt structure should implement this
 */
@FunctionalInterface
public interface BaseDefinitionProvider {
    /**
     * Get the unique ID of the base definition; definitions are loaded from datapack JSON:
     * {@code data/<namespace>/ftb_base_definitions/<path>}
     *
     * @return a resource location for the base definition
     */
    ResourceLocation getBaseDefinitionId();
}

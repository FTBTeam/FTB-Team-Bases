package dev.ftb.mods.ftbteambases.data.definition;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.Optional;
import java.util.stream.Stream;

@FunctionalInterface
public interface StructureSetProvider {
    Optional<ResourceLocation> structureSetId();

    static Stream<Holder<StructureSet>> getStructureSets(HolderLookup<StructureSet> holderLookup, StructureSetProvider provider) {
        ResourceLocation setId = provider.structureSetId().orElse(BaseDefinition.DEFAULT_STRUCTURE_SET);
        return holderLookup.getOrThrow(TagKey.create(Registries.STRUCTURE_SET, setId)).stream();
    }
}

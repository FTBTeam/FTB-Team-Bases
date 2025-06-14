package dev.ftb.mods.ftbteambases.data.definition;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@FunctionalInterface
public interface StructureSetProvider {
    List<ResourceLocation> structureSetIds();

    static Stream<Holder<StructureSet>> getStructureSets(HolderLookup<StructureSet> holderLookup, StructureSetProvider provider) {
        List<Holder<StructureSet>> res = new ArrayList<>();
        provider.structureSetIds().stream()
                .map(id -> holderLookup.getOrThrow(TagKey.create(Registries.STRUCTURE_SET, id)))
                .forEach(l -> l.forEach(res::add));
        return res.stream();
    }
}

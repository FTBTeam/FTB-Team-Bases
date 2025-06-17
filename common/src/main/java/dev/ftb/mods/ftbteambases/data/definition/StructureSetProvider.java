package dev.ftb.mods.ftbteambases.data.definition;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@FunctionalInterface
public interface StructureSetProvider {
    List<ResourceLocation> structureSetIds();

    static Stream<Holder<StructureSet>> getStructureSets(HolderLookup.RegistryLookup<StructureSet> holderLookup, StructureSetProvider provider) {
        List<Holder<StructureSet>> res = new ArrayList<>();

        for (ResourceLocation id : provider.structureSetIds()) {
            if (id.getPath().startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(id.toString().substring(1));
                if (tagId == null) {
                    FTBTeamBases.LOGGER.warn("Invalid structure set tag format: {}", id);
                    continue;
                }

                TagKey<StructureSet> tag = TagKey.create(Registries.STRUCTURE_SET, tagId);
                try {
                    HolderSet.Named<StructureSet> set = holderLookup.get(tag).orElseThrow();
                    set.forEach(res::add);
                } catch (Exception e) {
                    FTBTeamBases.LOGGER.warn("Could not resolve structure set tag: {}", tagId, e);
                }
            } else {
                try {
                    Holder<StructureSet> holder = holderLookup.get(ResourceKey.create(Registries.STRUCTURE_SET, id)).orElseThrow();
                    res.add(holder);
                } catch (Exception e) {
                    FTBTeamBases.LOGGER.warn("Could not resolve structure set ID: {}", id, e);
                }
            }
        }

        return res.stream();
    }

}

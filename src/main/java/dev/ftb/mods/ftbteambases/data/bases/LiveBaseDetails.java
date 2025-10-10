package dev.ftb.mods.ftbteambases.data.bases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record LiveBaseDetails(RegionExtents extents, ResourceKey<Level> dimension, BlockPos spawnPos) {
    public static final Codec<LiveBaseDetails> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            RegionExtents.CODEC.fieldOf("extents").forGetter(LiveBaseDetails::extents),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(LiveBaseDetails::dimension),
            BlockPos.CODEC.fieldOf("spawnPos").forGetter(LiveBaseDetails::spawnPos)
    ).apply(inst, LiveBaseDetails::new));
}

package dev.ftb.mods.ftbteambases.data.bases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftbteambases.util.RegionExtents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record ArchivedBaseDetails(RegionExtents extents, ResourceKey<Level> dimension, BlockPos spawnPos, UUID ownerId, long archiveTime) {
    public static final Codec<ArchivedBaseDetails> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            RegionExtents.CODEC.fieldOf("extents").forGetter(ArchivedBaseDetails::extents),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ArchivedBaseDetails::dimension),
            BlockPos.CODEC.fieldOf("spawnPos").forGetter(ArchivedBaseDetails::spawnPos),
            UUIDUtil.STRING_CODEC.fieldOf("owner_id").forGetter(ArchivedBaseDetails::ownerId),
            Codec.LONG.fieldOf("archive_time").forGetter(ArchivedBaseDetails::archiveTime)
    ).apply(inst, ArchivedBaseDetails::new));
}

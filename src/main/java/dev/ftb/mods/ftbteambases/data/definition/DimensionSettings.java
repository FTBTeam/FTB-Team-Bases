package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record DimensionSettings(boolean privateDimension, Optional<ResourceLocation> dimensionId,
                                Optional<ResourceLocation> dimensionType) {
    public static final Codec<DimensionSettings> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("private").forGetter(DimensionSettings::privateDimension),
            ResourceLocation.CODEC.optionalFieldOf("dimension_id").forGetter(DimensionSettings::dimensionId),
            ResourceLocation.CODEC.optionalFieldOf("dimension_type").forGetter(DimensionSettings::dimensionType)
    ).apply(inst, DimensionSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DimensionSettings::privateDimension,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), DimensionSettings::dimensionId,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), DimensionSettings::dimensionType,
            DimensionSettings::new
    );
}

package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record DimensionSettings(boolean privateDimension, Optional<Identifier> dimensionId,
                                Optional<Identifier> dimensionType) {
    public static final Codec<DimensionSettings> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("private").forGetter(DimensionSettings::privateDimension),
            Identifier.CODEC.optionalFieldOf("dimension_id").forGetter(DimensionSettings::dimensionId),
            Identifier.CODEC.optionalFieldOf("dimension_type").forGetter(DimensionSettings::dimensionType)
    ).apply(inst, DimensionSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DimensionSettings::privateDimension,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), DimensionSettings::dimensionId,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), DimensionSettings::dimensionType,
            DimensionSettings::new
    );
}

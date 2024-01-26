package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record DimensionSettings(boolean privateDimension, Optional<ResourceLocation> dimensionId,
                                Optional<ResourceLocation> dimensionType) {
    public static final Codec<DimensionSettings> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("private").forGetter(DimensionSettings::privateDimension),
            ResourceLocation.CODEC.optionalFieldOf("dimension_id").forGetter(DimensionSettings::dimensionId),
            ResourceLocation.CODEC.optionalFieldOf("dimension_type").forGetter(DimensionSettings::dimensionType)
    ).apply(inst, DimensionSettings::new));

    public static DimensionSettings fromBytes(FriendlyByteBuf buf) {
        boolean privateDimension = buf.readBoolean();
        Optional<ResourceLocation> dimensionType = buf.readOptional(FriendlyByteBuf::readResourceLocation);
        Optional<ResourceLocation> dimensionId = buf.readOptional(FriendlyByteBuf::readResourceLocation);

        return new DimensionSettings(privateDimension, dimensionId, dimensionType);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(privateDimension);
        buf.writeOptional(dimensionType, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(dimensionId, FriendlyByteBuf::writeResourceLocation);
    }
}

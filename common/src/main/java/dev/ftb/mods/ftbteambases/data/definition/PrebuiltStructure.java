package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record PrebuiltStructure(ResourceLocation structureLocation, Optional<ResourceLocation> structureSetId,
                                int height) {
    public static final Codec<PrebuiltStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("structure_location")
                    .forGetter(PrebuiltStructure::structureLocation),
            ResourceLocation.CODEC.optionalFieldOf("structure_set")
                    .forGetter(PrebuiltStructure::structureSetId),
            Codec.INT.optionalFieldOf("height", 64)
                    .forGetter(PrebuiltStructure::height)
    ).apply(instance, PrebuiltStructure::new));

    public static PrebuiltStructure fromBytes(FriendlyByteBuf buf) {
        return new PrebuiltStructure(buf.readResourceLocation(), buf.readOptional(FriendlyByteBuf::readResourceLocation), buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(structureLocation);
        buf.writeOptional(structureSetId, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarInt(height);
    }
}

package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record PrebuiltStructure(ResourceLocation startStructure, Optional<ResourceLocation> structureSetId,
                                int height) implements INetworkWritable{
    public static final Codec<PrebuiltStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("start_structure")
                    .forGetter(PrebuiltStructure::startStructure),
            ResourceLocation.CODEC.optionalFieldOf("structure_set")
                    .forGetter(PrebuiltStructure::structureSetId),
            Codec.INT.optionalFieldOf("height", 64)
                    .forGetter(PrebuiltStructure::height)
    ).apply(instance, PrebuiltStructure::new));

    public static PrebuiltStructure fromBytes(FriendlyByteBuf buf) {
        return new PrebuiltStructure(buf.readResourceLocation(), buf.readOptional(FriendlyByteBuf::readResourceLocation), buf.readVarInt());
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(startStructure);
        buf.writeOptional(structureSetId, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarInt(height);
    }
}

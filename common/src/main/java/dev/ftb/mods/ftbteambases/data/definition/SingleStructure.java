package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SingleStructure(ResourceLocation structureLocation, Optional<Integer> yPos, boolean includeEntities) implements INetworkWritable {
    public static final Codec<SingleStructure> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("structure_location").forGetter(SingleStructure::structureLocation),
            Codec.INT.optionalFieldOf("y_pos").forGetter(SingleStructure::yPos),
            Codec.BOOL.optionalFieldOf("include_entities", false).forGetter(SingleStructure::includeEntities)
    ).apply(inst, SingleStructure::new));

    public static SingleStructure fromBytes(FriendlyByteBuf buf) {
        return new SingleStructure(buf.readResourceLocation(), buf.readOptional(FriendlyByteBuf::readInt), buf.readBoolean());
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(structureLocation);
        buf.writeOptional(yPos, FriendlyByteBuf::writeInt);
        buf.writeBoolean(includeEntities);
    }
}

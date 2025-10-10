package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SingleStructure(ResourceLocation structureLocation, Optional<Integer> yPos, boolean includeEntities) implements INetworkWritable<SingleStructure> {
    public static final Codec<SingleStructure> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("structure_location").forGetter(SingleStructure::structureLocation),
            Codec.INT.optionalFieldOf("y_pos").forGetter(SingleStructure::yPos),
            Codec.BOOL.optionalFieldOf("include_entities", false).forGetter(SingleStructure::includeEntities)
    ).apply(inst, SingleStructure::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SingleStructure> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SingleStructure::structureLocation,
            ByteBufCodecs.optional(ByteBufCodecs.INT), SingleStructure::yPos,
            ByteBufCodecs.BOOL, SingleStructure::includeEntities,
            SingleStructure::new
    );

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SingleStructure> streamCodec() {
        return STREAM_CODEC;
    }
}

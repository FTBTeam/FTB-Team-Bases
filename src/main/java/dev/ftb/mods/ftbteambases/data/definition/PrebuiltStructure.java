package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record PrebuiltStructure(Identifier startStructure, Optional<Identifier> structureSetId,
                                int height) implements INetworkWritable<PrebuiltStructure>, StructureSetProvider {
    public static final Codec<PrebuiltStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("start_structure")
                    .forGetter(PrebuiltStructure::startStructure),
            Identifier.CODEC.optionalFieldOf("structure_set")
                    .forGetter(PrebuiltStructure::structureSetId),
            Codec.INT.optionalFieldOf("height", 64)
                    .forGetter(PrebuiltStructure::height)
    ).apply(instance, PrebuiltStructure::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrebuiltStructure> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, PrebuiltStructure::startStructure,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), PrebuiltStructure::structureSetId,
            ByteBufCodecs.INT, PrebuiltStructure::height,
            PrebuiltStructure::new
    );

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, PrebuiltStructure> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public List<Identifier> structureSetIds() {
        return structureSetId.map(List::of).orElse(List.of());
    }
}

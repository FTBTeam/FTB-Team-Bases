package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record Pregen(String templateId, Optional<ResourceLocation> structureSetId) implements INetworkWritable<Pregen>, StructureSetProvider {
    public static final Codec<Pregen> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pregen_template")
                    .forGetter(Pregen::templateId),
            ResourceLocation.CODEC.optionalFieldOf("structure_set")
                    .forGetter(Pregen::structureSetId)
    ).apply(instance, Pregen::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Pregen> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Pregen::templateId,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Pregen::structureSetId,
            Pregen::new
    );

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Pregen> streamCodec() {
        return STREAM_CODEC;
    }
}

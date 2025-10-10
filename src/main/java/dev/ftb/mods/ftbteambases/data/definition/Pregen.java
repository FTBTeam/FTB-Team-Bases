package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record Pregen(String templateId, List<ResourceLocation> structureSetIds) implements INetworkWritable<Pregen>, StructureSetProvider {
    public static final Codec<Pregen> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pregen_template")
                    .forGetter(Pregen::templateId),
            ResourceLocation.CODEC.listOf().fieldOf("structure_sets")
                    .forGetter(Pregen::structureSetIds)
    ).apply(instance, Pregen::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Pregen> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Pregen::templateId,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), Pregen::structureSetIds,
            Pregen::new
    );

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Pregen> streamCodec() {
        return STREAM_CODEC;
    }
}

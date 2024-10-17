package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Pregen(String templateId) implements INetworkWritable<Pregen> {
    public static final Codec<Pregen> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pregen_template")
                    .forGetter(Pregen::templateId)
    ).apply(instance, Pregen::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Pregen> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Pregen::templateId,
            Pregen::new
    );

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Pregen> streamCodec() {
        return STREAM_CODEC;
    }
}

package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

public record Pregen(String templateId) implements INetworkWritable {
    public static final Codec<Pregen> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pregen_template")
                    .forGetter(Pregen::templateId)
    ).apply(instance, Pregen::new));

    public static Pregen fromBytes(FriendlyByteBuf buf) {
        return new Pregen(buf.readUtf());
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(templateId);
    }
}

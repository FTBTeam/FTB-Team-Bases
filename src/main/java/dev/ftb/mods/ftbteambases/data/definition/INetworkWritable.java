package dev.ftb.mods.ftbteambases.data.definition;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface INetworkWritable<T> {
    StreamCodec<RegistryFriendlyByteBuf,T> streamCodec();
}

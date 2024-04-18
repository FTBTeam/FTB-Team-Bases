package dev.ftb.mods.ftbteambases.data.definition;

import net.minecraft.network.FriendlyByteBuf;

public interface INetworkWritable {
    void toBytes(FriendlyByteBuf buf);
}

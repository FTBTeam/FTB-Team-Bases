package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.client.FTBTeamBasesClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public enum ShowSelectionGuiMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<ShowSelectionGuiMessage> TYPE = new Type<>(FTBTeamBases.rl("show_selection_gui"));
    public static final StreamCodec<FriendlyByteBuf, ShowSelectionGuiMessage> STREAM_CODEC = StreamCodec.unit(ShowSelectionGuiMessage.INSTANCE);

    public static void handle(ShowSelectionGuiMessage ignored, NetworkManager.PacketContext context) {
        context.queue(FTBTeamBasesClient::openSelectionScreen);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

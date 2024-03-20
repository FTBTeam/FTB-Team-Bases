package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteambases.client.FTBTeamBasesClient;
import net.minecraft.network.FriendlyByteBuf;

public class ShowSelectionGuiMessage extends BaseS2CMessage {
    public ShowSelectionGuiMessage() {}

    public ShowSelectionGuiMessage(FriendlyByteBuf buf) {}

    @Override
    public MessageType getType() {
        return FTBTeamBasesNet.SHOW_SELECTION_GUI;
    }

    @Override
    public void write(FriendlyByteBuf buf) {}

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(FTBTeamBasesClient::openSelectionScreen);
    }
}

package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteambases.client.DimensionsClient;
import dev.ftb.mods.ftbteambases.client.VoidTeamLevelData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class VoidTeamDimensionMessage extends BaseS2CMessage {
    private static final VoidTeamDimensionMessage INSTANCE = new VoidTeamDimensionMessage();

    private VoidTeamDimensionMessage() {
    }

    VoidTeamDimensionMessage(FriendlyByteBuf buf) {
    }

    @Override
    public MessageType getType() {
        return FTBTeamBasesNet.VOID_TEAM_DIMENSION;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (DimensionsClient.clientLevel().getLevelData() instanceof VoidTeamLevelData vld) {
            vld.ftb$setVoidTeamDimension();
        }
    }

    public static void syncTo(ServerPlayer player) {
        INSTANCE.sendTo(player);
    }
}

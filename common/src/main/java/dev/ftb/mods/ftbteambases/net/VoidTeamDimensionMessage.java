package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.client.FTBTeamBasesClient;
import dev.ftb.mods.ftbteambases.client.VoidTeamLevelData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public enum VoidTeamDimensionMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<VoidTeamDimensionMessage> TYPE = new Type<>(FTBTeamBases.rl("void_team_dimension"));
    public static final StreamCodec<FriendlyByteBuf, VoidTeamDimensionMessage> STREAM_CODEC = StreamCodec.unit(VoidTeamDimensionMessage.INSTANCE);

    public static void handle(VoidTeamDimensionMessage ignored, NetworkManager.PacketContext ignoredContext) {
        if (FTBTeamBasesClient.clientLevel().getLevelData() instanceof VoidTeamLevelData vld) {
            vld.ftb$setVoidTeamDimension();
        }
    }

    public static void syncTo(ServerPlayer player) {
        NetworkHelper.sendTo(player, INSTANCE);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.ftb.mods.ftbteambases.net;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VisitBaseMessage(String teamName, boolean archived) implements CustomPacketPayload {
    public static final Type<VisitBaseMessage> TYPE = new Type<>(FTBTeamBases.rl("visit_base"));
    public static final StreamCodec<FriendlyByteBuf, VisitBaseMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, VisitBaseMessage::teamName,
            ByteBufCodecs.BOOL, VisitBaseMessage::archived,
            VisitBaseMessage::new
    );

    public static void handle(VisitBaseMessage message, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sp && sp.hasPermissions(2) && sp.getServer() != null) {
            if (message.archived) {
                BaseInstanceManager.get(sp.getServer()).teleportToArchivedBase(sp, message.teamName);
            } else {
                FTBTeamsAPI.api().getManager().getTeamByName(message.teamName).ifPresent(team -> {
                    BaseInstanceManager.get(sp.getServer()).teleportToBaseSpawn(sp, team.getTeamId());
                });
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

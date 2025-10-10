package dev.ftb.mods.ftbteambases.net;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collection;

public record SyncBaseTemplatesMessage(Collection<BaseDefinition> templates) implements CustomPacketPayload {
    public static final Type<SyncBaseTemplatesMessage> TYPE = new Type<>(FTBTeamBases.rl("sync_base_templates"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBaseTemplatesMessage> STREAM_CODEC = StreamCodec.composite(
            BaseDefinition.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)), SyncBaseTemplatesMessage::templates,
            SyncBaseTemplatesMessage::new
    );

    public static void handle(SyncBaseTemplatesMessage message, IPayloadContext ignored) {
        BaseDefinitionManager.getClientInstance().syncFromServer(message.templates);
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncBaseTemplatesMessage(BaseDefinitionManager.getServerInstance().getDefinitions()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

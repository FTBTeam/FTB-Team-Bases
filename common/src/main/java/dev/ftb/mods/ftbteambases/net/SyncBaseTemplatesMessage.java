package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;

public class SyncBaseTemplatesMessage extends BaseS2CMessage {
    private final Collection<BaseDefinition> templates;

    public SyncBaseTemplatesMessage(BaseDefinitionManager manager) {
        this.templates = manager.getDefinitions();
    }

    public SyncBaseTemplatesMessage(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        templates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            templates.add(BaseDefinition.fromBytes(buf));
        }
    }

    @Override
    public MessageType getType() {
        return FTBTeamBasesNet.SYNC_BASE_TEMPLATES;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(templates.size());
        templates.forEach(s -> s.toBytes(buf));
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        BaseDefinitionManager.getClientInstance().syncFromServer(templates);
    }

    public static void syncTo(ServerPlayer player) {
        new SyncBaseTemplatesMessage(BaseDefinitionManager.getServerInstance()).sendTo(player);
    }
}

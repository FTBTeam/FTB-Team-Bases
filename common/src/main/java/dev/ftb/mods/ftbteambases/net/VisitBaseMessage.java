package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class VisitBaseMessage extends BaseC2SMessage {
    private final String teamName;
    private final boolean archived;

    public VisitBaseMessage(String teamName, boolean archived) {
        this.teamName = teamName;
        this.archived = archived;
    }

    public VisitBaseMessage(FriendlyByteBuf buf) {
        teamName = buf.readUtf();
        archived = buf.readBoolean();
    }

    @Override
    public MessageType getType() {
        return FTBTeamBasesNet.VISIT_LIVE_BASE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(teamName);
        buf.writeBoolean(archived);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp && sp.hasPermissions(2) && sp.getServer() != null) {
            if (archived) {
                BaseInstanceManager.get(sp.getServer()).teleportToArchivedBase(sp, teamName);
            } else {
                FTBTeamsAPI.api().getManager().getTeamByName(teamName).ifPresent(team -> {
                    BaseInstanceManager.get(sp.getServer()).teleportToBaseSpawn(sp, team.getTeamId());
                });
            }
        }
    }
}

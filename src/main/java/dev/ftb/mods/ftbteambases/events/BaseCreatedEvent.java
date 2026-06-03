package dev.ftb.mods.ftbteambases.events;

import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jspecify.annotations.Nullable;

public class BaseCreatedEvent extends Event {
    private final BaseInstanceManager manager;
    @Nullable
    private final ServerPlayer player;
    private final Team partyTeam;

    public BaseCreatedEvent(BaseInstanceManager manager, @Nullable ServerPlayer player, Team partyTeam) {
        this.manager = manager;
        this.player = player;
        this.partyTeam = partyTeam;
    }

    public BaseInstanceManager getManager() {
        return manager;
    }

    @Nullable
    public ServerPlayer getPlayer() {
        return player;
    }

    public Team getPartyTeam() {
        return partyTeam;
    }
}

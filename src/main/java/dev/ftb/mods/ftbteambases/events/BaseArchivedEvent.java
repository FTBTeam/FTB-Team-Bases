package dev.ftb.mods.ftbteambases.events;

import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.Team;
import net.neoforged.bus.api.Event;

public class BaseArchivedEvent extends Event {
    private final BaseInstanceManager manager;
    private final Team partyTeam;

    public BaseArchivedEvent(BaseInstanceManager manager, Team partyTeam) {
        this.manager = manager;
        this.partyTeam = partyTeam;
    }

    public BaseInstanceManager getManager() {
        return manager;
    }

    public Team getPartyTeam() {
        return partyTeam;
    }
}

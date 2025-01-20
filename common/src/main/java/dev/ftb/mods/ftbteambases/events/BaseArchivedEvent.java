package dev.ftb.mods.ftbteambases.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.Team;

public interface BaseArchivedEvent {
    Event<BaseArchivedEvent> ARCHIVED = EventFactory.createLoop();

    void deleted(BaseInstanceManager manager, Team partyTeam);
}

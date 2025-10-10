package dev.ftb.mods.ftbteambases.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;

public interface BaseCreatedEvent {
    Event<BaseCreatedEvent> CREATED = EventFactory.createLoop();

    void created(BaseInstanceManager manager, ServerPlayer player, Team partyTeam);
}

package dev.ftb.mods.ftbteambases.events.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class TeamBasesPortalEvent extends PlayerEvent implements ICancellableEvent {
    private Component reason = Component.empty();

    public TeamBasesPortalEvent(Player player) {
        super(player);
    }

    public void cancelWithReason(Component reason) {
        this.reason = reason;
        setCanceled(true);
    }

    public Component getCancellationReason() {
        return reason;
    }
}

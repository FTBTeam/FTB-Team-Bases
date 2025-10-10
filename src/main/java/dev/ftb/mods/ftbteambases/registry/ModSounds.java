package dev.ftb.mods.ftbteambases.registry;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, FTBTeamBases.MOD_ID);

    public static final Supplier<SoundEvent> PORTAL
            = SOUNDS.register("portal", () -> SoundEvent.createVariableRangeEvent(FTBTeamBases.rl("portal")));

    public static void init(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}

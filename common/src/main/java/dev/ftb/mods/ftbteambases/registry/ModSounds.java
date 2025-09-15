package dev.ftb.mods.ftbteambases.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(FTBTeamBases.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> PORTAL
            = SOUNDS.register("portal", () -> SoundEvent.createVariableRangeEvent(FTBTeamBases.rl("portal")));

    public static void init() {
        SOUNDS.register();
    }
}

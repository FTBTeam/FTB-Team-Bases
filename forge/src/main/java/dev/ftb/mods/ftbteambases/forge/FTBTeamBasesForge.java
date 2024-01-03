package dev.ftb.mods.ftbteambases.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FTBTeamBases.MOD_ID)
public class FTBTeamBasesForge {
    public FTBTeamBasesForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(FTBTeamBases.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        FTBTeamBases.init();
    }
}

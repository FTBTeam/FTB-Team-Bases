package dev.ftb.mods.ftbteambases.fabric;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.fabricmc.api.ModInitializer;

public class FTBTeamBasesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FTBTeamBases.init();
    }
}

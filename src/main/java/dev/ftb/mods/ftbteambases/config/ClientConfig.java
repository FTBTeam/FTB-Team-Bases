package dev.ftb.mods.ftbteambases.config;

import dev.ftb.mods.ftblibrary.config.value.BooleanValue;
import dev.ftb.mods.ftblibrary.config.value.Config;
import dev.ftb.mods.ftblibrary.config.value.DoubleValue;
import dev.ftb.mods.ftbteambases.FTBTeamBases;

public interface ClientConfig {
    Config CONFIG = Config.create(FTBTeamBases.MOD_ID + "-client");

    Config GENERAL = CONFIG.addGroup("general");

    BooleanValue HIDE_VOID_FOG = GENERAL.addBoolean("hide_void_fog", true)
            .comment("If true, suppress the void fog effect that appears at low Y levels while in void team dimensions");
    DoubleValue VOID_HORIZON = GENERAL.addDouble("void_horizon", 0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
            .comment("In void team dimensions, the Y level of the horizon; the lower sky turns black if the player's eye position is below this level");

    Config ADVANCED = CONFIG.addGroup("advanced");
    BooleanValue SHOW_DEV_BASES = ADVANCED.addBoolean("show_dev_mode", false)
            .comment("(Advanced) If true, base definition types marked as 'dev_mode: true' will be shown in the base selection GUI. These bases are intended for development mode debugging, and are not generally useful for players.");
}

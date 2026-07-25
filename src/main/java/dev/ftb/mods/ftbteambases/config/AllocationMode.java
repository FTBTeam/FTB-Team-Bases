package dev.ftb.mods.ftbteambases.config;

import dev.ftb.mods.ftblibrary.config.NameMap;

public enum AllocationMode {
    SPIRAL,
    ROWS;

    public static final NameMap<AllocationMode> NAME_MAP = NameMap.of(SPIRAL, values()).create();
}

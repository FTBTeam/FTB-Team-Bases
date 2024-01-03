package dev.ftb.mods.ftbteambases;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.ftb.mods.ftbteambases.command.ModCommands;
import dev.ftb.mods.ftbteambases.util.RegionFileRelocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

public class FTBTeamBases {
    public static final String MOD_ID = "ftbteambases";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init() {
        try {
            Files.createDirectories(RegionFileRelocator.PREGEN_PATH);
        } catch (IOException e) {
            LOGGER.error("can't create {}: {}", RegionFileRelocator.PREGEN_PATH, e.getMessage());
        }

        CommandRegistrationEvent.EVENT.register(ModCommands::registerCommands);
    }
}

package dev.ftb.mods.ftbteambases.fabric;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.command.arguments.BaseDefinitionArgument;
import dev.ftb.mods.ftbteambases.util.MiscUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.server.level.ServerLevel;

public class FTBTeamBasesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ArgumentTypeRegistry.registerArgumentType(FTBTeamBases.rl("base_definition"),
                BaseDefinitionArgument.class, SingletonArgumentInfo.contextFree(BaseDefinitionArgument::create));

        FTBTeamBases.init();
    }
}

package dev.ftb.mods.ftbteambases.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.command.arguments.BaseDefinitionArgument;
import dev.ftb.mods.ftbteambases.util.MiscUtil;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(FTBTeamBases.MOD_ID)
public class FTBTeamBasesForge {
    public static final DeferredRegister<ArgumentTypeInfo<?,?>> ARGUMENT_TYPES
            = DeferredRegister.create(ForgeRegistries.Keys.COMMAND_ARGUMENT_TYPES, FTBTeamBases.MOD_ID);

    private static final RegistryObject<ArgumentTypeInfo<?,?>> PREBUILT_COMMAND
            = ARGUMENT_TYPES.register("prebuilt", () -> ArgumentTypeInfos.registerByClass(
                    BaseDefinitionArgument.class, SingletonArgumentInfo.contextFree(BaseDefinitionArgument::create)));

    public FTBTeamBasesForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(FTBTeamBases.MOD_ID, modEventBus);

        ARGUMENT_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onSleepFinished);

        FTBTeamBases.init();
    }

    private void onSleepFinished(final SleepFinishedTimeEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().location().getNamespace().equals(FTBTeamBases.MOD_ID)) {
            // player has slept in a dynamic dimension
            // sleeping in dynamic dimensions doesn't work in general: https://bugs.mojang.com/browse/MC-188578
            // best we can do here is advance the overworld time
            MiscUtil.setOverworldTime(level.getServer(), event.getNewTime());
        }
    }
}

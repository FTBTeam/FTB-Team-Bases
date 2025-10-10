package dev.ftb.mods.ftbteambases.registry;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.command.arguments.BaseDefinitionArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?,?>> ARGUMENT_TYPES
            = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, FTBTeamBases.MOD_ID);

    private static final DeferredHolder<ArgumentTypeInfo<?,?>, SingletonArgumentInfo<BaseDefinitionArgument>> PREBUILT_COMMAND
            = ARGUMENT_TYPES.register("prebuilt", () -> ArgumentTypeInfos.registerByClass(
            BaseDefinitionArgument.class, SingletonArgumentInfo.contextFree(BaseDefinitionArgument::create)));

    public static void init(IEventBus eventBus) {
        ARGUMENT_TYPES.register(eventBus);
    }
}

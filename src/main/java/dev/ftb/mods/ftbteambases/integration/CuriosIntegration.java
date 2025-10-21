package dev.ftb.mods.ftbteambases.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosIntegration {
    public static void clearCurios(ServerPlayer serverPlayer) {
        CuriosApi.getCuriosInventory(serverPlayer).ifPresent(handler -> {
            handler.getCurios().forEach((id, stackHandler) -> {
                IDynamicStackHandler stacks = stackHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    stacks.setStackInSlot(i, ItemStack.EMPTY);
                }
            });
        });
    }
}

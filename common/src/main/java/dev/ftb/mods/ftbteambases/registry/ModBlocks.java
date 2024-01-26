package dev.ftb.mods.ftbteambases.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.block.BasesPortalBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(FTBTeamBases.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(FTBTeamBases.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Block> PORTAL = BLOCKS.register("portal", BasesPortalBlock::new);

    // Note: not in creative tab: you can only get this with "/give @s ftbteambases:portal"
    // Intended for builders to create lobby structures
    public static final RegistrySupplier<Item> PORTAL_ITEM = ITEMS.register("portal", () -> new BlockItem(PORTAL.get(), new Item.Properties()));

    public static void init() {
        BLOCKS.register();
        ITEMS.register();
    }
}

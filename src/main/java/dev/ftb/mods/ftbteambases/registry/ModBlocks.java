package dev.ftb.mods.ftbteambases.registry;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.block.BasesPortalBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FTBTeamBases.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FTBTeamBases.MOD_ID);

    public static final DeferredBlock<BasesPortalBlock> PORTAL
            = BLOCKS.register("portal", () -> new BasesPortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL)));

    // Note: not in creative tab: you can only get this with "/give @s ftbteambases:portal"
    //  or by middle-clicking a portal in creative mode
    // Intended for builders to create pregen lobby structures
    public static final DeferredItem<Item> PORTAL_ITEM = ITEMS.register("portal", () -> new BlockItem(PORTAL.get(), new Item.Properties()));

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}

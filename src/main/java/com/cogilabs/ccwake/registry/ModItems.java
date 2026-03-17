package com.cogilabs.ccwake.registry;

import com.cogilabs.ccwake.CcWakeMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CcWakeMod.MOD_ID);

    public static final RegistryObject<Item> WAKE_CONTROLLER = ITEMS.register("wake_controller",
            () -> new BlockItem(ModBlocks.WAKE_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> WAKE_NODE = ITEMS.register("wake_node",
            () -> new BlockItem(ModBlocks.WAKE_NODE.get(), new Item.Properties()));

    public static final RegistryObject<Item> WAKE_CHIP = ITEMS.register("wake_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INCOMPLETE_WAKE_CHIP = ITEMS.register("incomplete_wake_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INCOMPLETE_WAKE_NODE = ITEMS.register("incomplete_wake_node",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INCOMPLETE_WAKE_CONTROLLER = ITEMS.register("incomplete_wake_controller",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        eventBus.addListener(ModItems::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(WAKE_NODE);
            event.accept(WAKE_CONTROLLER);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(WAKE_CHIP);
        }
    }
}

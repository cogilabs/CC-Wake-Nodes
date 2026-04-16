package com.cogilabs.ccwake.registry;

import com.cogilabs.ccwake.CcWakeMod;
import com.cogilabs.ccwake.block.WakeNodeBlock;
import com.cogilabs.ccwake.block.WakeNodeAdvancedBlock;
import com.cogilabs.ccwake.block.WakeControllerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CcWakeMod.MOD_ID);

    public static final RegistryObject<Block> WAKE_NODE = BLOCKS.register("wake_node",
            () -> new WakeNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.6f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> WAKE_NODE_ADVANCED = BLOCKS.register("wake_node_advanced",
            () -> new WakeNodeAdvancedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(1.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> WAKE_CONTROLLER = BLOCKS.register("wake_controller",
            () -> new WakeControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5f)
                    .sound(SoundType.METAL)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

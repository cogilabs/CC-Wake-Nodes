package com.cogilabs.ccwake.registry;

import com.cogilabs.ccwake.CcWakeMod;
import com.cogilabs.ccwake.block.WakeNodeBlockEntity;
import com.cogilabs.ccwake.block.WakeNodeAdvancedBlockEntity;
import com.cogilabs.ccwake.block.WakeControllerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CcWakeMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<WakeNodeBlockEntity>> WAKE_NODE =
            BLOCK_ENTITIES.register("wake_node",
                    () -> BlockEntityType.Builder.of(WakeNodeBlockEntity::new,
                            ModBlocks.WAKE_NODE.get()).build(null));

    public static final RegistryObject<BlockEntityType<WakeNodeAdvancedBlockEntity>> WAKE_NODE_ADVANCED =
            BLOCK_ENTITIES.register("wake_node_advanced",
                    () -> BlockEntityType.Builder.of(WakeNodeAdvancedBlockEntity::new,
                            ModBlocks.WAKE_NODE_ADVANCED.get()).build(null));

    public static final RegistryObject<BlockEntityType<WakeControllerBlockEntity>> WAKE_CONTROLLER =
            BLOCK_ENTITIES.register("wake_controller",
                    () -> BlockEntityType.Builder.of(WakeControllerBlockEntity::new,
                            ModBlocks.WAKE_CONTROLLER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

package com.cogilabs.ccwake.block;

import com.cogilabs.ccwake.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WakeControllerBlockEntity extends BlockEntity {

    public WakeControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAKE_CONTROLLER.get(), pos, state);
    }
}

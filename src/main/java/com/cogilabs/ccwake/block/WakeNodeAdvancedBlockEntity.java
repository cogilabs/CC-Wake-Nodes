package com.cogilabs.ccwake.block;

import com.cogilabs.ccwake.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class WakeNodeAdvancedBlockEntity extends WakeNodeBlockEntity {

    private int range = 3;

    public WakeNodeAdvancedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAKE_NODE_ADVANCED.get(), pos, state);
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Range", range);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Range")) {
            range = tag.getInt("Range");
        }
    }
}

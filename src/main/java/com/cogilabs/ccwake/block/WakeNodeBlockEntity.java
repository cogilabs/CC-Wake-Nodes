package com.cogilabs.ccwake.block;

import com.cogilabs.ccwake.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WakeNodeBlockEntity extends BlockEntity {

    private @Nullable String nodeId;

    public WakeNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAKE_NODE.get(), pos, state);
    }

    public @Nullable String getNodeId() {
        return nodeId;
    }

    public void setNodeId(@Nullable String nodeId) {
        this.nodeId = nodeId;
        setChanged();
    }

    public BlockPos getComputerPos() {
        BlockState state = getBlockState();
        Direction facing = state.getValue(WakeNodeBlock.FACING);
        return getBlockPos().relative(facing.getOpposite());
    }

    public Direction getAttachFace() {
        return getBlockState().getValue(WakeNodeBlock.FACING);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (nodeId != null) {
            tag.putString("NodeId", nodeId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("NodeId")) {
            nodeId = tag.getString("NodeId");
        }
    }
}

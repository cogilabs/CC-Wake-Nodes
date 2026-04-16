package com.cogilabs.ccwake.block;

import com.cogilabs.ccwake.chunk.ChunkLoadingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WakeNodeAdvancedBlock extends WakeNodeBlock {

    public WakeNodeAdvancedBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WakeNodeAdvancedBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WakeNodeAdvancedBlockEntity wakeNode) {
                String nodeId = wakeNode.getNodeId();
                if (nodeId != null) {
                    ChunkLoadingManager.get((ServerLevel) level)
                            .unloadNodeSilent((ServerLevel) level, nodeId);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}

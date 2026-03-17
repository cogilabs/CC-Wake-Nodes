package com.cogilabs.ccwake.block;

import com.cogilabs.ccwake.chunk.WakeNodeRegistry;
import com.cogilabs.ccwake.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class WakeNodeBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    private static final VoxelShape SHAPE_UP    = box(2, 0, 2, 14, 2, 14);
    private static final VoxelShape SHAPE_DOWN  = box(2, 14, 2, 14, 16, 14);
    private static final VoxelShape SHAPE_SOUTH = box(2, 2, 0, 14, 14, 2);
    private static final VoxelShape SHAPE_NORTH = box(2, 2, 14, 14, 14, 16);
    private static final VoxelShape SHAPE_EAST  = box(0, 2, 2, 2, 14, 14);
    private static final VoxelShape SHAPE_WEST  = box(14, 2, 2, 16, 14, 14);

    public WakeNodeBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case DOWN  -> SHAPE_DOWN;
            case UP    -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction supportFace = ctx.getClickedFace();
        Direction facing = supportFace;
        BlockPos computerPos = ctx.getClickedPos().relative(facing.getOpposite());

        Level level = ctx.getLevel();
        if (!isComputerBlock(level, computerPos)) {
            return null; // refuse placement
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return isComputerBlock(level, supportPos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction supportDir = state.getValue(FACING).getOpposite();
        if (direction == supportDir && !isComputerBlock(level, neighborPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof WakeNodeBlockEntity wakeNode) {
                    String nodeId = wakeNode.getNodeId();
                    if (nodeId != null) {
                        WakeNodeRegistry.get(level).unregisterNode(nodeId);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WakeNodeBlockEntity(pos, state);
    }

    public static BlockPos getComputerPos(BlockState state, BlockPos nodePos) {
        Direction facing = state.getValue(FACING);
        return nodePos.relative(facing.getOpposite());
    }

    private static boolean isComputerBlock(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        String blockId = state.getBlock().builtInRegistryHolder().key().location().toString();
        return blockId.startsWith("computercraft:computer_");
    }
}

package com.cogilabs.ccwake.peripheral;

import com.cogilabs.ccwake.block.WakeNodeAdvancedBlockEntity;
import com.cogilabs.ccwake.chunk.ChunkLoadingManager;
import com.cogilabs.ccwake.chunk.WakeNodeRegistry;
import com.cogilabs.ccwake.config.CcWakeConfig;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class WakeNodeAdvancedPeripheral extends WakeNodePeripheral {

    public WakeNodeAdvancedPeripheral(WakeNodeAdvancedBlockEntity blockEntity) {
        super(blockEntity);
    }

    private WakeNodeAdvancedBlockEntity getAdvancedBlockEntity() {
        return (WakeNodeAdvancedBlockEntity) this.blockEntity;
    }

    @Override
    public String getType() {
        return "wake_node_advanced";
    }

    @Override
    protected int getNodeRange() {
        return getAdvancedBlockEntity().getRange();
    }

    @LuaFunction
    public final boolean setRange(IComputerAccess computer, int size) throws LuaException {
        if (size != 1 && size != 3 && size != 5) {
            throw new LuaException("Range must be 1, 3 or 5");
        }

        if (!CcWakeConfig.ADVANCED_WAKE_NODE_ENABLED.get()) {
            throw new LuaException("Advanced Wake Nodes are disabled");
        }

        int maxRange = CcWakeConfig.ADVANCED_WAKE_NODE_MAX_RANGE.get();
        if (size > maxRange) {
            throw new LuaException("Range " + size + " exceeds max allowed (" + maxRange + ")");
        }

        int callerComputerId = getCallerComputerId(computer);
        WakeNodeRegistry registry = getRegistry();
        String nodeId = requireNodeId();
        requireOwnershipOrClaimLegacy(registry, nodeId, callerComputerId);

        WakeNodeAdvancedBlockEntity advBE = getAdvancedBlockEntity();
        int oldRange = advBE.getRange();

        // Update block entity
        advBE.setRange(size);

        // Update registry
        registry.setRange(nodeId, size);

        // Handle chunk changes if node is currently loaded
        if (oldRange != size) {
            Level level = blockEntity.getLevel();
            if (level instanceof ServerLevel serverLevel) {
                boolean allowChange = CcWakeConfig.ADVANCED_WAKE_NODE_ALLOW_RANGE_CHANGE_WHILE_LOADED.get();
                ChunkLoadingManager clm = ChunkLoadingManager.get(serverLevel);

                if (clm.isLoaded(nodeId)) {
                    if (!allowChange) {
                        // Revert changes
                        advBE.setRange(oldRange);
                        registry.setRange(nodeId, oldRange);
                        throw new LuaException("Cannot change range while node is loaded (disabled by config)");
                    }

                    int oldRadius = rangeToRadius(oldRange);
                    int newRadius = rangeToRadius(size);
                    clm.requestChangeRadius(nodeId, newRadius);
                }
            }
        }

        return true;
    }

    @LuaFunction
    public final int getRange() {
        return getAdvancedBlockEntity().getRange();
    }

    @LuaFunction
    public final List<Integer> listAvailableRanges() {
        int maxRange = CcWakeConfig.ADVANCED_WAKE_NODE_MAX_RANGE.get();
        if (maxRange >= 5) return List.of(1, 3, 5);
        if (maxRange >= 3) return List.of(1, 3);
        return List.of(1);
    }

    @Override
    @LuaFunction
    public @Nullable Map<String, Object> getInfo() {
        Map<String, Object> result = super.getInfo();
        if (result == null) return null;

        WakeNodeAdvancedBlockEntity advBE = getAdvancedBlockEntity();
        int range = advBE.getRange();
        result.put("range", range);
        result.put("loaded_chunks", range * range);
        return result;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof WakeNodeAdvancedPeripheral wnp
                && wnp.blockEntity.getBlockPos().equals(this.blockEntity.getBlockPos());
    }

    public static int rangeToRadius(int range) {
        if (range <= 0) return CcWakeConfig.DEFAULT_LOAD_RADIUS.get();
        return (range - 1) / 2;
    }
}

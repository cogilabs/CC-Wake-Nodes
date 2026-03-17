package com.cogilabs.ccwake.peripheral;

import com.cogilabs.ccwake.block.WakeNodeBlock;
import com.cogilabs.ccwake.block.WakeNodeBlockEntity;
import com.cogilabs.ccwake.chunk.WakeNodeRegistry;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WakeNodePeripheral implements IPeripheral {

    private final WakeNodeBlockEntity blockEntity;

    public WakeNodePeripheral(WakeNodeBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "wake_node";
    }

    @LuaFunction
    public final void setId(IComputerAccess computer, String id) throws LuaException {
        if (id == null || id.isBlank()) {
            throw new LuaException("id must be a non-empty string");
        }

        int callerComputerId = getCallerComputerId(computer);

        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos nodePos = blockEntity.getBlockPos();
        BlockState state = blockEntity.getBlockState();
        BlockPos computerPos = WakeNodeBlock.getComputerPos(state, nodePos);
        String dimension = serverLevel.dimension().location().toString();
        int chunkX = computerPos.getX() >> 4;
        int chunkZ = computerPos.getZ() >> 4;
        String attachFace = state.getValue(WakeNodeBlock.FACING).getName();

        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        WakeNodeRegistry.NodeData existing = registry.getNode(id);

        if (existing != null) {
            if (existing.nodePos().equals(nodePos) && existing.dimension().equals(dimension)) {
                if (existing.ownerComputerId() <= 0) {
                    registry.setOwner(id, callerComputerId);
                } else if (existing.ownerComputerId() != callerComputerId) {
                    throw new LuaException("Node id '" + id + "' is owned by another computer");
                }
                blockEntity.setNodeId(id);
                return;
            }
            throw new LuaException("Node id '" + id + "' is already registered to another computer");
        }

        registry.registerNode(id, dimension, nodePos, computerPos, chunkX, chunkZ, attachFace, callerComputerId);
        blockEntity.setNodeId(id);
    }

    @LuaFunction
    public final @Nullable String getId() {
        return blockEntity.getNodeId();
    }

    @LuaFunction
    public final Map<String, Object> getChunk() {
        Level level = blockEntity.getLevel();
        if (level == null) return Map.of();

        BlockPos computerPos = blockEntity.getComputerPos();
        ResourceKey<Level> dim = level.dimension();

        Map<String, Object> result = new HashMap<>();
        result.put("dimension", dim.location().toString());
        result.put("chunk_x", computerPos.getX() >> 4);
        result.put("chunk_z", computerPos.getZ() >> 4);
        return result;
    }

    @LuaFunction
    public final @Nullable Map<String, Object> getInfo() {
        String id = blockEntity.getNodeId();
        Level level = blockEntity.getLevel();
        if (level == null) return null;

        BlockPos computerPos = blockEntity.getComputerPos();
        ResourceKey<Level> dim = level.dimension();

        Map<String, Object> result = new HashMap<>();
        result.put("id", id != null ? id : "");
        result.put("dimension", dim.location().toString());
        result.put("chunk_x", computerPos.getX() >> 4);
        result.put("chunk_z", computerPos.getZ() >> 4);
        return result;
    }

    @LuaFunction
    public final void grantController(IComputerAccess computer, int controllerId) throws LuaException {
        if (controllerId <= 0) {
            throw new LuaException("controllerId must be a positive integer");
        }

        int callerComputerId = getCallerComputerId(computer);
        WakeNodeRegistry registry = getRegistry();
        String nodeId = requireNodeId();
        WakeNodeRegistry.NodeData node = requireOwnershipOrClaimLegacy(registry, nodeId, callerComputerId);

        if (controllerId == node.ownerComputerId()) {
            return;
        }
        registry.grantController(nodeId, controllerId);
    }

    @LuaFunction
    public final void revokeController(IComputerAccess computer, int controllerId) throws LuaException {
        if (controllerId <= 0) {
            throw new LuaException("controllerId must be a positive integer");
        }

        int callerComputerId = getCallerComputerId(computer);
        WakeNodeRegistry registry = getRegistry();
        String nodeId = requireNodeId();
        WakeNodeRegistry.NodeData node = requireOwnershipOrClaimLegacy(registry, nodeId, callerComputerId);

        if (controllerId == node.ownerComputerId()) {
            throw new LuaException("Cannot revoke owner");
        }
        registry.revokeController(nodeId, controllerId);
    }

    @LuaFunction
    public final List<Integer> listControllers(IComputerAccess computer) throws LuaException {
        int callerComputerId = getCallerComputerId(computer);
        WakeNodeRegistry registry = getRegistry();
        String nodeId = requireNodeId();
        WakeNodeRegistry.NodeData node = requireOwnershipOrClaimLegacy(registry, nodeId, callerComputerId);

        List<Integer> ids = new ArrayList<>(node.controllerComputerIds());
        ids.sort(Integer::compareTo);
        return ids;
    }

    @LuaFunction
    public final Map<String, Object> getPermissions(IComputerAccess computer) throws LuaException {
        int callerComputerId = getCallerComputerId(computer);
        WakeNodeRegistry registry = getRegistry();
        String nodeId = requireNodeId();
        WakeNodeRegistry.NodeData node = requireOwnershipOrClaimLegacy(registry, nodeId, callerComputerId);

        List<Integer> controllers = new ArrayList<>(node.controllerComputerIds());
        controllers.sort(Integer::compareTo);

        Map<String, Object> result = new HashMap<>();
        result.put("owner", node.ownerComputerId());
        result.put("controllers", controllers);
        return result;
    }


    private int getCallerComputerId(IComputerAccess computer) throws LuaException {
        int id = computer.getID();
        if (id <= 0) {
            throw new LuaException("Unable to resolve caller computer id");
        }
        return id;
    }

    private WakeNodeRegistry getRegistry() throws LuaException {
        Level level = blockEntity.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide) {
            throw new LuaException("Node not available");
        }
        return WakeNodeRegistry.get(serverLevel);
    }

    private String requireNodeId() throws LuaException {
        String nodeId = blockEntity.getNodeId();
        if (nodeId == null || nodeId.isBlank()) {
            throw new LuaException("Node is not registered. Call setId first.");
        }
        return nodeId;
    }

    private WakeNodeRegistry.NodeData requireOwnershipOrClaimLegacy(WakeNodeRegistry registry, String nodeId, int callerComputerId) throws LuaException {
        WakeNodeRegistry.NodeData node = registry.getNode(nodeId);
        if (node == null) {
            throw new LuaException("Node not registered");
        }

        if (node.ownerComputerId() <= 0) {
            registry.setOwner(nodeId, callerComputerId);
            WakeNodeRegistry.NodeData claimed = registry.getNode(nodeId);
            if (claimed == null) {
                throw new LuaException("Node not registered");
            }
            return claimed;
        }

        if (node.ownerComputerId() != callerComputerId) {
            throw new LuaException("Not authorized");
        }

        return node;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof WakeNodePeripheral wnp
                && wnp.blockEntity.getBlockPos().equals(this.blockEntity.getBlockPos());
    }
}

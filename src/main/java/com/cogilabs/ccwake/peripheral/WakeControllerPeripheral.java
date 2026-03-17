package com.cogilabs.ccwake.peripheral;

import com.cogilabs.ccwake.chunk.ChunkLoadingManager;
import com.cogilabs.ccwake.chunk.WakeNodeRegistry;
import com.cogilabs.ccwake.config.CcWakeConfig;
import com.cogilabs.ccwake.block.WakeControllerBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WakeControllerPeripheral implements IPeripheral {

    private final WakeControllerBlockEntity blockEntity;

    public WakeControllerPeripheral(WakeControllerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "wake_controller";
    }

    private ServerLevel getServerLevel() throws LuaException {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            throw new LuaException("Controller not available");
        }
        return (ServerLevel) level;
    }

    @LuaFunction
    public final List<String> listNodes(IComputerAccess computer) throws LuaException {
        ServerLevel serverLevel = getServerLevel();
        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        int callerId = getCallerComputerId(computer);

        List<String> ids = new ArrayList<>();
        for (String id : registry.getAllNodeIds()) {
            WakeNodeRegistry.NodeData data = registry.getNode(id);
            if (data != null && canControlNode(data, callerId)) {
                ids.add(id);
            }
        }
        return ids;
    }

    @LuaFunction
    public final @Nullable Map<String, Object> getNodeInfo(IComputerAccess computer, String id) throws LuaException {
        if (id == null || id.isBlank()) throw new LuaException("id required");

        ServerLevel serverLevel = getServerLevel();
        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        WakeNodeRegistry.NodeData data = requireAuthorizedNode(registry, id, computer);

        ChunkLoadingManager clm = ChunkLoadingManager.get(serverLevel);

        Map<String, Object> result = new HashMap<>();
        result.put("id", data.id());
        result.put("dimension", data.dimension());
        result.put("chunk_x", data.chunkX());
        result.put("chunk_z", data.chunkZ());
        result.put("loaded", clm.isLoaded(id));

        Long expiresAt = clm.getExpiresAt(id);
        if (expiresAt != null) {
            long currentTime = serverLevel.getGameTime();
            long remainingTicks = expiresAt - currentTime;
            result.put("expires_at", Math.max(0, remainingTicks / 20));
        }

        return result;
    }

    @LuaFunction
    public final void loadNode(IComputerAccess computer, String id) throws LuaException {
        if (id == null || id.isBlank()) throw new LuaException("id required");

        ServerLevel serverLevel = getServerLevel();
        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        WakeNodeRegistry.NodeData data = requireAuthorizedNode(registry, id, computer);

        ChunkLoadingManager clm = ChunkLoadingManager.get(serverLevel);
        clm.requestLoadNode(data, -1);
    }

    @LuaFunction
    public final void loadFor(IComputerAccess computer, String id, int seconds) throws LuaException {
        if (id == null || id.isBlank()) throw new LuaException("id required");
        if (seconds <= 0) throw new LuaException("seconds must be positive");

        int maxDuration = CcWakeConfig.MAX_LOAD_DURATION_SECONDS.get();
        if (seconds > maxDuration) {
            throw new LuaException("Duration exceeds max allowed (" + maxDuration + "s)");
        }

        ServerLevel serverLevel = getServerLevel();
        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        WakeNodeRegistry.NodeData data = requireAuthorizedNode(registry, id, computer);

        ChunkLoadingManager clm = ChunkLoadingManager.get(serverLevel);
        clm.requestLoadNode(data, seconds);
    }

    @LuaFunction
    public final void unloadNode(IComputerAccess computer, String id) throws LuaException {
        if (id == null || id.isBlank()) throw new LuaException("id required");

        ServerLevel serverLevel = getServerLevel();
        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        requireAuthorizedNode(registry, id, computer);
        ChunkLoadingManager clm = ChunkLoadingManager.get(serverLevel);
        clm.requestUnloadNode(id);
    }

    @LuaFunction
    public final void unloadAll(IComputerAccess computer) throws LuaException {
        ServerLevel serverLevel = getServerLevel();
        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        int callerId = getCallerComputerId(computer);

        ChunkLoadingManager clm = ChunkLoadingManager.get(serverLevel);
        for (String id : clm.getLoadedNodeIds()) {
            WakeNodeRegistry.NodeData data = registry.getNode(id);
            if (data != null && canControlNode(data, callerId)) {
                clm.requestUnloadNode(id);
            }
        }
    }

    @LuaFunction
    public final boolean isNodeLoaded(IComputerAccess computer, String id) throws LuaException {
        if (id == null || id.isBlank()) throw new LuaException("id required");

        ServerLevel serverLevel = getServerLevel();
        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        requireAuthorizedNode(registry, id, computer);
        ChunkLoadingManager clm = ChunkLoadingManager.get(serverLevel);
        return clm.isLoaded(id);
    }

    @LuaFunction
    public final List<String> getLoadedNodes(IComputerAccess computer) throws LuaException {
        ServerLevel serverLevel = getServerLevel();
        WakeNodeRegistry registry = WakeNodeRegistry.get(serverLevel);
        int callerId = getCallerComputerId(computer);

        ChunkLoadingManager clm = ChunkLoadingManager.get(serverLevel);
        List<String> ids = new ArrayList<>();
        for (String id : clm.getLoadedNodeIds()) {
            WakeNodeRegistry.NodeData data = registry.getNode(id);
            if (data != null && canControlNode(data, callerId)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private WakeNodeRegistry.NodeData requireAuthorizedNode(WakeNodeRegistry registry, String id, IComputerAccess computer) throws LuaException {
        WakeNodeRegistry.NodeData data = registry.getNode(id);
        if (data == null) {
            throw new LuaException("Unknown node: " + id);
        }

        int callerId = getCallerComputerId(computer);
        if (!canControlNode(data, callerId)) {
            throw new LuaException("Not authorized for node: " + id);
        }
        return data;
    }

    private int getCallerComputerId(IComputerAccess computer) throws LuaException {
        int id = computer.getID();
        if (id <= 0) {
            throw new LuaException("Unable to resolve caller computer id");
        }
        return id;
    }

    private boolean canControlNode(WakeNodeRegistry.NodeData data, int callerComputerId) {
        int owner = data.ownerComputerId();
        if (owner <= 0) {
            return true;
        }
        return owner == callerComputerId || data.controllerComputerIds().contains(callerComputerId);
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof WakeControllerPeripheral wcp
                && wcp.blockEntity.getBlockPos().equals(this.blockEntity.getBlockPos());
    }
}

package com.cogilabs.ccwake.chunk;

import com.cogilabs.ccwake.CcWakeMod;
import com.cogilabs.ccwake.config.CcWakeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChunkLoadingManager {

    private static final Map<ServerLevel, ChunkLoadingManager> INSTANCES = new WeakHashMap<>();

    private final Map<String, LoadEntry> loadedNodes = new ConcurrentHashMap<>();
    private final Queue<ChunkOp> pendingOps = new ConcurrentLinkedQueue<>();

    private record LoadEntry(String nodeId, int chunkX, int chunkZ, String dimension, int radius, long expiresAtTick) {
        boolean hasExpiration() {
            return expiresAtTick > 0;
        }
    }

    private sealed interface ChunkOp permits LoadOp, UnloadOp, UnloadAllOp, ChangeRadiusOp {
    }

    private record LoadOp(WakeNodeRegistry.NodeData node, int durationSeconds) implements ChunkOp {
    }

    private record UnloadOp(String nodeId) implements ChunkOp {
    }

    private record UnloadAllOp() implements ChunkOp {
    }

    private record ChangeRadiusOp(String nodeId, int newRadius) implements ChunkOp {
    }

    public static ChunkLoadingManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, k -> new ChunkLoadingManager());
    }

    public static void clearAll() {
        INSTANCES.clear();
    }

    public void requestLoadNode(WakeNodeRegistry.NodeData node, int durationSeconds) {
        pendingOps.add(new LoadOp(node, durationSeconds));
    }

    public void requestUnloadNode(String nodeId) {
        pendingOps.add(new UnloadOp(nodeId));
    }

    public void requestUnloadAll() {
        pendingOps.add(new UnloadAllOp());
    }

    public void requestChangeRadius(String nodeId, int newRadius) {
        pendingOps.add(new ChangeRadiusOp(nodeId, newRadius));
    }

    private static int computeRadius(WakeNodeRegistry.NodeData node) {
        int range = node.range();
        if (range <= 0) return CcWakeConfig.DEFAULT_LOAD_RADIUS.get();
        return (range - 1) / 2;
    }

    private void applyLoadNode(ServerLevel level, WakeNodeRegistry.NodeData node, int durationSeconds) {
        int maxLoaded = CcWakeConfig.MAX_LOADED_NODES.get();
        if (loadedNodes.size() >= maxLoaded && !loadedNodes.containsKey(node.id())) {
            CcWakeMod.LOGGER.warn("[WakeNodes] Cannot load node {} — max loaded nodes ({}) reached", node.id(), maxLoaded);
            return;
        }

        int radius = computeRadius(node);
        long expiresAt = durationSeconds > 0
                ? level.getGameTime() + (long) durationSeconds * 20L
                : -1;

        LoadEntry existing = loadedNodes.get(node.id());
        if (existing != null
                && existing.chunkX() == node.chunkX()
                && existing.chunkZ() == node.chunkZ()
                && Objects.equals(existing.dimension(), node.dimension())) {
            loadedNodes.put(node.id(), new LoadEntry(node.id(), existing.chunkX(), existing.chunkZ(), existing.dimension(), existing.radius(), expiresAt));
            if (CcWakeConfig.ENABLE_NODE_LOGS.get()) {
                if (durationSeconds > 0) {
                    CcWakeMod.LOGGER.info("[WakeNodes] Refreshed node {} for {}s", node.id(), durationSeconds);
                } else {
                    CcWakeMod.LOGGER.info("[WakeNodes] Refreshed node {}", node.id());
                }
            }
            return;
        }

        if (existing != null) {
            applyUnloadNode(level, node.id());
        }

        forceChunks(level, node.chunkX(), node.chunkZ(), radius, true);

        loadedNodes.put(node.id(), new LoadEntry(node.id(), node.chunkX(), node.chunkZ(), node.dimension(), radius, expiresAt));

        if (CcWakeConfig.ENABLE_NODE_LOGS.get()) {
            if (durationSeconds > 0) {
                CcWakeMod.LOGGER.info("[WakeNodes] Loaded node {} for {}s", node.id(), durationSeconds);
            } else {
                CcWakeMod.LOGGER.info("[WakeNodes] Loaded node {}", node.id());
            }
        }
    }

    private void applyUnloadNode(ServerLevel level, String nodeId) {
        LoadEntry entry = loadedNodes.remove(nodeId);
        if (entry != null) {
            forceChunks(level, entry.chunkX(), entry.chunkZ(), entry.radius(), false);

            if (CcWakeConfig.ENABLE_NODE_LOGS.get()) {
                CcWakeMod.LOGGER.info("[WakeNodes] Unloaded node {}", nodeId);
            }
        }
    }

    public void unloadAll(ServerLevel level) {
        for (LoadEntry entry : new ArrayList<>(loadedNodes.values())) {
            forceChunks(level, entry.chunkX(), entry.chunkZ(), entry.radius(), false);
            if (CcWakeConfig.ENABLE_NODE_LOGS.get()) {
                CcWakeMod.LOGGER.info("[WakeNodes] Unloaded node {}", entry.nodeId());
            }
        }
        loadedNodes.clear();
    }

    public void unloadNodeSilent(ServerLevel level, String nodeId) {
        LoadEntry entry = loadedNodes.remove(nodeId);
        if (entry != null) {
            forceChunks(level, entry.chunkX(), entry.chunkZ(), entry.radius(), false);
        }
    }

    public boolean isLoaded(String nodeId) {
        return loadedNodes.containsKey(nodeId);
    }

    public @Nullable Long getExpiresAt(String nodeId) {
        LoadEntry entry = loadedNodes.get(nodeId);
        if (entry != null && entry.hasExpiration()) {
            return entry.expiresAtTick();
        }
        return null;
    }

    public Set<String> getLoadedNodeIds() {
        return Collections.unmodifiableSet(loadedNodes.keySet());
    }

    public void tick(ServerLevel level) {
        processPending(level);

        if (loadedNodes.isEmpty()) return;

        long currentTick = level.getGameTime();
        List<String> toExpire = new ArrayList<>();

        for (LoadEntry entry : loadedNodes.values()) {
            if (entry.hasExpiration() && currentTick >= entry.expiresAtTick()) {
                toExpire.add(entry.nodeId());
            }
        }

        int radius = CcWakeConfig.DEFAULT_LOAD_RADIUS.get();
        for (String nodeId : toExpire) {
            LoadEntry entry = loadedNodes.remove(nodeId);
            if (entry != null) {
                forceChunks(level, entry.chunkX(), entry.chunkZ(), entry.radius(), false);
                if (CcWakeConfig.ENABLE_NODE_LOGS.get()) {
                    CcWakeMod.LOGGER.info("[WakeNodes] Auto-expired node {}", nodeId);
                }
            }
        }
    }

    private void processPending(ServerLevel level) {
        int opsPerTick = CcWakeConfig.CHUNK_OPS_PER_TICK.get();
        int processed = 0;
        while (processed < opsPerTick) {
            ChunkOp op = pendingOps.poll();
            if (op == null) {
                return;
            }

            if (op instanceof LoadOp loadOp) {
                applyLoadNode(level, loadOp.node(), loadOp.durationSeconds());
            } else if (op instanceof UnloadOp unloadOp) {
                applyUnloadNode(level, unloadOp.nodeId());
            } else if (op instanceof ChangeRadiusOp changeOp) {
                applyChangeRadius(level, changeOp.nodeId(), changeOp.newRadius());
            } else if (op instanceof UnloadAllOp) {
                unloadAll(level);
            }

            processed++;
        }
    }

    private void forceChunks(ServerLevel level, int centerChunkX, int centerChunkZ, int radius, boolean add) {
        BlockPos owner = new BlockPos(centerChunkX * 16, 0, centerChunkZ * 16);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;
                ForgeChunkManager.forceChunk(level, CcWakeMod.MOD_ID, owner, cx, cz, add, true);
            }
        }
    }

    private void applyChangeRadius(ServerLevel level, String nodeId, int newRadius) {
        LoadEntry entry = loadedNodes.get(nodeId);
        if (entry == null) return;

        int oldRadius = entry.radius();
        if (oldRadius == newRadius) return;

        // Force all chunks for the new radius (idempotent for overlap)
        forceChunks(level, entry.chunkX(), entry.chunkZ(), newRadius, true);

        // Unforce chunks that were in old radius but not in new radius
        if (newRadius < oldRadius) {
            BlockPos owner = new BlockPos(entry.chunkX() * 16, 0, entry.chunkZ() * 16);
            for (int dx = -oldRadius; dx <= oldRadius; dx++) {
                for (int dz = -oldRadius; dz <= oldRadius; dz++) {
                    if (Math.abs(dx) > newRadius || Math.abs(dz) > newRadius) {
                        int cx = entry.chunkX() + dx;
                        int cz = entry.chunkZ() + dz;
                        ForgeChunkManager.forceChunk(level, CcWakeMod.MOD_ID, owner, cx, cz, false, true);
                    }
                }
            }
        }

        loadedNodes.put(nodeId, new LoadEntry(
                entry.nodeId(), entry.chunkX(), entry.chunkZ(),
                entry.dimension(), newRadius, entry.expiresAtTick()));

        if (CcWakeConfig.ENABLE_NODE_LOGS.get()) {
            CcWakeMod.LOGGER.info("[WakeNodes] Changed radius for node {} from {} to {}", nodeId, oldRadius, newRadius);
        }
    }
}

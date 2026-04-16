package com.cogilabs.ccwake.chunk;

import com.cogilabs.ccwake.CcWakeMod;
import com.cogilabs.ccwake.config.CcWakeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WakeNodeRegistry extends SavedData {

    private static final String DATA_NAME = CcWakeMod.MOD_ID + "_nodes";

    private final Map<String, NodeData> nodes = new HashMap<>();

    public record NodeData(
            String id,
            String dimension,
            BlockPos nodePos,
            BlockPos computerPos,
            int chunkX,
            int chunkZ,
            String attachFace,
            int ownerComputerId,
            Set<Integer> controllerComputerIds,
            int range
    ) {
        public NodeData {
            controllerComputerIds = Collections.unmodifiableSet(new HashSet<>(controllerComputerIds));
        }
    }

    public static WakeNodeRegistry get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return get(serverLevel);
        }
        throw new IllegalStateException("WakeNodeRegistry is server-side only");
    }

    public static WakeNodeRegistry get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) overworld = level;
        return overworld.getDataStorage().computeIfAbsent(
                WakeNodeRegistry::load,
                WakeNodeRegistry::new,
                DATA_NAME
        );
    }

    public void registerNode(String id, String dimension, BlockPos nodePos, BlockPos computerPos,
                             int chunkX, int chunkZ, String attachFace, int ownerComputerId, int range) {
        nodes.put(id, new NodeData(id, dimension, nodePos, computerPos, chunkX, chunkZ, attachFace, ownerComputerId, Set.of(), range));
        setDirty();

        if (CcWakeConfig.ENABLE_NODE_LOGS.get()) {
            CcWakeMod.LOGGER.info("[WakeNodes] Registered node {}", id);
        }
    }

    public void unregisterNode(String id) {
        NodeData removed = nodes.remove(id);
        if (removed != null) {
            setDirty();
            if (CcWakeConfig.ENABLE_NODE_LOGS.get()) {
                CcWakeMod.LOGGER.info("[WakeNodes] Unregistered node {}", id);
            }
        }
    }

    public @Nullable NodeData getNode(String id) {
        return nodes.get(id);
    }

    public void setOwner(String id, int ownerComputerId) {
        NodeData data = nodes.get(id);
        if (data == null) return;

        nodes.put(id, new NodeData(
                data.id(),
                data.dimension(),
                data.nodePos(),
                data.computerPos(),
                data.chunkX(),
                data.chunkZ(),
                data.attachFace(),
                ownerComputerId,
                data.controllerComputerIds(),
                data.range()
        ));
        setDirty();
    }

    public void setRange(String id, int range) {
        NodeData data = nodes.get(id);
        if (data == null) return;

        nodes.put(id, new NodeData(
                data.id(),
                data.dimension(),
                data.nodePos(),
                data.computerPos(),
                data.chunkX(),
                data.chunkZ(),
                data.attachFace(),
                data.ownerComputerId(),
                data.controllerComputerIds(),
                range
        ));
        setDirty();
    }

    public boolean grantController(String id, int controllerComputerId) {
        NodeData data = nodes.get(id);
        if (data == null) return false;
        if (controllerComputerId <= 0 || controllerComputerId == data.ownerComputerId()) return false;

        Set<Integer> updated = new HashSet<>(data.controllerComputerIds());
        boolean changed = updated.add(controllerComputerId);
        if (!changed) return false;

        nodes.put(id, new NodeData(
                data.id(),
                data.dimension(),
                data.nodePos(),
                data.computerPos(),
                data.chunkX(),
                data.chunkZ(),
                data.attachFace(),
                data.ownerComputerId(),
                updated,
                data.range()
        ));
        setDirty();
        return true;
    }

    public boolean revokeController(String id, int controllerComputerId) {
        NodeData data = nodes.get(id);
        if (data == null) return false;

        Set<Integer> updated = new HashSet<>(data.controllerComputerIds());
        boolean changed = updated.remove(controllerComputerId);
        if (!changed) return false;

        nodes.put(id, new NodeData(
                data.id(),
                data.dimension(),
                data.nodePos(),
                data.computerPos(),
                data.chunkX(),
                data.chunkZ(),
                data.attachFace(),
                data.ownerComputerId(),
                updated,
                data.range()
        ));
        setDirty();
        return true;
    }

    public Set<String> getAllNodeIds() {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public Collection<NodeData> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (NodeData node : nodes.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", node.id());
            tag.putString("dimension", node.dimension());
            tag.putInt("nodeX", node.nodePos().getX());
            tag.putInt("nodeY", node.nodePos().getY());
            tag.putInt("nodeZ", node.nodePos().getZ());
            tag.putInt("computerX", node.computerPos().getX());
            tag.putInt("computerY", node.computerPos().getY());
            tag.putInt("computerZ", node.computerPos().getZ());
            tag.putInt("chunkX", node.chunkX());
            tag.putInt("chunkZ", node.chunkZ());
            tag.putString("attachFace", node.attachFace());
            tag.putInt("ownerComputerId", node.ownerComputerId());
            int[] controllerIds = node.controllerComputerIds().stream()
                .mapToInt(Integer::intValue)
                .toArray();
            tag.putIntArray("controllerComputerIds", controllerIds);
            tag.putInt("range", node.range());
            list.add(tag);
        }
        root.put("nodes", list);
        return root;
    }

    public static WakeNodeRegistry load(CompoundTag root) {
        WakeNodeRegistry registry = new WakeNodeRegistry();
        ListTag list = root.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String id = tag.getString("id");
            String dimension = tag.getString("dimension");
            BlockPos nodePos = new BlockPos(tag.getInt("nodeX"), tag.getInt("nodeY"), tag.getInt("nodeZ"));
            BlockPos computerPos = new BlockPos(tag.getInt("computerX"), tag.getInt("computerY"), tag.getInt("computerZ"));
            int chunkX = tag.getInt("chunkX");
            int chunkZ = tag.getInt("chunkZ");
            String attachFace = tag.getString("attachFace");
            int ownerComputerId = tag.contains("ownerComputerId") ? tag.getInt("ownerComputerId") : -1;

            Set<Integer> controllerComputerIds = new HashSet<>();
            int[] rawControllers = tag.getIntArray("controllerComputerIds");
            for (int controllerId : rawControllers) {
                if (controllerId > 0) {
                    controllerComputerIds.add(controllerId);
                }
            }

            int range = tag.contains("range") ? tag.getInt("range") : 0;

            registry.nodes.put(id, new NodeData(
                    id,
                    dimension,
                    nodePos,
                    computerPos,
                    chunkX,
                    chunkZ,
                    attachFace,
                    ownerComputerId,
                    controllerComputerIds,
                    range
            ));
        }
        return registry;
    }
}

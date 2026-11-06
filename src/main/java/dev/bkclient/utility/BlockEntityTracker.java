package dev.bkclient.utility;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

public final class BlockEntityTracker {
    private static final Map<BlockPos, BlockEntity> BLOCK_ENTITIES = new HashMap<>();

    private BlockEntityTracker() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static synchronized void put(BlockEntity blockEntity) {
        if (blockEntity != null && !blockEntity.isRemoved()) {
            BLOCK_ENTITIES.put(blockEntity.getPos().toImmutable(), blockEntity);
        }
    }

    public static synchronized void putAll(Collection<? extends BlockEntity> blockEntities) {
        blockEntities.forEach(BlockEntityTracker::put);
    }

    public static synchronized List<BlockEntity> snapshot(ClientWorld world) {
        prune(world);
        return List.copyOf(BLOCK_ENTITIES.values());
    }

    public static synchronized void clear() {
        BLOCK_ENTITIES.clear();
    }

    public static synchronized void remove(BlockPos pos) {
        BLOCK_ENTITIES.remove(pos);
    }

    private static void prune(ClientWorld world) {
        BLOCK_ENTITIES.values().removeIf(blockEntity ->
                blockEntity == null || blockEntity.isRemoved() || blockEntity.getWorld() != world);
    }
}

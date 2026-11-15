package dev.astanix.esp.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spatial cache container for storage blocks and entities located within a single chunk.
 */
public class StorageChunk {
    private final ChunkPos chunkPos;
    private final UUID worldId;
    private final Map<BlockPos, StorageBlock> blocks = new ConcurrentHashMap<>();
    private final Set<UUID> entities = ConcurrentHashMap.newKeySet();

    public StorageChunk(ChunkPos chunkPos, UUID worldId) {
        this.chunkPos = chunkPos;
        this.worldId = worldId;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void addBlock(StorageBlock block) {
        blocks.put(block.getPos(), block);
    }

    public StorageBlock removeBlock(BlockPos pos) {
        return blocks.remove(pos);
    }

    public StorageBlock getBlock(BlockPos pos) {
        return blocks.get(pos);
    }

    public Collection<StorageBlock> getBlocks() {
        return blocks.values();
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public void addEntity(UUID entityId) {
        entities.add(entityId);
    }

    public void removeEntity(UUID entityId) {
        entities.remove(entityId);
    }

    public Set<UUID> getEntities() {
        return entities;
    }

    public int getEntityCount() {
        return entities.size();
    }

    public boolean isEmpty() {
        return blocks.isEmpty() && entities.isEmpty();
    }

    public void clear() {
        blocks.clear();
        entities.clear();
    }
}

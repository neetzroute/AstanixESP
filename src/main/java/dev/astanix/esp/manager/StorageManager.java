package dev.astanix.esp.manager;

import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.disguise.DisguiseResolver;
import dev.astanix.esp.model.BlockPos;
import dev.astanix.esp.model.ChunkPos;
import dev.astanix.esp.model.StorageBlock;
import dev.astanix.esp.model.StorageChunk;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages spatial in-memory storage block and entity cache indexed by World and Chunk.
 */
public class StorageManager {
    private final PluginConfig config;
    private final DisguiseResolver disguiseResolver;
    private final Logger logger;

    // Spatial index: World UUID -> (Chunk coordinate long key -> StorageChunk)
    private final Map<UUID, Map<Long, StorageChunk>> worldStorage = new ConcurrentHashMap<>();

    public StorageManager(PluginConfig config, DisguiseResolver disguiseResolver, Logger logger) {
        this.config = config;
        this.disguiseResolver = disguiseResolver;
        this.logger = logger;
    }

    /**
     * Scans and indexes all storage tile entities and storage entities within a loaded chunk.
     * Uses chunk.getTileEntities(false) for maximum performance without inventory snapshot overhead.
     */
    public StorageChunk loadChunk(Chunk chunk) {
        World world = chunk.getWorld();
        ChunkPos cPos = ChunkPos.of(chunk);
        StorageChunk storageChunk = getOrCreateStorageChunk(world, cPos.x(), cPos.z());

        // Fast tile entity scan (useSnapshot = false)
        BlockState[] tileEntities = chunk.getTileEntities(false);
        for (BlockState state : tileEntities) {
            Material type = state.getType();
            if (config.isTargetMaterial(type)) {
                Block block = state.getBlock();
                BlockPos pos = BlockPos.of(block);
                BlockData disguise = disguiseResolver.resolveDisguise(world, pos.x(), pos.y(), pos.z());
                StorageBlock storageBlock = new StorageBlock(pos, type, state.getBlockData(), disguise);
                storageChunk.addBlock(storageBlock);
            }
        }

        // Entity scan (storage minecarts, boats)
        Entity[] entities = chunk.getEntities();
        for (Entity entity : entities) {
            if (config.isTargetEntity(entity.getType())) {
                storageChunk.addEntity(entity.getUniqueId());
            }
        }

        if (config.isDebug() && !storageChunk.isEmpty()) {
            logger.info("Indexed chunk [" + cPos.x() + ", " + cPos.z() + "] in " + world.getName() +
                    ": " + storageChunk.getBlockCount() + " blocks, " + storageChunk.getEntityCount() + " entities.");
        }

        return storageChunk;
    }

    /**
     * Unloads and frees spatial cache for an unloaded chunk.
     */
    public StorageChunk unloadChunk(Chunk chunk) {
        World world = chunk.getWorld();
        Map<Long, StorageChunk> map = worldStorage.get(world.getUID());
        if (map != null) {
            long key = ChunkPos.toLong(chunk.getX(), chunk.getZ());
            StorageChunk removed = map.remove(key);
            if (map.isEmpty()) {
                worldStorage.remove(world.getUID());
            }
            return removed;
        }
        return null;
    }

    /**
     * Registers a newly placed storage block.
     */
    public StorageBlock registerBlock(Block block) {
        World world = block.getWorld();
        BlockPos pos = BlockPos.of(block);
        StorageChunk storageChunk = getOrCreateStorageChunk(world, pos.getChunkX(), pos.getChunkZ());

        BlockData disguise = disguiseResolver.resolveDisguise(world, pos.x(), pos.y(), pos.z());
        StorageBlock storageBlock = new StorageBlock(pos, block.getType(), block.getBlockData(), disguise);
        storageChunk.addBlock(storageBlock);
        return storageBlock;
    }

    /**
     * Unregisters a broken or destroyed storage block.
     */
    public StorageBlock unregisterBlock(Block block) {
        World world = block.getWorld();
        BlockPos pos = BlockPos.of(block);
        StorageChunk storageChunk = getStorageChunk(world, pos.getChunkX(), pos.getChunkZ());
        if (storageChunk != null) {
            StorageBlock removed = storageChunk.removeBlock(pos);
            if (storageChunk.isEmpty()) {
                removeChunkIfEmpty(world, pos.getChunkX(), pos.getChunkZ());
            }
            return removed;
        }
        return null;
    }

    /**
     * Recalculates and updates the disguise block for a storage block (e.g. if the block below changed).
     */
    public void updateBlockDisguise(Block block) {
        World world = block.getWorld();
        BlockPos pos = BlockPos.of(block);
        StorageChunk storageChunk = getStorageChunk(world, pos.getChunkX(), pos.getChunkZ());
        if (storageChunk != null) {
            StorageBlock storageBlock = storageChunk.getBlock(pos);
            if (storageBlock != null) {
                BlockData newDisguise = disguiseResolver.resolveDisguise(world, pos.x(), pos.y(), pos.z());
                storageBlock.setDisguiseBlockData(newDisguise);
            }
        }
    }

    /**
     * Registers a spawned storage entity.
     */
    public void registerEntity(Entity entity) {
        World world = entity.getWorld();
        ChunkPos cPos = ChunkPos.of(entity.getLocation());
        StorageChunk storageChunk = getOrCreateStorageChunk(world, cPos.x(), cPos.z());
        storageChunk.addEntity(entity.getUniqueId());
    }

    /**
     * Unregisters a despawned or destroyed storage entity.
     */
    public void unregisterEntity(Entity entity) {
        World world = entity.getWorld();
        ChunkPos cPos = ChunkPos.of(entity.getLocation());
        StorageChunk storageChunk = getStorageChunk(world, cPos.x(), cPos.z());
        if (storageChunk != null) {
            storageChunk.removeEntity(entity.getUniqueId());
            if (storageChunk.isEmpty()) {
                removeChunkIfEmpty(world, cPos.x(), cPos.z());
            }
        }
    }

    public StorageChunk getStorageChunk(World world, int chunkX, int chunkZ) {
        Map<Long, StorageChunk> map = worldStorage.get(world.getUID());
        if (map == null) return null;
        return map.get(ChunkPos.toLong(chunkX, chunkZ));
    }

    public StorageChunk getStorageChunk(Chunk chunk) {
        return getStorageChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    public StorageChunk getOrCreateStorageChunk(World world, int chunkX, int chunkZ) {
        Map<Long, StorageChunk> map = worldStorage.computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>());
        long key = ChunkPos.toLong(chunkX, chunkZ);
        return map.computeIfAbsent(key, k -> new StorageChunk(ChunkPos.of(chunkX, chunkZ), world.getUID()));
    }

    private void removeChunkIfEmpty(World world, int chunkX, int chunkZ) {
        Map<Long, StorageChunk> map = worldStorage.get(world.getUID());
        if (map != null) {
            long key = ChunkPos.toLong(chunkX, chunkZ);
            StorageChunk chunk = map.get(key);
            if (chunk != null && chunk.isEmpty()) {
                map.remove(key);
            }
        }
    }

    public int getTotalChunkCount() {
        int count = 0;
        for (Map<Long, StorageChunk> map : worldStorage.values()) {
            count += map.size();
        }
        return count;
    }

    public int getTotalBlockCount() {
        int count = 0;
        for (Map<Long, StorageChunk> map : worldStorage.values()) {
            for (StorageChunk chunk : map.values()) {
                count += chunk.getBlockCount();
            }
        }
        return count;
    }

    public int getTotalEntityCount() {
        int count = 0;
        for (Map<Long, StorageChunk> map : worldStorage.values()) {
            for (StorageChunk chunk : map.values()) {
                count += chunk.getEntityCount();
            }
        }
        return count;
    }

    public void clearAll() {
        worldStorage.clear();
    }
}

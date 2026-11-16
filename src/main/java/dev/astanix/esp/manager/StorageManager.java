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
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages spatial in-memory storage block and entity cache indexed by World and Chunk.
 * Tracks and links double chests atomically to eliminate texture glitches.
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
     * Links double chests together atomically.
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
                StorageBlock storageBlock = new StorageBlock(pos, type, block.getBlockData(), disguise);
                storageChunk.addBlock(storageBlock);
            }
        }

        // Link double chests in this chunk and neighboring boundaries
        linkDoubleChests(storageChunk, world);

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
     * Finds and pairs both halves of double chests together so they are treated atomically.
     */
    public void linkDoubleChests(StorageChunk storageChunk, World world) {
        for (StorageBlock block : storageChunk.getBlocks()) {
            if (block.isDoubleChest()) continue;

            BlockData bd = block.getOriginalBlockData();
            if (bd instanceof Chest chestData) {
                if (chestData.getType() == Chest.Type.SINGLE) continue;

                BlockFace connectedFace = getConnectedChestFace(chestData);
                if (connectedFace == null) continue;

                BlockPos neighborPos = new BlockPos(
                        block.getPos().x() + connectedFace.getModX(),
                        block.getPos().y() + connectedFace.getModY(),
                        block.getPos().z() + connectedFace.getModZ()
                );

                StorageBlock neighborBlock = getStorageBlock(world, neighborPos);
                if (neighborBlock != null && neighborBlock.getOriginalBlockData() instanceof Chest neighborChest) {
                    if (neighborChest.getFacing() == chestData.getFacing() &&
                            neighborChest.getType() != Chest.Type.SINGLE &&
                            neighborChest.getType() != chestData.getType()) {
                        block.setPairedBlock(neighborBlock);
                        neighborBlock.setPairedBlock(block);
                    }
                }
            }
        }
    }

    /**
     * Calculates the direction to the other half of a double chest based on facing and chest type.
     */
    public static BlockFace getConnectedChestFace(Chest chest) {
        BlockFace facing = chest.getFacing();
        Chest.Type type = chest.getType();
        if (type == Chest.Type.SINGLE) return null;

        // In Minecraft:
        // When facing NORTH: LEFT half has partner at EAST, RIGHT half has partner at WEST
        // When facing SOUTH: LEFT half has partner at WEST, RIGHT half has partner at EAST
        // When facing EAST:  LEFT half has partner at SOUTH, RIGHT half has partner at NORTH
        // When facing WEST:  LEFT half has partner at NORTH, RIGHT half has partner at SOUTH
        return switch (facing) {
            case NORTH -> (type == Chest.Type.LEFT) ? BlockFace.EAST : BlockFace.WEST;
            case SOUTH -> (type == Chest.Type.LEFT) ? BlockFace.WEST : BlockFace.EAST;
            case EAST -> (type == Chest.Type.LEFT) ? BlockFace.SOUTH : BlockFace.NORTH;
            case WEST -> (type == Chest.Type.LEFT) ? BlockFace.NORTH : BlockFace.SOUTH;
            default -> null;
        };
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

        // Check if placing this block connects it to form a double chest
        if (block.getBlockData() instanceof Chest chestData && chestData.getType() != Chest.Type.SINGLE) {
            BlockFace connectedFace = getConnectedChestFace(chestData);
            if (connectedFace != null) {
                BlockPos neighborPos = new BlockPos(
                        pos.x() + connectedFace.getModX(),
                        pos.y() + connectedFace.getModY(),
                        pos.z() + connectedFace.getModZ()
                );
                StorageBlock neighbor = getStorageBlock(world, neighborPos);
                if (neighbor != null) {
                    storageBlock.setPairedBlock(neighbor);
                    neighbor.setPairedBlock(storageBlock);
                }
            }
        }

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
            if (removed != null && removed.isDoubleChest()) {
                StorageBlock paired = removed.getPairedBlock();
                if (paired != null) {
                    paired.setPairedBlock(null);
                }
            }
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

    public StorageBlock getStorageBlock(World world, BlockPos pos) {
        StorageChunk sc = getStorageChunk(world, pos.getChunkX(), pos.getChunkZ());
        return (sc != null) ? sc.getBlock(pos) : null;
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

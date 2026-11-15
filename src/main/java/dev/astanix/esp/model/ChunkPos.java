package dev.astanix.esp.model;

import org.bukkit.Chunk;
import org.bukkit.Location;

/**
 * Immutable chunk coordinate record (x, z).
 */
public record ChunkPos(int x, int z) {

    public static ChunkPos of(int x, int z) {
        return new ChunkPos(x, z);
    }

    public static ChunkPos of(Chunk chunk) {
        return new ChunkPos(chunk.getX(), chunk.getZ());
    }

    public static ChunkPos of(Location loc) {
        return new ChunkPos(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public static ChunkPos ofBlock(int blockX, int blockZ) {
        return new ChunkPos(blockX >> 4, blockZ >> 4);
    }

    public static long toLong(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public long asLong() {
        return toLong(x, z);
    }
}

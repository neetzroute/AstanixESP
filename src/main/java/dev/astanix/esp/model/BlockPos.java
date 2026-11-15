package dev.astanix.esp.model;

import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Immutable, lightweight 3D integer block coordinate.
 */
public record BlockPos(int x, int y, int z) {

    public static BlockPos of(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    public static BlockPos of(Location loc) {
        return new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static BlockPos of(Block block) {
        return new BlockPos(block.getX(), block.getY(), block.getZ());
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    public BlockPosition toPaperPosition() {
        return Position.block(x, y, z);
    }

    /**
     * Calculates squared Euclidean distance from the center of this block (x + 0.5, y + 0.5, z + 0.5)
     * to the given player coordinates.
     */
    public double distanceSquared(double px, double py, double pz) {
        double dx = (x + 0.5) - px;
        double dy = (y + 0.5) - py;
        double dz = (z + 0.5) - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Encodes this block position into a unique long key.
     * Compatible with standard Minecraft coordinate ranges (-30M to +30M, Y -2048 to 2047).
     */
    public long asLong() {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    public int getChunkX() {
        return x >> 4;
    }

    public int getChunkZ() {
        return z >> 4;
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos(x >> 4, z >> 4);
    }
}

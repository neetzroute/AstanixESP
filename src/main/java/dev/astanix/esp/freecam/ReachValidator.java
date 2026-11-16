package dev.astanix.esp.freecam;

import dev.astanix.esp.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/**
 * Validates interaction reach distances between player eye location and targets.
 */
public class ReachValidator {
    private final PluginConfig config;

    public ReachValidator(PluginConfig config) {
        this.config = config;
    }

    public boolean checkBlockReach(Player player, Block block, double[] outDistance) {
        if (!config.isFreeCamReachEnabled()) return true;

        Location eyeLoc = player.getEyeLocation();
        if (!eyeLoc.getWorld().equals(block.getWorld())) return false;

        double eyeX = eyeLoc.getX();
        double eyeY = eyeLoc.getY();
        double eyeZ = eyeLoc.getZ();

        double minX = block.getX();
        double minY = block.getY();
        double minZ = block.getZ();
        double maxX = minX + 1.0;
        double maxY = minY + 1.0;
        double maxZ = minZ + 1.0;

        double closestX = Math.clamp(eyeX, minX, maxX);
        double closestY = Math.clamp(eyeY, minY, maxY);
        double closestZ = Math.clamp(eyeZ, minZ, maxZ);

        double dx = eyeX - closestX;
        double dy = eyeY - closestY;
        double dz = eyeZ - closestZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (outDistance != null && outDistance.length > 0) {
            outDistance[0] = dist;
        }

        double maxReach = config.getFreeCamMaxReach(player.getGameMode());
        return dist <= maxReach;
    }

    public boolean checkEntityReach(Player player, Entity entity, double[] outDistance) {
        if (!config.isFreeCamReachEnabled()) return true;

        Location eyeLoc = player.getEyeLocation();
        if (!eyeLoc.getWorld().equals(entity.getWorld())) return false;

        BoundingBox bb = entity.getBoundingBox();
        double eyeX = eyeLoc.getX();
        double eyeY = eyeLoc.getY();
        double eyeZ = eyeLoc.getZ();

        double closestX = Math.clamp(eyeX, bb.getMinX(), bb.getMaxX());
        double closestY = Math.clamp(eyeY, bb.getMinY(), bb.getMaxY());
        double closestZ = Math.clamp(eyeZ, bb.getMinZ(), bb.getMaxZ());

        double dx = eyeX - closestX;
        double dy = eyeY - closestY;
        double dz = eyeZ - closestZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (outDistance != null && outDistance.length > 0) {
            outDistance[0] = dist;
        }

        double maxReach = config.getFreeCamMaxReach(player.getGameMode());
        return dist <= maxReach;
    }
}

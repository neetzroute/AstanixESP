package dev.astanix.esp.freecam;

import dev.astanix.esp.config.PluginConfig;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Validates Line of Sight (LOS) between player eyes and target objects using native Paper raytracing.
 */
public class RaytraceValidator {
    private final PluginConfig config;

    public RaytraceValidator(PluginConfig config) {
        this.config = config;
    }

    public boolean hasLineOfSight(Player player, Block target, BlockFace face) {
        if (!config.isFreeCamRaytraceEnabled()) return true;

        Location eyeLoc = player.getEyeLocation();
        World world = eyeLoc.getWorld();
        if (world == null || !world.equals(target.getWorld())) return false;

        if (eyeLoc.getBlockX() == target.getX() &&
                eyeLoc.getBlockY() == target.getY() &&
                eyeLoc.getBlockZ() == target.getZ()) {
            return true;
        }

        Vector eyePos = eyeLoc.toVector();
        Vector targetBase = new Vector(target.getX(), target.getY(), target.getZ());

        Vector[] samplePoints = getBlockSamplePoints(targetBase, face);
        int sampleCount = Math.min(samplePoints.length, config.getFreeCamMultiRaySamples());

        for (int i = 0; i < sampleCount; i++) {
            Vector sample = samplePoints[i];
            Vector dir = sample.clone().subtract(eyePos);
            double dist = dir.length();

            if (dist < 0.001) return true;

            dir.normalize();

            RayTraceResult result = world.rayTraceBlocks(
                    eyeLoc,
                    dir,
                    dist - 0.05,
                    FluidCollisionMode.NEVER,
                    true
            );

            if (result == null || result.getHitBlock() == null) {
                return true;
            }

            Block hit = result.getHitBlock();
            if (hit.equals(target) || config.isFreeCamIgnoredBlock(hit.getType())) {
                return true;
            }
        }

        return false;
    }

    public boolean hasLineOfSight(Player player, Entity entity) {
        if (!config.isFreeCamRaytraceEnabled() || !config.isFreeCamCheckEntities()) return true;

        Location eyeLoc = player.getEyeLocation();
        World world = eyeLoc.getWorld();
        if (world == null || !world.equals(entity.getWorld())) return false;

        Vector eyePos = eyeLoc.toVector();
        Location entLoc = entity.getLocation();
        double height = entity.getHeight();

        Vector[] samples = new Vector[]{
                entLoc.toVector().add(new Vector(0, height * 0.85, 0)),
                entLoc.toVector().add(new Vector(0, height * 0.5, 0)),
                entLoc.toVector().add(new Vector(0, height * 0.15, 0))
        };

        for (Vector sample : samples) {
            Vector dir = sample.clone().subtract(eyePos);
            double dist = dir.length();
            if (dist < 0.001) return true;

            dir.normalize();

            RayTraceResult result = world.rayTraceBlocks(
                    eyeLoc,
                    dir,
                    dist - 0.05,
                    FluidCollisionMode.NEVER,
                    true
            );

            if (result == null || result.getHitBlock() == null || config.isFreeCamIgnoredBlock(result.getHitBlock().getType())) {
                return true;
            }
        }

        return false;
    }

    private Vector[] getBlockSamplePoints(Vector base, BlockFace face) {
        if (face == null) {
            return new Vector[]{
                    base.clone().add(new Vector(0.5, 0.5, 0.5)),
                    base.clone().add(new Vector(0.2, 0.8, 0.2)),
                    base.clone().add(new Vector(0.8, 0.8, 0.2)),
                    base.clone().add(new Vector(0.2, 0.2, 0.8)),
                    base.clone().add(new Vector(0.8, 0.2, 0.8))
            };
        }

        double modX = face.getModX();
        double modY = face.getModY();
        double modZ = face.getModZ();

        Vector faceCenter = base.clone().add(new Vector(
                0.5 + modX * 0.45,
                0.5 + modY * 0.45,
                0.5 + modZ * 0.45
        ));

        Vector u = (Math.abs(modY) == 1) ? new Vector(0.3, 0, 0) : new Vector(0, 0.3, 0);
        Vector v = (Math.abs(modZ) == 1) ? new Vector(0.3, 0, 0) : new Vector(0, 0.3, 0);

        return new Vector[]{
                faceCenter,
                faceCenter.clone().add(u).add(v),
                faceCenter.clone().add(u).subtract(v),
                faceCenter.clone().subtract(u).add(v),
                faceCenter.clone().subtract(u).subtract(v)
        };
    }
}

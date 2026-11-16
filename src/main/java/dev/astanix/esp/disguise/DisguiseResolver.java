package dev.astanix.esp.disguise;

import dev.astanix.esp.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

/**
 * Intelligent contextual disguise resolver.
 * Computes natural visual replacements for storage blocks based on their supporting environment.
 */
public class DisguiseResolver {
    private static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final PluginConfig config;

    public DisguiseResolver(PluginConfig config) {
        this.config = config;
    }

    /**
     * Resolves the visual disguise BlockData for a target storage block located at (x, y, z) in the world.
     *
     * Rules:
     * 1. Inspect downwards up to configured max depth for the supporting surface.
     * 2. If resting on GRASS_BLOCK -> replaces with GRASS_BLOCK (seamless natural grass terrain).
     * 3. If resting on DIRT_PATH -> replaces with DIRT_PATH.
     * 4. If resting on DIRT -> replaces with DIRT.
     * 5. If resting on SNOW/SNOW_BLOCK -> replaces with SNOW_BLOCK.
     * 6. If resting on any solid opaque block (STONE, PLANKS, BRICKS, etc.) -> replaces with that block type.
     * 7. If suspended/floating in air -> continues downward scan until reaching solid ground.
     * 8. If no ground below (e.g. hung over void) -> checks horizontal wall neighbors and ceiling.
     * 9. Fallback to dimension-appropriate solid material (STONE/DEEPSLATE/NETHERRACK/END_STONE).
     */
    public BlockData resolveDisguise(World world, int x, int y, int z) {
        int minY = world.getMinHeight();
        int maxSearch = config.getMaxDownwardSearch();

        // 1. Scan downwards to find supporting surface
        for (int dy = 1; dy <= maxSearch; dy++) {
            int checkY = y - dy;
            if (checkY < minY) break;

            Block block = world.getBlockAt(x, checkY, z);
            Material mat = block.getType();

            // Skip air, fluids, and other target storage containers (e.g. stacked chests)
            if (mat.isAir() || isFluid(mat) || config.isTargetMaterial(mat)) {
                continue;
            }

            if (isEligibleDisguise(mat)) {
                return mapToDisguiseBlockData(mat, block.getBlockData());
            }
        }

        // 2. Check horizontal neighbors (attached to wall)
        for (BlockFace face : HORIZONTAL_FACES) {
            Block neighbor = world.getBlockAt(x + face.getModX(), y, z + face.getModZ());
            Material mat = neighbor.getType();
            if (!mat.isAir() && !isFluid(mat) && !config.isTargetMaterial(mat) && isEligibleDisguise(mat)) {
                return mapToDisguiseBlockData(mat, neighbor.getBlockData());
            }
        }

        // 3. Check ceiling above (hung from ceiling)
        if (y + 1 < world.getMaxHeight()) {
            Block above = world.getBlockAt(x, y + 1, z);
            Material mat = above.getType();
            if (!mat.isAir() && !isFluid(mat) && !config.isTargetMaterial(mat) && isEligibleDisguise(mat)) {
                return mapToDisguiseBlockData(mat, above.getBlockData());
            }
        }

        // 4. Fallback based on dimension and elevation
        return getFallbackBlockData(world, y);
    }

    /**
     * Maps supporting material to natural disguise block data.
     */
    private BlockData mapToDisguiseBlockData(Material mat, BlockData actualData) {
        // Natural terrain blocks: mask as the EXACT supporting block
        if (mat == Material.GRASS_BLOCK) {
            return Material.GRASS_BLOCK.createBlockData();
        }
        if (mat == Material.DIRT_PATH) {
            return Material.DIRT_PATH.createBlockData();
        }
        if (mat == Material.PODZOL) {
            return Material.PODZOL.createBlockData();
        }
        if (mat == Material.MYCELIUM) {
            return Material.MYCELIUM.createBlockData();
        }
        if (mat == Material.DIRT || mat == Material.COARSE_DIRT || mat == Material.ROOTED_DIRT) {
            return mat.createBlockData();
        }

        // Snow variations -> Snow Block
        if (mat == Material.SNOW || mat == Material.SNOW_BLOCK || mat == Material.POWDER_SNOW) {
            return Material.SNOW_BLOCK.createBlockData();
        }

        // If it's a solid, full block (planks, stone, cobblestone, deepslate, bricks, sand, etc.)
        if (mat.isBlock() && mat.isSolid()) {
            try {
                return mat.createBlockData();
            } catch (Exception ignored) {}
        }

        return Material.STONE.createBlockData();
    }

    /**
     * Checks if a material is a suitable disguise block (solid, non-hazardous, occluding).
     */
    private boolean isEligibleDisguise(Material mat) {
        if (!mat.isSolid() || !mat.isBlock()) return false;
        if (mat.isAir()) return false;

        // Exclude dangerous, interactive, or transparent materials
        if (mat == Material.TNT || mat == Material.RESPAWN_ANCHOR || mat == Material.FIRE ||
                mat == Material.SOUL_FIRE || mat == Material.CAMPFIRE || mat == Material.SOUL_CAMPFIRE) {
            return false;
        }

        // Exclude see-through blocks like glass or open slabs that could cause visual artifacts
        String name = mat.name();
        if (name.contains("GLASS") || name.contains("SLAB") || name.contains("STAIRS") ||
                name.contains("FENCE") || name.contains("WALL") || name.contains("DOOR") ||
                name.contains("TRAPDOOR") || name.contains("GATE")) {
            return false;
        }

        return mat.isOccluding() || mat.isSolid();
    }

    private boolean isFluid(Material mat) {
        return mat == Material.WATER || mat == Material.LAVA;
    }

    /**
     * Provides a context-aware fallback block if no environment support was found.
     */
    public BlockData getFallbackBlockData(World world, int y) {
        World.Environment env = world.getEnvironment();
        if (env == World.Environment.NETHER) {
            return config.getFallbackNether().createBlockData();
        }
        if (env == World.Environment.THE_END) {
            return config.getFallbackTheEnd().createBlockData();
        }
        if (y < 0) {
            return config.getFallbackOverworldDeepslate().createBlockData();
        }
        return config.getFallbackOverworld().createBlockData();
    }
}

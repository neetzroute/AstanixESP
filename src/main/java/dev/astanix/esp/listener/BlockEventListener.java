package dev.astanix.esp.listener;

import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.manager.StorageManager;
import dev.astanix.esp.model.BlockPos;
import dev.astanix.esp.model.StorageBlock;
import dev.astanix.esp.tracker.PlayerTrackerService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

/**
 * Listens to block state modifications (place, break, explosions, pistons).
 */
public class BlockEventListener implements Listener {
    private final PluginConfig config;
    private final StorageManager storageManager;
    private final PlayerTrackerService trackerService;

    public BlockEventListener(PluginConfig config, StorageManager storageManager, PlayerTrackerService trackerService) {
        this.config = config;
        this.storageManager = storageManager;
        this.trackerService = trackerService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        if (config.isTargetMaterial(type)) {
            StorageBlock storageBlock = storageManager.registerBlock(block);
            trackerService.onBlockPlaced(storageBlock, block.getWorld());
        }

        // If placing a block beneath a storage block, update the storage block's disguise
        Block above = block.getRelative(BlockFace.UP);
        if (config.isTargetMaterial(above.getType())) {
            storageManager.updateBlockDisguise(above);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        if (config.isTargetMaterial(type)) {
            BlockPos pos = BlockPos.of(block);
            storageManager.unregisterBlock(block);
            trackerService.onBlockBroken(pos, Material.AIR.createBlockData(), block.getWorld());
        }

        // If breaking a block beneath a storage block, update the storage block's disguise
        Block above = block.getRelative(BlockFace.UP);
        if (config.isTargetMaterial(above.getType())) {
            storageManager.updateBlockDisguise(above);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList(), event.getBlock().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList(), event.getLocation().getWorld());
    }

    private void handleExplosion(List<Block> blocks, World world) {
        if (world == null) return;
        for (Block b : blocks) {
            if (config.isTargetMaterial(b.getType())) {
                BlockPos pos = BlockPos.of(b);
                storageManager.unregisterBlock(b);
                trackerService.onBlockBroken(pos, Material.AIR.createBlockData(), world);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (config.isTargetMaterial(block.getType())) {
            BlockPos pos = BlockPos.of(block);
            storageManager.unregisterBlock(block);
            trackerService.onBlockBroken(pos, Material.AIR.createBlockData(), block.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePiston(event.getBlocks(), event.getBlock().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePiston(event.getBlocks(), event.getBlock().getWorld());
    }

    private void handlePiston(List<Block> blocks, World world) {
        if (world == null) return;
        for (Block b : blocks) {
            if (config.isTargetMaterial(b.getType())) {
                BlockPos pos = BlockPos.of(b);
                storageManager.unregisterBlock(b);
                trackerService.onBlockBroken(pos, Material.AIR.createBlockData(), world);
            }
        }
    }
}

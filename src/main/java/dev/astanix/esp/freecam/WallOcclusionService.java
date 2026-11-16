package dev.astanix.esp.freecam;

import dev.astanix.esp.AstanixESPPlugin;
import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.manager.StorageManager;
import dev.astanix.esp.model.BlockPos;
import dev.astanix.esp.model.StorageBlock;
import dev.astanix.esp.model.StorageChunk;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time wall occlusion service for containers.
 * Prevents FreeCam from seeing chests, barrels, and shulkers through solid walls
 * by masking occluded containers as their disguise blocks (stone, floor, etc.).
 */
public class WallOcclusionService {
    private final AstanixESPPlugin plugin;
    private final PluginConfig config;
    private final StorageManager storageManager;
    private final RaytraceValidator raytraceValidator;
    private final ViolationManager violationManager;

    private final Map<UUID, Set<BlockPos>> playerHiddenBlocks = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;

    public WallOcclusionService(AstanixESPPlugin plugin, PluginConfig config,
                                StorageManager storageManager,
                                RaytraceValidator raytraceValidator,
                                ViolationManager violationManager) {
        this.plugin = plugin;
        this.config = config;
        this.storageManager = storageManager;
        this.raytraceValidator = raytraceValidator;
        this.violationManager = violationManager;
    }

    public void start() {
        stop();
        if (!config.isFreeCamOcclusionEnabled()) return;
        this.tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 3L, 3L);
    }

    public void stop() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        restoreAll();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (violationManager.hasBypass(player)) {
                restorePlayer(player);
                continue;
            }
            updatePlayer(player);
        }
    }

    private void updatePlayer(Player player) {
        Location eyeLoc = player.getEyeLocation();
        World world = eyeLoc.getWorld();
        if (world == null) return;

        int pChunkX = eyeLoc.getBlockX() >> 4;
        int pChunkZ = eyeLoc.getBlockZ() >> 4;

        Set<BlockPos> currentlyHidden = playerHiddenBlocks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        Set<BlockPos> shouldBeHidden = new HashSet<>();

        double maxDistSq = config.getFreeCamOcclusionRadius() * config.getFreeCamOcclusionRadius();
        double hideDistSq = config.getHideDistanceSquared();

        // Check 3x3 chunks around player (covers ~16-20 blocks)
        for (int cx = pChunkX - 1; cx <= pChunkX + 1; cx++) {
            for (int cz = pChunkZ - 1; cz <= pChunkZ + 1; cz++) {
                StorageChunk sc = storageManager.getStorageChunk(world, cx, cz);
                if (sc == null || sc.isEmpty()) continue;

                for (StorageBlock sb : sc.getBlocks()) {
                    BlockPos pos = sb.getPos();
                    double distSq = pos.distanceSquared(eyeLoc.getBlockX(), eyeLoc.getBlockY(), eyeLoc.getBlockZ());

                    // If distance is beyond hideDistance (25m), PlayerTrackerService already disguises it
                    if (distSq > hideDistSq || distSq > maxDistSq) continue;

                    Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
                    boolean hasLoS = raytraceValidator.hasLineOfSight(player, block, null);

                    if (!hasLoS) {
                        shouldBeHidden.add(pos);
                        if (currentlyHidden.add(pos)) {
                            player.sendBlockChange(block.getLocation(), sb.getDisguiseBlockData());
                        }

                        // Atomic double chest handling
                        if (sb.isDoubleChest()) {
                            StorageBlock paired = sb.getPairedBlock();
                            if (paired != null) {
                                BlockPos pPos = paired.getPos();
                                shouldBeHidden.add(pPos);
                                if (currentlyHidden.add(pPos)) {
                                    Block pBlock = world.getBlockAt(pPos.x(), pPos.y(), pPos.z());
                                    player.sendBlockChange(pBlock.getLocation(), paired.getDisguiseBlockData());
                                }
                            }
                        }
                    }
                }
            }
        }

        // Restore blocks that now have clear line of sight
        Iterator<BlockPos> it = currentlyHidden.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (!shouldBeHidden.contains(pos)) {
                it.remove();
                StorageBlock sb = storageManager.getStorageBlock(world, pos);
                BlockData realData = (sb != null) ? sb.getOriginalBlockData() : world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData();
                player.sendBlockChange(new Location(world, pos.x(), pos.y(), pos.z()), realData);
            }
        }
    }

    public void restorePlayer(Player player) {
        Set<BlockPos> hidden = playerHiddenBlocks.remove(player.getUniqueId());
        if (hidden == null || hidden.isEmpty()) return;

        World world = player.getWorld();
        for (BlockPos pos : hidden) {
            StorageBlock sb = storageManager.getStorageBlock(world, pos);
            BlockData realData = (sb != null) ? sb.getOriginalBlockData() : world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData();
            player.sendBlockChange(new Location(world, pos.x(), pos.y(), pos.z()), realData);
        }
    }

    public void restoreAll() {
        for (UUID uuid : new ArrayList<>(playerHiddenBlocks.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                restorePlayer(p);
            }
        }
        playerHiddenBlocks.clear();
    }

    public int getHiddenCount(Player player) {
        Set<BlockPos> hidden = playerHiddenBlocks.get(player.getUniqueId());
        return hidden != null ? hidden.size() : 0;
    }
}

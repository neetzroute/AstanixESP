package dev.astanix.esp.tracker;

import dev.astanix.esp.AstanixESPPlugin;
import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.manager.StorageManager;
import dev.astanix.esp.model.BlockPos;
import dev.astanix.esp.model.PlayerState;
import dev.astanix.esp.model.StorageBlock;
import dev.astanix.esp.model.StorageChunk;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * High-performance tracker service responsible for distance calculation,
 * tick-distributed player checking, and batch packet dispatching.
 */
public class PlayerTrackerService {
    private final AstanixESPPlugin plugin;
    private final PluginConfig config;
    private final StorageManager storageManager;
    private final Logger logger;

    private final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;
    private long currentTick = 0;

    public PlayerTrackerService(AstanixESPPlugin plugin, PluginConfig config,
                                StorageManager storageManager, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.storageManager = storageManager;
        this.logger = logger;
    }

    public void start() {
        stop();
        // Run tick task at interval 1 tick, but spread players evenly across checkIntervalTicks
        this.tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
    }

    private void tick() {
        long tickIndex = currentTick++;
        long interval = config.getCheckIntervalTicks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Tick spreading: evenly distribute players across ticks
            if ((Math.abs(player.getEntityId()) + tickIndex) % interval != 0) {
                continue;
            }
            checkPlayer(player, false);
        }
    }

    /**
     * Evaluates a player's field of perception and sends batched block and entity packets.
     *
     * @param player the player to check
     * @param force  if true, ignores movement threshold and enforces full refresh
     */
    public void checkPlayer(Player player, boolean force) {
        if (!player.isOnline()) return;

        PlayerState state = getPlayerState(player);
        if (state.isBypass() && player.hasPermission("astanix.esp.bypass")) {
            return;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Movement optimization: skip if player barely moved
        Location lastLoc = state.getLastCheckLocation();
        if (!force && lastLoc != null && world.equals(lastLoc.getWorld())) {
            double movedDistSq = loc.distanceSquared(lastLoc);
            if (movedDistSq < 0.25) { // less than 0.5 block movement
                return;
            }
        }
        state.setLastCheckLocation(loc.clone());

        double px = loc.getX();
        double py = loc.getY();
        double pz = loc.getZ();
        double hideDistSq = config.getHideDistanceSq();
        double hideDist = config.getHideDistance();

        // Calculate chunk radius needed to cover hideDistance
        int chunkRadius = (int) Math.ceil(hideDist / 16.0) + 1;
        int pChunkX = loc.getBlockX() >> 4;
        int pChunkZ = loc.getBlockZ() >> 4;

        Map<Position, BlockData> toDisguise = new HashMap<>();
        Map<Position, BlockData> toReveal = new HashMap<>();
        List<Entity> toHideEntities = new ArrayList<>();
        List<Entity> toShowEntities = new ArrayList<>();

        // 1. Scan chunks within the spatial bounding window
        for (int cx = pChunkX - chunkRadius; cx <= pChunkX + chunkRadius; cx++) {
            for (int cz = pChunkZ - chunkRadius; cz <= pChunkZ + chunkRadius; cz++) {
                StorageChunk storageChunk = storageManager.getStorageChunk(world, cx, cz);
                if (storageChunk == null || storageChunk.isEmpty()) {
                    continue;
                }

                // Check blocks
                for (StorageBlock block : storageChunk.getBlocks()) {
                    BlockPos bPos = block.getPos();
                    double distSq = bPos.distanceSquared(px, py, pz);

                    if (distSq <= hideDistSq) {
                        // In proximity -> Reveal real block
                        if (state.markBlockRevealed(bPos)) {
                            toReveal.put(bPos.toPaperPosition(), block.getOriginalBlockData());
                        }
                    } else {
                        // Distant -> Disguise block
                        if (state.markBlockHidden(bPos)) {
                            toDisguise.put(bPos.toPaperPosition(), block.getDisguiseBlockData());
                        }
                    }
                }

                // Check entities (storage minecarts, boats)
                for (UUID entityId : storageChunk.getEntities()) {
                    Entity entity = Bukkit.getEntity(entityId);
                    if (entity == null || !entity.isValid()) continue;

                    double entDistSq = entity.getLocation().distanceSquared(loc);
                    if (entDistSq <= hideDistSq) {
                        if (state.markEntityRevealed(entityId)) {
                            toShowEntities.add(entity);
                        }
                    } else {
                        if (state.markEntityHidden(entityId)) {
                            toHideEntities.add(entity);
                        }
                    }
                }
            }
        }

        // 2. Validate previously revealed blocks that may now be outside range or in different chunks
        Iterator<BlockPos> blockIt = state.getRevealedBlocks().iterator();
        while (blockIt.hasNext()) {
            BlockPos bPos = blockIt.next();
            double distSq = bPos.distanceSquared(px, py, pz);
            if (distSq > hideDistSq) {
                blockIt.remove();
                StorageChunk sc = storageManager.getStorageChunk(world, bPos.getChunkX(), bPos.getChunkZ());
                BlockData disguise = (sc != null && sc.getBlock(bPos) != null)
                        ? sc.getBlock(bPos).getDisguiseBlockData()
                        : plugin.getDisguiseResolver().getFallbackBlockData(world, bPos.y());
                toDisguise.put(bPos.toPaperPosition(), disguise);
            }
        }

        // 3. Validate previously revealed entities
        Iterator<UUID> entIt = state.getRevealedEntities().iterator();
        while (entIt.hasNext()) {
            UUID entityId = entIt.next();
            Entity entity = Bukkit.getEntity(entityId);
            if (entity == null || !entity.isValid() || !entity.getWorld().equals(world) ||
                    entity.getLocation().distanceSquared(loc) > hideDistSq) {
                entIt.remove();
                if (entity != null && entity.isValid()) {
                    toHideEntities.add(entity);
                }
            }
        }

        // 4. Send batched block updates
        if (!toDisguise.isEmpty()) {
            player.sendMultiBlockChange(toDisguise);
        }
        if (!toReveal.isEmpty()) {
            player.sendMultiBlockChange(toReveal);
        }

        // 5. Send entity visibility updates
        for (Entity entity : toHideEntities) {
            player.hideEntity(plugin, entity);
        }
        for (Entity entity : toShowEntities) {
            player.showEntity(plugin, entity);
        }
    }

    /**
     * Handled immediately when Paper dispatches a chunk to a player.
     * Prevents Storage ESP outline flicker before ticker runs.
     */
    public void onPlayerChunkLoad(Player player, org.bukkit.Chunk chunk) {
        StorageChunk sc = storageManager.getStorageChunk(chunk);
        if (sc == null || sc.isEmpty()) return;

        PlayerState state = getPlayerState(player);
        if (state.isBypass() && player.hasPermission("astanix.esp.bypass")) return;

        Location pLoc = player.getLocation();
        if (!chunk.getWorld().equals(pLoc.getWorld())) return;

        double px = pLoc.getX();
        double py = pLoc.getY();
        double pz = pLoc.getZ();
        double hideDistSq = config.getHideDistanceSq();

        Map<Position, BlockData> chunkDisguises = new HashMap<>();

        for (StorageBlock block : sc.getBlocks()) {
            BlockPos bPos = block.getPos();
            double dSq = bPos.distanceSquared(px, py, pz);
            if (dSq > hideDistSq) {
                chunkDisguises.put(bPos.toPaperPosition(), block.getDisguiseBlockData());
                state.markBlockHidden(bPos);
            } else {
                state.markBlockRevealed(bPos);
            }
        }

        if (!chunkDisguises.isEmpty()) {
            player.sendMultiBlockChange(chunkDisguises);
        }

        for (UUID entityId : sc.getEntities()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                double entDistSq = entity.getLocation().distanceSquared(pLoc);
                if (entDistSq > hideDistSq) {
                    player.hideEntity(plugin, entity);
                    state.markEntityHidden(entityId);
                } else {
                    state.markEntityRevealed(entityId);
                }
            }
        }
    }

    /**
     * Notification when a new storage block is placed in the world.
     */
    public void onBlockPlaced(StorageBlock block, World world) {
        BlockPos pos = block.getPos();
        double hideDistSq = config.getHideDistanceSq();

        for (Player player : world.getPlayers()) {
            PlayerState state = getPlayerState(player);
            if (state.isBypass() && player.hasPermission("astanix.esp.bypass")) continue;

            double dSq = pos.distanceSquared(player.getLocation().getX(),
                    player.getLocation().getY(), player.getLocation().getZ());

            if (dSq > hideDistSq) {
                state.markBlockHidden(pos);
                player.sendBlockChange(pos.toLocation(world), block.getDisguiseBlockData());
            } else {
                state.markBlockRevealed(pos);
            }
        }
    }

    /**
     * Notification when a storage block is destroyed or removed.
     */
    public void onBlockBroken(BlockPos pos, BlockData newBlockData, World world) {
        for (Player player : world.getPlayers()) {
            PlayerState state = getPlayerState(player);
            state.markBlockHidden(pos);
            player.sendBlockChange(pos.toLocation(world), newBlockData);
        }
    }

    public PlayerState getPlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUniqueId(), PlayerState::new);
    }

    public void removePlayer(Player player) {
        PlayerState state = playerStates.remove(player.getUniqueId());
        if (state != null) {
            state.clear();
        }
    }

    /**
     * Restores all real blocks and shows all hidden entities for all players (e.g. on disable).
     */
    public void restoreAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            restorePlayer(player);
        }
        playerStates.clear();
    }

    public void restorePlayer(Player player) {
        PlayerState state = playerStates.get(player.getUniqueId());
        if (state == null) return;

        World world = player.getWorld();

        // Restore real block appearances
        for (BlockPos pos : state.getRevealedBlocks()) {
            StorageChunk sc = storageManager.getStorageChunk(world, pos.getChunkX(), pos.getChunkZ());
            if (sc != null) {
                StorageBlock block = sc.getBlock(pos);
                if (block != null) {
                    player.sendBlockChange(pos.toLocation(world), block.getOriginalBlockData());
                }
            }
        }

        // Show all hidden entities
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                StorageChunk sc = storageManager.getStorageChunk(c);
                if (sc != null) {
                    for (UUID eid : sc.getEntities()) {
                        Entity entity = Bukkit.getEntity(eid);
                        if (entity != null && entity.isValid()) {
                            player.showEntity(plugin, entity);
                        }
                    }
                }
            }
        }

        state.clear();
    }
}

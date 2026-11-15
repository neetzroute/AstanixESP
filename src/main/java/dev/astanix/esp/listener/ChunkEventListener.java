package dev.astanix.esp.listener;

import dev.astanix.esp.manager.StorageManager;
import dev.astanix.esp.tracker.PlayerTrackerService;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Listens to chunk lifecycle and chunk packet dispatch events.
 */
public class ChunkEventListener implements Listener {
    private final StorageManager storageManager;
    private final PlayerTrackerService trackerService;

    public ChunkEventListener(StorageManager storageManager, PlayerTrackerService trackerService) {
        this.storageManager = storageManager;
        this.trackerService = trackerService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        storageManager.loadChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        storageManager.unloadChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        trackerService.onPlayerChunkLoad(event.getPlayer(), event.getChunk());
    }
}

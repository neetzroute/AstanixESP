package dev.astanix.esp.listener;

import dev.astanix.esp.freecam.ViolationManager;
import dev.astanix.esp.freecam.WallOcclusionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player lifecycle events for FreeCam protection.
 */
public class FreeCamMoveListener implements Listener {
    private final ViolationManager violationManager;
    private final WallOcclusionService wallOcclusionService;

    public FreeCamMoveListener(ViolationManager violationManager, WallOcclusionService wallOcclusionService) {
        this.violationManager = violationManager;
        this.wallOcclusionService = wallOcclusionService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        violationManager.removeProfile(player);
        wallOcclusionService.restorePlayer(player);
    }
}

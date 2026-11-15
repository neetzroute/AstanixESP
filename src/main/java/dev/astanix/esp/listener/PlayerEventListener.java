package dev.astanix.esp.listener;

import dev.astanix.esp.AstanixESPPlugin;
import dev.astanix.esp.model.PlayerState;
import dev.astanix.esp.tracker.PlayerTrackerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

/**
 * Listens to player lifecycle and transit events (join, quit, teleport, world change, respawn).
 */
public class PlayerEventListener implements Listener {
    private final AstanixESPPlugin plugin;
    private final PlayerTrackerService trackerService;

    public PlayerEventListener(AstanixESPPlugin plugin, PlayerTrackerService trackerService) {
        this.plugin = plugin;
        this.trackerService = trackerService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                trackerService.checkPlayer(player, true);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        trackerService.removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                trackerService.checkPlayer(player, true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PlayerState state = trackerService.getPlayerState(player);
        state.clear();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                trackerService.checkPlayer(player, true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                trackerService.checkPlayer(player, true);
            }
        }, 1L);
    }
}

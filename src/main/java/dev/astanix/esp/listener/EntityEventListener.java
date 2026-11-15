package dev.astanix.esp.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import dev.astanix.esp.AstanixESPPlugin;
import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.manager.StorageManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Listens to entity lifecycle events (spawn, death, removal from world).
 */
public class EntityEventListener implements Listener {
    private final AstanixESPPlugin plugin;
    private final PluginConfig config;
    private final StorageManager storageManager;

    public EntityEventListener(AstanixESPPlugin plugin, PluginConfig config, StorageManager storageManager) {
        this.plugin = plugin;
        this.config = config;
        this.storageManager = storageManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (config.isTargetEntity(entity.getType())) {
            storageManager.registerEntity(entity);

            // Hide from distant players immediately
            Location loc = entity.getLocation();
            World world = loc.getWorld();
            if (world != null) {
                double hideDistSq = config.getHideDistanceSq();
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distanceSquared(loc) > hideDistSq) {
                        player.hideEntity(plugin, entity);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (config.isTargetEntity(entity.getType())) {
            storageManager.unregisterEntity(entity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (config.isTargetEntity(entity.getType())) {
            storageManager.unregisterEntity(entity);
        }
    }
}

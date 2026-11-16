package dev.astanix.esp.listener;

import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.freecam.RaytraceValidator;
import dev.astanix.esp.freecam.ReachValidator;
import dev.astanix.esp.freecam.ViolationManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Validates container inventory openings against FreeCam wall-peeking and remote access.
 * Silently denies openings without direct line of sight or beyond reach.
 */
public class FreeCamInventoryListener implements Listener {
    private final PluginConfig config;
    private final ReachValidator reachValidator;
    private final RaytraceValidator raytraceValidator;
    private final ViolationManager violationManager;

    public FreeCamInventoryListener(PluginConfig config, ReachValidator reachValidator,
                                    RaytraceValidator raytraceValidator,
                                    ViolationManager violationManager) {
        this.config = config;
        this.reachValidator = reachValidator;
        this.raytraceValidator = raytraceValidator;
        this.violationManager = violationManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!config.isFreeCamCheckInventoryOpen()) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (violationManager.hasBypass(player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) return;

        if (holder instanceof Container container) {
            validateContainer(player, container.getBlock(), event);
        } else if (holder instanceof DoubleChest doubleChest) {
            Location loc = doubleChest.getLocation();
            if (loc != null) {
                validateContainer(player, loc.getBlock(), event);
            }
        } else if (holder instanceof Entity entity) {
            double[] dist = new double[1];
            if (!reachValidator.checkEntityReach(player, entity, dist) ||
                    !raytraceValidator.hasLineOfSight(player, entity)) {
                event.setCancelled(true);
            }
        }
    }

    private void validateContainer(Player player, Block block, InventoryOpenEvent event) {
        double[] dist = new double[1];
        if (!reachValidator.checkBlockReach(player, block, dist) ||
                !raytraceValidator.hasLineOfSight(player, block, null)) {
            event.setCancelled(true);
        }
    }
}

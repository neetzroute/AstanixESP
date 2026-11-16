package dev.astanix.esp.listener;

import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.freecam.RaytraceValidator;
import dev.astanix.esp.freecam.ReachValidator;
import dev.astanix.esp.freecam.ViolationManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;

/**
 * Validates block breaking and damage actions against wall occlusion and reach cheats.
 * Silently cancels illegal break attempts.
 */
public class FreeCamBreakListener implements Listener {
    private final PluginConfig config;
    private final ReachValidator reachValidator;
    private final RaytraceValidator raytraceValidator;
    private final ViolationManager violationManager;

    public FreeCamBreakListener(PluginConfig config, ReachValidator reachValidator,
                                RaytraceValidator raytraceValidator,
                                ViolationManager violationManager) {
        this.config = config;
        this.reachValidator = reachValidator;
        this.raytraceValidator = raytraceValidator;
        this.violationManager = violationManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent event) {
        if (!config.isFreeCamCheckBreaking()) return;

        Player player = event.getPlayer();
        if (violationManager.hasBypass(player)) return;

        Block block = event.getBlock();
        double[] distance = new double[1];

        if (!reachValidator.checkBlockReach(player, block, distance)) {
            event.setCancelled(true);
            return;
        }

        if (!raytraceValidator.hasLineOfSight(player, block, null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!config.isFreeCamCheckBreaking()) return;

        Player player = event.getPlayer();
        if (violationManager.hasBypass(player)) return;

        Block block = event.getBlock();
        double[] distance = new double[1];

        if (!reachValidator.checkBlockReach(player, block, distance)) {
            event.setCancelled(true);
            return;
        }

        if (!raytraceValidator.hasLineOfSight(player, block, null)) {
            event.setCancelled(true);
        }
    }
}

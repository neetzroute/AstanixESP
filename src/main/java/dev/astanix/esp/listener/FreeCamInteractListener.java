package dev.astanix.esp.listener;

import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.freecam.RaytraceValidator;
import dev.astanix.esp.freecam.ReachValidator;
import dev.astanix.esp.freecam.ViolationManager;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;

/**
 * Validates block interactions, entity clicks, air clicks and arm swings against FreeCam.
 * Silently blocks invalid interactions without chat alerts.
 */
public class FreeCamInteractListener implements Listener {
    private final PluginConfig config;
    private final ReachValidator reachValidator;
    private final RaytraceValidator raytraceValidator;
    private final ViolationManager violationManager;

    public FreeCamInteractListener(PluginConfig config, ReachValidator reachValidator,
                                   RaytraceValidator raytraceValidator,
                                   ViolationManager violationManager) {
        this.config = config;
        this.reachValidator = reachValidator;
        this.raytraceValidator = raytraceValidator;
        this.violationManager = violationManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (violationManager.hasBypass(player)) return;

        Action action = event.getAction();

        // 1. Direct Block Interaction
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            validateBlockInteraction(player, block, event.getBlockFace(), event);
            return;
        }

        // 2. Air Click validation (FreeCam remote clicking beyond reach)
        if (config.isFreeCamCheckAirClicks() && (action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR)) {
            RayTraceResult trace = player.getWorld().rayTraceBlocks(
                    player.getEyeLocation(),
                    player.getLocation().getDirection(),
                    30.0,
                    FluidCollisionMode.NEVER,
                    true
            );

            if (trace != null && trace.getHitBlock() != null) {
                Block targeted = trace.getHitBlock();
                if (isInteractiveBlock(targeted.getType())) {
                    double dist = player.getEyeLocation().distance(targeted.getLocation().add(0.5, 0.5, 0.5));
                    if (dist > config.getFreeCamMaxReach(player.getGameMode())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private void validateBlockInteraction(Player player, Block block, BlockFace face, PlayerInteractEvent event) {
        double[] distance = new double[1];
        if (!reachValidator.checkBlockReach(player, block, distance)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        if (config.isFreeCamCheckBlocks()) {
            if (!raytraceValidator.hasLineOfSight(player, block, face)) {
                event.setCancelled(true);
                event.setUseInteractedBlock(Event.Result.DENY);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!config.isFreeCamCheckEntities()) return;

        Player player = event.getPlayer();
        if (violationManager.hasBypass(player)) return;

        Entity entity = event.getRightClicked();
        double[] distance = new double[1];
        if (!reachValidator.checkEntityReach(player, entity, distance)) {
            event.setCancelled(true);
            return;
        }

        if (!raytraceValidator.hasLineOfSight(player, entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (!config.isFreeCamCheckAirClicks()) return;
        Player player = event.getPlayer();
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (violationManager.hasBypass(player)) return;

        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock != null && isInteractiveBlock(targetBlock.getType())) {
            double dist = player.getEyeLocation().distance(targetBlock.getLocation().add(0.5, 0.5, 0.5));
            if (dist > config.getFreeCamMaxReach(player.getGameMode()) ||
                    !raytraceValidator.hasLineOfSight(player, targetBlock, null)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isInteractiveBlock(Material material) {
        String name = material.name();
        return name.contains("CHEST") || name.contains("SHULKER") || name.contains("BARREL") ||
                name.contains("FURNACE") || name.contains("DOOR") || name.contains("BUTTON") ||
                name.contains("LEVER") || name.contains("GATE") || name.contains("HOPPER") ||
                name.contains("DISPENSER") || name.contains("DROPPER") || name.contains("CRAFTER");
    }
}

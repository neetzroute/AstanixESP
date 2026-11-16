package dev.astanix.esp.freecam;

import dev.astanix.esp.config.PluginConfig;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Heuristic helper for FreeCam context.
 */
public class FreeCamHeuristics {
    private final PluginConfig config;
    private final ViolationManager violationManager;

    public FreeCamHeuristics(PluginConfig config, ViolationManager violationManager) {
        this.config = config;
        this.violationManager = violationManager;
    }

    public double evaluateContextualWeight(Block targetBlock, Player player) {
        return 1.0;
    }
}

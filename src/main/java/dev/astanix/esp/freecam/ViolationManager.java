package dev.astanix.esp.freecam;

import dev.astanix.esp.AstanixESPPlugin;
import dev.astanix.esp.config.PluginConfig;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages player profiles and bypass permissions for FreeCam protection.
 * Alerts are disabled to keep chest hiding completely silent and seamless.
 */
public class ViolationManager {
    private final AstanixESPPlugin plugin;
    private final PluginConfig config;
    private final Logger logger;

    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    public ViolationManager(AstanixESPPlugin plugin, PluginConfig config, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
    }

    public void start() {
        // No background alert decay needed when alerts are disabled
    }

    public void stop() {
        profiles.clear();
    }

    public PlayerProfile getProfile(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), PlayerProfile::new);
    }

    public void removeProfile(Player player) {
        profiles.remove(player.getUniqueId());
    }

    public boolean hasBypass(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile.isBypass() && player.hasPermission("astanix.freecam.bypass");
    }
}

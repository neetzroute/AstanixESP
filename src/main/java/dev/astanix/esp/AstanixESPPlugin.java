package dev.astanix.esp;

import dev.astanix.esp.command.EspCommand;
import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.disguise.DisguiseResolver;
import dev.astanix.esp.listener.BlockEventListener;
import dev.astanix.esp.listener.ChunkEventListener;
import dev.astanix.esp.listener.EntityEventListener;
import dev.astanix.esp.listener.PlayerEventListener;
import dev.astanix.esp.manager.StorageManager;
import dev.astanix.esp.tracker.PlayerTrackerService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Astanix ESP.
 * High-performance anti-storage ESP masking engine for Paper 1.21.x.
 */
public final class AstanixESPPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private DisguiseResolver disguiseResolver;
    private StorageManager storageManager;
    private PlayerTrackerService trackerService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(getConfig(), getLogger());
        this.disguiseResolver = new DisguiseResolver(pluginConfig);
        this.storageManager = new StorageManager(pluginConfig, disguiseResolver, getLogger());
        this.trackerService = new PlayerTrackerService(this, pluginConfig, storageManager, getLogger());

        // Register Event Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ChunkEventListener(storageManager, trackerService), this);
        pm.registerEvents(new BlockEventListener(pluginConfig, storageManager, trackerService), this);
        pm.registerEvents(new EntityEventListener(this, pluginConfig, storageManager), this);
        pm.registerEvents(new PlayerEventListener(this, trackerService), this);

        // Register Command
        PluginCommand cmd = getCommand("astanixesp");
        if (cmd != null) {
            EspCommand espCommand = new EspCommand(this);
            cmd.setExecutor(espCommand);
            cmd.setTabCompleter(espCommand);
        }

        // Index all currently loaded chunks across all worlds
        int loadedChunksCount = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                storageManager.loadChunk(chunk);
                loadedChunksCount++;
            }
        }

        // Start tracker ticker
        trackerService.start();

        // Initialize state for any already connected players (e.g. reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            trackerService.checkPlayer(player, true);
        }

        getLogger().info("=================================================");
        getLogger().info(" Astanix-ESP v" + getPluginMeta().getVersion() + " successfully enabled!");
        getLogger().info(" Anti-Storage ESP Protection Active");
        getLogger().info(" Masking distance: " + pluginConfig.getHideDistance() + " blocks");
        getLogger().info(" Indexed " + storageManager.getTotalBlockCount() + " storage blocks across " + loadedChunksCount + " loaded chunks");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        // Stop background ticker
        if (trackerService != null) {
            trackerService.stop();
            // Restore all masked blocks and show hidden entities for online players
            trackerService.restoreAll();
        }

        if (storageManager != null) {
            storageManager.clearAll();
        }

        getLogger().info("=================================================");
        getLogger().info(" Astanix-ESP has been safely disabled. All blocks restored.");
        getLogger().info("=================================================");
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public DisguiseResolver getDisguiseResolver() {
        return disguiseResolver;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public PlayerTrackerService getTrackerService() {
        return trackerService;
    }
}

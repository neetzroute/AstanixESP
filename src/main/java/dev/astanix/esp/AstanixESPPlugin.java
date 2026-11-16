package dev.astanix.esp;

import dev.astanix.esp.command.EspCommand;
import dev.astanix.esp.command.FreeCamCommand;
import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.disguise.DisguiseResolver;
import dev.astanix.esp.freecam.*;
import dev.astanix.esp.listener.*;
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
 * AstanixESP - High-performance Anti-Storage ESP & Anti-FreeCam Security Plugin for Paper 1.21.x.
 * Operates in silent mode: disguises storage blocks seamlessly and prevents wall hacking without alert spam.
 */
public final class AstanixESPPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private DisguiseResolver disguiseResolver;
    private StorageManager storageManager;
    private PlayerTrackerService trackerService;

    // FreeCam Components
    private ViolationManager violationManager;
    private ReachValidator reachValidator;
    private RaytraceValidator raytraceValidator;
    private FreeCamHeuristics heuristics;
    private WallOcclusionService wallOcclusionService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(getConfig(), getLogger());
        this.disguiseResolver = new DisguiseResolver(pluginConfig);
        this.storageManager = new StorageManager(pluginConfig, disguiseResolver, getLogger());
        this.trackerService = new PlayerTrackerService(this, pluginConfig, storageManager, getLogger());

        // Initialize FreeCam components
        this.violationManager = new ViolationManager(this, pluginConfig, getLogger());
        this.reachValidator = new ReachValidator(pluginConfig);
        this.raytraceValidator = new RaytraceValidator(pluginConfig);
        this.heuristics = new FreeCamHeuristics(pluginConfig, violationManager);
        this.wallOcclusionService = new WallOcclusionService(this, pluginConfig, storageManager, raytraceValidator, violationManager);

        // Register Event Listeners
        PluginManager pm = getServer().getPluginManager();

        // ESP Listeners
        pm.registerEvents(new ChunkEventListener(storageManager, trackerService), this);
        pm.registerEvents(new BlockEventListener(pluginConfig, storageManager, trackerService), this);
        pm.registerEvents(new EntityEventListener(this, pluginConfig, storageManager), this);
        pm.registerEvents(new PlayerEventListener(this, trackerService), this);

        // FreeCam Listeners (Silent Denial)
        pm.registerEvents(new FreeCamInteractListener(pluginConfig, reachValidator, raytraceValidator, violationManager), this);
        pm.registerEvents(new FreeCamBreakListener(pluginConfig, reachValidator, raytraceValidator, violationManager), this);
        pm.registerEvents(new FreeCamInventoryListener(pluginConfig, reachValidator, raytraceValidator, violationManager), this);
        pm.registerEvents(new FreeCamMoveListener(violationManager, wallOcclusionService), this);

        // Register Commands
        PluginCommand espCmd = getCommand("astanixesp");
        if (espCmd != null) {
            EspCommand espCommand = new EspCommand(this);
            espCmd.setExecutor(espCommand);
            espCmd.setTabCompleter(espCommand);
        }

        PluginCommand afcCmd = getCommand("afc");
        if (afcCmd != null) {
            FreeCamCommand freeCamCommand = new FreeCamCommand(this);
            afcCmd.setExecutor(freeCamCommand);
            afcCmd.setTabCompleter(freeCamCommand);
        }

        // Index all currently loaded chunks across all worlds
        int loadedChunksCount = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                storageManager.loadChunk(chunk);
                loadedChunksCount++;
            }
        }

        // Start background tasks
        trackerService.start();
        violationManager.start();
        wallOcclusionService.start();

        // Initialize state for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            trackerService.checkPlayer(player, true);
        }

        getLogger().info("=================================================");
        getLogger().info(" Astanix-ESP v" + getPluginMeta().getVersion() + " successfully enabled!");
        getLogger().info(" Anti-Storage ESP & Wall-Occlusion Active (Silent Mode)");
        getLogger().info(" Masking distance: " + pluginConfig.getHideDistance() + " blocks");
        getLogger().info(" Indexed " + storageManager.getTotalBlockCount() + " storage blocks across " + loadedChunksCount + " loaded chunks");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        if (wallOcclusionService != null) {
            wallOcclusionService.stop();
        }

        if (violationManager != null) {
            violationManager.stop();
        }

        if (trackerService != null) {
            trackerService.stop();
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

    public ViolationManager getViolationManager() {
        return violationManager;
    }

    public ReachValidator getReachValidator() {
        return reachValidator;
    }

    public RaytraceValidator getRaytraceValidator() {
        return raytraceValidator;
    }

    public FreeCamHeuristics getHeuristics() {
        return heuristics;
    }

    public WallOcclusionService getWallOcclusionService() {
        return wallOcclusionService;
    }
}

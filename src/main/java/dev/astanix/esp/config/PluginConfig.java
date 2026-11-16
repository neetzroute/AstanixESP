package dev.astanix.esp.config;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Handles reading, validating, and caching of all plugin configuration settings.
 * Operates in silent mode: no alert broadcasts or sound spam.
 */
public class PluginConfig {

    private final Logger logger;

    // Core ESP settings
    private double hideDistance;
    private double hideDistanceSquared;
    private long checkIntervalTicks;
    private int maxDownwardSearch;
    private boolean debug;

    // Fallback disguises
    private Material fallbackOverworld;
    private Material fallbackDeepslate;
    private Material fallbackNether;
    private Material fallbackTheEnd;

    // Target materials and entities to disguise
    private final Set<Material> targetBlocks = new HashSet<>();
    private final Set<EntityType> targetEntities = new HashSet<>();

    // FreeCam settings
    private boolean freeCamReachEnabled;
    private double freeCamReachSurvivalMax;
    private double freeCamReachCreativeMax;
    private boolean freeCamRaytraceEnabled;
    private boolean freeCamCheckBlocks;
    private boolean freeCamCheckEntities;
    private boolean freeCamCheckBreaking;
    private boolean freeCamCheckAirClicks;
    private boolean freeCamCheckInventoryOpen;
    private boolean freeCamOcclusionEnabled;
    private double freeCamOcclusionRadius;
    private int freeCamMultiRaySamples;
    private final Set<Material> freeCamIgnoredBlocks = new HashSet<>();

    // Messages
    private String prefix;
    private String msgNoPermission;
    private String msgReloaded;
    private String msgBypassEnabled;
    private String msgBypassDisabled;
    private String msgPlayerNotFound;

    public PluginConfig(FileConfiguration config, Logger logger) {
        this.logger = logger;
        load(config);
    }

    public void load(FileConfiguration config) {
        // --- Core ESP ---
        this.hideDistance = Math.max(1.0, config.getDouble("hide-distance", 25.0));
        this.hideDistanceSquared = this.hideDistance * this.hideDistance;
        this.checkIntervalTicks = Math.max(1L, config.getLong("check-interval-ticks", 2L));
        this.maxDownwardSearch = Math.max(1, config.getInt("max-downward-search", 32));
        this.debug = config.getBoolean("debug", false);

        // Fallbacks
        this.fallbackOverworld = parseMaterial(config.getString("fallback-disguise.overworld", "STONE"), Material.STONE);
        this.fallbackDeepslate = parseMaterial(config.getString("fallback-disguise.overworld-deepslate", "DEEPSLATE"), Material.DEEPSLATE);
        this.fallbackNether = parseMaterial(config.getString("fallback-disguise.nether", "NETHERRACK"), Material.NETHERRACK);
        this.fallbackTheEnd = parseMaterial(config.getString("fallback-disguise.the-end", "END_STONE"), Material.END_STONE);

        // Target Blocks
        targetBlocks.clear();
        List<String> blockList = config.getStringList("target-blocks");
        for (String name : blockList) {
            Material mat = Material.matchMaterial(name.trim().toUpperCase());
            if (mat != null && mat.isBlock()) {
                targetBlocks.add(mat);
            }
        }

        // Target Entities
        targetEntities.clear();
        List<String> entityList = config.getStringList("target-entities");
        for (String name : entityList) {
            try {
                EntityType type = EntityType.valueOf(name.trim().toUpperCase());
                targetEntities.add(type);
            } catch (IllegalArgumentException ignored) {}
        }

        // --- FreeCam / Wall Occlusion ---
        this.freeCamReachEnabled = config.getBoolean("freecam.reach.enabled", true);
        this.freeCamReachSurvivalMax = Math.max(1.0, config.getDouble("freecam.reach.survival-max", 4.8));
        this.freeCamReachCreativeMax = Math.max(1.0, config.getDouble("freecam.reach.creative-max", 5.5));

        this.freeCamRaytraceEnabled = config.getBoolean("freecam.raytrace.enabled", true);
        this.freeCamCheckBlocks = config.getBoolean("freecam.raytrace.check-blocks", true);
        this.freeCamCheckEntities = config.getBoolean("freecam.raytrace.check-entities", true);
        this.freeCamCheckBreaking = config.getBoolean("freecam.raytrace.check-breaking", true);
        this.freeCamCheckAirClicks = config.getBoolean("freecam.raytrace.check-air-clicks", true);
        this.freeCamCheckInventoryOpen = config.getBoolean("freecam.raytrace.check-inventory-open", true);

        this.freeCamOcclusionEnabled = config.getBoolean("freecam.occlusion.enabled", true);
        this.freeCamOcclusionRadius = Math.max(4.0, config.getDouble("freecam.occlusion.radius", 18.0));
        this.freeCamMultiRaySamples = Math.clamp(config.getInt("freecam.raytrace.multi-ray-samples", 5), 1, 9);

        freeCamIgnoredBlocks.clear();
        List<String> ignoredList = config.getStringList("freecam.raytrace.ignored-blocks");
        for (String name : ignoredList) {
            Material mat = Material.matchMaterial(name.trim().toUpperCase());
            if (mat != null) {
                freeCamIgnoredBlocks.add(mat);
            }
        }

        // --- Messages ---
        this.prefix = config.getString("messages.prefix", "<gradient:#00D2FF:#3A7BD5><bold>[Astanix-Security]</bold></gradient> ");
        this.msgNoPermission = config.getString("messages.no-permission", "<red>У вас нет прав для выполнения этой команды!</red>");
        this.msgReloaded = config.getString("messages.reloaded", "<green>Конфигурация успешно перезагружена!</green>");
        this.msgBypassEnabled = config.getString("messages.bypass-enabled", "<yellow>Режим обхода FreeCam <green>включен</green> (проверки отключены).</yellow>");
        this.msgBypassDisabled = config.getString("messages.bypass-disabled", "<yellow>Режим обхода FreeCam <red>выключен</red> (проверки активны).</yellow>");
        this.msgPlayerNotFound = config.getString("messages.player-not-found", "<red>Игрок не найден в сети.</red>");

        if (debug) {
            logger.info("Configuration loaded: HideDist=" + hideDistance + ", Targets=" + targetBlocks.size() +
                    ", Occlusion=" + freeCamOcclusionEnabled + ", Radius=" + freeCamOcclusionRadius);
        }
    }

    private Material parseMaterial(String name, Material def) {
        if (name == null) return def;
        Material mat = Material.matchMaterial(name.trim().toUpperCase());
        return (mat != null && mat.isBlock()) ? mat : def;
    }

    public double getHideDistance() {
        return hideDistance;
    }

    public double getHideDistanceSquared() {
        return hideDistanceSquared;
    }

    public double getHideDistanceSq() {
        return hideDistanceSquared;
    }

    public long getCheckIntervalTicks() {
        return checkIntervalTicks;
    }

    public int getMaxDownwardSearch() {
        return maxDownwardSearch;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Material getFallbackOverworld() {
        return fallbackOverworld;
    }

    public Material getFallbackDeepslate() {
        return fallbackDeepslate;
    }

    public Material getFallbackOverworldDeepslate() {
        return fallbackDeepslate;
    }

    public Material getFallbackNether() {
        return fallbackNether;
    }

    public Material getFallbackTheEnd() {
        return fallbackTheEnd;
    }

    public boolean isTargetMaterial(Material material) {
        return targetBlocks.contains(material);
    }

    public boolean isTargetEntity(EntityType entityType) {
        return targetEntities.contains(entityType);
    }

    public Set<Material> getTargetBlocks() {
        return Collections.unmodifiableSet(targetBlocks);
    }

    public Set<EntityType> getTargetEntities() {
        return Collections.unmodifiableSet(targetEntities);
    }

    public boolean isFreeCamReachEnabled() {
        return freeCamReachEnabled;
    }

    public double getFreeCamMaxReach(GameMode gameMode) {
        return (gameMode == GameMode.CREATIVE) ? freeCamReachCreativeMax : freeCamReachSurvivalMax;
    }

    public boolean isFreeCamRaytraceEnabled() {
        return freeCamRaytraceEnabled;
    }

    public boolean isFreeCamCheckBlocks() {
        return freeCamCheckBlocks;
    }

    public boolean isFreeCamCheckEntities() {
        return freeCamCheckEntities;
    }

    public boolean isFreeCamCheckBreaking() {
        return freeCamCheckBreaking;
    }

    public boolean isFreeCamCheckAirClicks() {
        return freeCamCheckAirClicks;
    }

    public boolean isFreeCamCheckInventoryOpen() {
        return freeCamCheckInventoryOpen;
    }

    public boolean isFreeCamOcclusionEnabled() {
        return freeCamOcclusionEnabled;
    }

    public double getFreeCamOcclusionRadius() {
        return freeCamOcclusionRadius;
    }

    public int getFreeCamMultiRaySamples() {
        return freeCamMultiRaySamples;
    }

    public boolean isFreeCamIgnoredBlock(Material material) {
        return freeCamIgnoredBlocks.contains(material);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public String getMsgReloaded() {
        return msgReloaded;
    }

    public String getMsgBypassEnabled() {
        return msgBypassEnabled;
    }

    public String getMsgBypassDisabled() {
        return msgBypassDisabled;
    }

    public String getMsgPlayerNotFound() {
        return msgPlayerNotFound;
    }
}

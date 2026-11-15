package dev.astanix.esp.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Configuration holder and parser for Astanix ESP.
 */
public class PluginConfig {
    private final Logger logger;

    private double hideDistance;
    private double hideDistanceSq;
    private long checkIntervalTicks;
    private int maxDownwardSearch;

    private Material fallbackOverworld;
    private Material fallbackOverworldDeepslate;
    private Material fallbackNether;
    private Material fallbackTheEnd;

    private final Set<Material> targetBlocks = EnumSet.noneOf(Material.class);
    private final Set<EntityType> targetEntities = EnumSet.noneOf(EntityType.class);
    private boolean debug;

    public PluginConfig(FileConfiguration config, Logger logger) {
        this.logger = logger;
        load(config);
    }

    public void load(FileConfiguration config) {
        this.hideDistance = Math.max(1.0, config.getDouble("hide-distance", 25.0));
        this.hideDistanceSq = this.hideDistance * this.hideDistance;
        this.checkIntervalTicks = Math.max(1L, config.getLong("check-interval-ticks", 2L));
        this.maxDownwardSearch = Math.max(1, config.getInt("max-downward-search", 32));
        this.debug = config.getBoolean("debug", false);

        // Fallback materials
        this.fallbackOverworld = parseMaterial(config.getString("fallback-disguise.overworld", "STONE"), Material.STONE);
        this.fallbackOverworldDeepslate = parseMaterial(config.getString("fallback-disguise.overworld-deepslate", "DEEPSLATE"), Material.DEEPSLATE);
        this.fallbackNether = parseMaterial(config.getString("fallback-disguise.nether", "NETHERRACK"), Material.NETHERRACK);
        this.fallbackTheEnd = parseMaterial(config.getString("fallback-disguise.the-end", "END_STONE"), Material.END_STONE);

        // Target Blocks
        targetBlocks.clear();
        List<String> blockList = config.getStringList("target-blocks");
        for (String name : blockList) {
            try {
                Material mat = Material.matchMaterial(name.trim().toUpperCase());
                if (mat != null && mat.isBlock()) {
                    targetBlocks.add(mat);
                } else {
                    logger.warning("Unrecognized target block material in config: " + name);
                }
            } catch (Exception e) {
                logger.warning("Failed to parse material: " + name);
            }
        }

        // Target Entities
        targetEntities.clear();
        List<String> entityList = config.getStringList("target-entities");
        for (String name : entityList) {
            try {
                EntityType type = EntityType.valueOf(name.trim().toUpperCase());
                targetEntities.add(type);
            } catch (Exception e) {
                logger.warning("Unrecognized target entity type in config: " + name);
            }
        }

        if (debug) {
            logger.info("Loaded " + targetBlocks.size() + " target block types and " +
                    targetEntities.size() + " target entity types. Hide distance: " + hideDistance);
        }
    }

    private Material parseMaterial(String name, Material defaultMat) {
        if (name == null || name.isEmpty()) return defaultMat;
        Material mat = Material.matchMaterial(name.trim().toUpperCase());
        return (mat != null && mat.isBlock()) ? mat : defaultMat;
    }

    public boolean isTargetMaterial(Material material) {
        return material != null && targetBlocks.contains(material);
    }

    public boolean isTargetEntity(EntityType entityType) {
        return entityType != null && targetEntities.contains(entityType);
    }

    public double getHideDistance() {
        return hideDistance;
    }

    public double getHideDistanceSq() {
        return hideDistanceSq;
    }

    public long getCheckIntervalTicks() {
        return checkIntervalTicks;
    }

    public int getMaxDownwardSearch() {
        return maxDownwardSearch;
    }

    public Material getFallbackOverworld() {
        return fallbackOverworld;
    }

    public Material getFallbackOverworldDeepslate() {
        return fallbackOverworldDeepslate;
    }

    public Material getFallbackNether() {
        return fallbackNether;
    }

    public Material getFallbackTheEnd() {
        return fallbackTheEnd;
    }

    public Set<Material> getTargetBlocks() {
        return Collections.unmodifiableSet(targetBlocks);
    }

    public Set<EntityType> getTargetEntities() {
        return Collections.unmodifiableSet(targetEntities);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}

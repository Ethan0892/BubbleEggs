package com.bubbleeggs.managers;

import com.bubbleeggs.BubbleEggs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class ConfigManager {
    
    private final BubbleEggs plugin;
    private FileConfiguration config;
    private FileConfiguration mobsConfig;
    private FileConfiguration langConfig;
    
    private File configFile;
    private File mobsFile;
    private File langFile;
    
    public ConfigManager(BubbleEggs plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        loadMainConfig();
        loadMobsConfig();
        loadLanguageConfig();
    }
    
    private void loadMainConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        config = plugin.getConfig();
        
        // Update config if needed
        updateConfig();
    }
    
    private void loadMobsConfig() {
        mobsFile = new File(plugin.getDataFolder(), "mobs.yml");
        if (!mobsFile.exists()) {
            plugin.saveResource("mobs.yml", false);
        }
        mobsConfig = YamlConfiguration.loadConfiguration(mobsFile);
        
        // Load defaults from jar
        InputStream defaultStream = plugin.getResource("mobs.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            mobsConfig.setDefaults(defaultConfig);
        }
    }
    
    private void loadLanguageConfig() {
        String language = config.getString("settings.language", "en");
        langFile = new File(plugin.getDataFolder(), "lang" + File.separator + language + ".yml");
        
        // Create lang directory if it doesn't exist
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // Only extract the lang file if it doesn't exist — never overwrite user customizations
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + language + ".yml", false);
        }

        // Try to load the user's file; if it has invalid YAML, fall back gracefully
        langConfig = new YamlConfiguration();
        try {
            langConfig.load(langFile);
        } catch (Exception e) {
            plugin.getLogger().warning("lang/" + language + ".yml could not be parsed (" + e.getMessage()
                    + "). Using built-in defaults — please fix the file to restore your customizations.");
        }

        // Load defaults from jar so new/missing keys are always available after updates
        InputStream defaultStream = plugin.getResource("lang/" + language + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultConfig);
        }
    }
    
    private void updateConfig() {
        // Check if config needs updating by comparing versions or missing keys
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            
            boolean needsUpdate = false;
            Set<String> defaultKeys = defaultConfig.getKeys(true);
            
            for (String key : defaultKeys) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                    needsUpdate = true;
                }
            }
            
            if (needsUpdate) {
                saveMainConfig();
                plugin.getLogger().info("Config file updated with new options!");
            }
        }
    }
    
    public void reloadConfigs() {
        plugin.reloadConfig();
        loadConfigs();
    }
    
    public void saveMainConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }
    
    public void saveMobsConfig() {
        try {
            mobsConfig.save(mobsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mobs.yml: " + e.getMessage());
        }
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getMobsConfig() {
        return mobsConfig;
    }
    
    public FileConfiguration getLangConfig() {
        return langConfig;
    }
    
    // Convenience methods for common config access
    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }
    
    public String getCatchCapsuleMaterial() {
        return config.getString("catch-capsule.material", "SNOWBALL");
    }
    
    public String getCatchCapsuleName() {
        return config.getString("catch-capsule.name", "&#FF6B6B&lCatch Capsule");
    }
    
    public double getDefaultCatchChance() {
        return config.getDouble("catching.default-catch-chance", 0.5);
    }
    
    public boolean isEconomyEnabled() {
        return config.getBoolean("economy.enabled", false);
    }
    
    public boolean isItemCostEnabled() {
        return config.getBoolean("item-cost.enabled", false);
    }
    
    public boolean areParticlesEnabled() {
        return config.getBoolean("effects.particles.enabled", true);
    }
    
    public boolean areSoundsEnabled() {
        return config.getBoolean("effects.sounds.enabled", true);
    }
    
    public boolean isWorldGuardEnabled() {
        return config.getBoolean("worldguard.enabled", true);
    }
    
    public boolean isMobDataSavingEnabled() {
        return config.getBoolean("spawn-eggs.save-mob-data", true);
    }
    
    public int getCooldown() {
        return config.getInt("catching.cooldown", 5);
    }
    
    public double getMaxCatchDistance() {
        return config.getDouble("advanced.max-catch-distance", 10.0);
    }
    
    public double getMinCatchDistance() {
        return config.getDouble("advanced.min-catch-distance", 1.0);
    }
    
    // Mob-specific methods
    public boolean isMobEnabled(String mobType) {
        return mobsConfig.getBoolean(mobType + ".enabled", true);
    }
    
    public double getMobCatchChance(String mobType) {
        return mobsConfig.getDouble(mobType + ".catch-chance", getDefaultCatchChance());
    }
    
    public double getMobMoneyCost(String mobType) {
        return mobsConfig.getDouble(mobType + ".money-cost", 0.0);
    }
    
    public String getMobItemCost(String mobType) {
        return mobsConfig.getString(mobType + ".item-cost", "");
    }
    
    public double getMobMaxHealthPercentage(String mobType) {
        return mobsConfig.getDouble(mobType + ".max-health-percentage", 1.0);
    }

    public boolean getMobAllowsSpawnerChange(String mobType) {
        return mobsConfig.getBoolean(mobType + ".spawner-enabled", true);
    }

    // Per-world config helpers (fall back to global values if the world section is absent)

    public int getWorldCooldown(String worldName) {
        if (config.contains("catching.worlds." + worldName + ".cooldown")) {
            return config.getInt("catching.worlds." + worldName + ".cooldown");
        }
        return getCooldown();
    }

    public double getWorldCatchMultiplier(String worldName) {
        if (config.contains("catching.worlds." + worldName + ".chance-multiplier")) {
            return config.getDouble("catching.worlds." + worldName + ".chance-multiplier", 1.0);
        }
        return 1.0;
    }

    public double getWorldRareThreshold(String worldName) {
        if (config.contains("catching.worlds." + worldName + ".rare-threshold")) {
            return config.getDouble("catching.worlds." + worldName + ".rare-threshold");
        }
        return config.getDouble("catching.rare-threshold", 0.1);
    }

    public int getWorldXpCostLevels(String worldName) {
        if (config.contains("catching.worlds." + worldName + ".xp-cost-levels")) {
            return config.getInt("catching.worlds." + worldName + ".xp-cost-levels");
        }
        return config.getInt("catching.xp-cost-levels", 0);
    }

    public boolean isWorldEnabled(String worldName) {
        if (config.contains("catching.worlds." + worldName + ".enabled")) {
            return config.getBoolean("catching.worlds." + worldName + ".enabled", true);
        }
        List<String> disabledWorlds = config.getStringList("catching.disabled-worlds");
        return !disabledWorlds.contains(worldName);
    }
    
    public void setMobCatchChance(String mobType, double chance) {
        mobsConfig.set(mobType + ".catch-chance", chance);
    }
    
    public void setMobMoneyCost(String mobType, double cost) {
        mobsConfig.set(mobType + ".money-cost", cost);
    }
    
    public void setMobItemCost(String mobType, String cost) {
        mobsConfig.set(mobType + ".item-cost", cost);
    }
    
    // Language methods
    public String getMessage(String key) {
        return langConfig.getString(key, "Message not found: " + key);
    }
    
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }
}
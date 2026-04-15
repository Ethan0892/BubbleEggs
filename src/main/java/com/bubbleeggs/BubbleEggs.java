package com.bubbleeggs;

import com.bubbleeggs.commands.MainCommand;
import com.bubbleeggs.hooks.PlaceholderAPIHook;
import com.bubbleeggs.listeners.ProjectileListener;
import com.bubbleeggs.listeners.SpawnEggListener;
import com.bubbleeggs.listeners.SpawnerListener;
import com.bubbleeggs.managers.ConfigManager;
import com.bubbleeggs.managers.EconomyManager;
import com.bubbleeggs.managers.StatsManager;
import com.bubbleeggs.managers.WorldGuardManager;
import com.bubbleeggs.utils.ItemUtil;
import com.bubbleeggs.utils.DataStorageUtil;
import com.bubbleeggs.utils.MessageUtil;
import com.bubbleeggs.utils.NBTCompat;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BubbleEggs extends JavaPlugin {
    
    private static BubbleEggs instance;
    private ConfigManager configManager;
    private EconomyManager economyManager;
    private WorldGuardManager worldGuardManager;
    private StatsManager statsManager;
    private MessageUtil messageUtil;
    private ItemUtil itemUtil;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.economyManager = new EconomyManager(this);
        this.messageUtil = new MessageUtil(this);
        this.itemUtil = new ItemUtil(this);
        this.statsManager = new StatsManager(this);
        
        // Load configurations
        configManager.loadConfigs();
        
        // Setup economy (if Vault is available)
        economyManager.setupEconomy();
        
        // Setup WorldGuard (if available) - delayed initialization
        try {
            this.worldGuardManager = new WorldGuardManager(this);
            worldGuardManager.setupWorldGuard();
        } catch (NoClassDefFoundError e) {
            this.worldGuardManager = null;
            getLogger().info("WorldGuard not found! Region protection disabled.");
        }
        
        // Adjust command label if plugin.yml changed to bubbleeggs
        if (getCommand("bubbleeggs") != null) {
            getCommand("bubbleeggs").setExecutor(new MainCommand(this));
        } else if (getCommand("mte") != null) {
            getCommand("mte").setExecutor(new MainCommand(this));
        }
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ProjectileListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnEggListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);
        
        getLogger().info("BubbleEggs has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
        
        // Check for dependencies
        if (economyManager.isEconomyEnabled()) {
            getLogger().info("Vault detected! Economy features enabled.");
        }
        
        if (worldGuardManager != null && worldGuardManager.isWorldGuardEnabled()) {
            getLogger().info("WorldGuard detected! Region protection enabled.");
        }
        
        // Use NBTCompat detection
        NBTCompat.detect(this);
        if (NBTCompat.ENABLED) {
            getLogger().info("NBT compatibility enabled.");
        } else {
            getLogger().warning("NBT disabled. Reason: " + NBTCompat.DISABLE_REASON + ". Falling back to metadata-only mode.");
        }

        // Initialize unified data storage keys (PDC based)
        DataStorageUtil.init(this);

        // Register crafting recipes
        registerCraftingRecipes();

        // Register PlaceholderAPI hook if present
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new PlaceholderAPIHook(this).register();
                getLogger().info("PlaceholderAPI detected! Placeholders registered.");
            } catch (Exception e) {
                getLogger().warning("Failed to register PlaceholderAPI hook: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("BubbleEggs has been disabled!");
    }
    
    public static BubbleEggs getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }
    
    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
    
    public ItemUtil getItemUtil() {
        return itemUtil;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public void registerCraftingRecipes() {
        NamespacedKey key = new NamespacedKey(this, "catch_capsule");
        getServer().removeRecipe(key);

        if (!configManager.getConfig().getBoolean("crafting.catch-capsule.enabled", false)) {
            return;
        }

        ItemStack result = itemUtil.createCatchCapsule();
        result.setAmount(configManager.getConfig().getInt("crafting.catch-capsule.result-amount", 1));

        String type = configManager.getConfig().getString("crafting.catch-capsule.type", "SHAPED");
        try {
            if (type.equalsIgnoreCase("SHAPELESS")) {
                ShapelessRecipe recipe = new ShapelessRecipe(key, result);
                List<String> ingredients = configManager.getConfig().getStringList("crafting.catch-capsule.ingredients");
                for (String ing : ingredients) {
                    recipe.addIngredient(org.bukkit.Material.valueOf(ing.toUpperCase()));
                }
                getServer().addRecipe(recipe);
            } else {
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                List<String> shape = configManager.getConfig().getStringList("crafting.catch-capsule.shape");
                if (shape.size() >= 3) {
                    recipe.shape(shape.get(0), shape.get(1), shape.get(2));
                }
                ConfigurationSection ingredients = configManager.getConfig()
                        .getConfigurationSection("crafting.catch-capsule.ingredients");
                if (ingredients != null) {
                    for (String charKey : ingredients.getKeys(false)) {
                        recipe.setIngredient(charKey.charAt(0),
                                org.bukkit.Material.valueOf(ingredients.getString(charKey).toUpperCase()));
                    }
                }
                getServer().addRecipe(recipe);
            }
            getLogger().info("Catch Capsule crafting recipe registered.");
        } catch (Exception e) {
            getLogger().warning("Failed to register crafting recipe: " + e.getMessage());
        }
    }
}
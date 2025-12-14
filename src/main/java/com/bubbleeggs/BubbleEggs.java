package com.bubbleeggs;

import com.bubbleeggs.commands.MainCommand;
import com.bubbleeggs.listeners.ProjectileListener;
import com.bubbleeggs.listeners.SpawnEggListener;
import com.bubbleeggs.listeners.SpawnerListener;
import com.bubbleeggs.managers.ConfigManager;
import com.bubbleeggs.managers.EconomyManager;
import com.bubbleeggs.managers.WorldGuardManager;
import com.bubbleeggs.utils.ItemUtil;
import com.bubbleeggs.utils.DataStorageUtil;
import com.bubbleeggs.utils.MessageUtil;
import com.bubbleeggs.utils.NBTCompat;
import org.bukkit.plugin.java.JavaPlugin;

public class BubbleEggs extends JavaPlugin {
    
    private static BubbleEggs instance;
    private ConfigManager configManager;
    private EconomyManager economyManager;
    private WorldGuardManager worldGuardManager;
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
}
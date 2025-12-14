package com.bubbleeggs.managers;

import com.bubbleeggs.BubbleEggs;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardManager {
    
    private final BubbleEggs plugin;
    private boolean worldGuardEnabled = false;
    private BooleanFlag mobCatchingFlag;
    
    public WorldGuardManager(BubbleEggs plugin) {
        this.plugin = plugin;
    }
    
    public boolean setupWorldGuard() {
        if (!plugin.getConfigManager().isWorldGuardEnabled()) {
            plugin.getLogger().info("WorldGuard integration disabled in config.");
            return false;
        }
        
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            plugin.getLogger().info("WorldGuard not found! Region protection disabled.");
            return false;
        }
        
        try {
            // Register custom flag
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            String flagName = plugin.getConfigManager().getConfig().getString("worldguard.flag-name", "mob-catching");
            
            if (!flagName.isEmpty()) {
                try {
                    mobCatchingFlag = new BooleanFlag(flagName);
                    registry.register(mobCatchingFlag);
                    plugin.getLogger().info("Registered WorldGuard flag: " + flagName);
                } catch (FlagConflictException e) {
                    // Flag already exists, try to get it
                    mobCatchingFlag = (BooleanFlag) registry.get(flagName);
                    if (mobCatchingFlag != null) {
                        plugin.getLogger().info("Using existing WorldGuard flag: " + flagName);
                    } else {
                        plugin.getLogger().warning("Could not register or find WorldGuard flag: " + flagName);
                    }
                } catch (IllegalStateException e) {
                    // WorldGuard flags are frozen, try to get existing flag
                    mobCatchingFlag = (BooleanFlag) registry.get(flagName);
                    if (mobCatchingFlag != null) {
                        plugin.getLogger().info("Using existing WorldGuard flag: " + flagName);
                    } else {
                        plugin.getLogger().warning("WorldGuard flags are frozen and flag does not exist: " + flagName);
                    }
                }
            }
            
            worldGuardEnabled = true;
            plugin.getLogger().info("WorldGuard integration enabled.");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to setup WorldGuard integration: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
    
    public boolean canCatchInRegion(Player player, Location location) {
        if (!worldGuardEnabled) {
            return true;
        }
        
        // Check if player can bypass region restrictions
        boolean opsCanBypass = plugin.getConfigManager().getConfig().getBoolean("worldguard.ops-bypass", true);
        if (opsCanBypass && player.isOp()) {
            return true;
        }
        
        if (player.hasPermission("bubbleeggs.bypass.region")) {
            return true;
        }
        
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
            
            if (regionManager == null) {
                return getDefaultFlagValue();
            }
            
            ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            
            if (mobCatchingFlag != null) {
                // Check custom flag
                Boolean flagValue = regions.queryValue(WorldGuardPlugin.inst().wrapPlayer(player), mobCatchingFlag);
                if (flagValue != null) {
                    return flagValue;
                }
            }
            
            // Check if any regions deny building (fallback)
            if (!regions.testState(WorldGuardPlugin.inst().wrapPlayer(player), 
                com.sk89q.worldguard.protection.flags.Flags.BUILD)) {
                return false;
            }
            
            return getDefaultFlagValue();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard region: " + e.getMessage());
            return getDefaultFlagValue();
        }
    }
    
    private boolean getDefaultFlagValue() {
        return plugin.getConfigManager().getConfig().getBoolean("worldguard.default-flag-value", true);
    }
    
    public boolean canPlayerCatchInRegion(Player player, ProtectedRegion region) {
        if (!worldGuardEnabled || region == null) {
            return true;
        }
        
        try {
            if (mobCatchingFlag != null) {
                Boolean flagValue = region.getFlag(mobCatchingFlag);
                if (flagValue != null) {
                    return flagValue;
                }
            }
            
            return getDefaultFlagValue();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking region flag: " + e.getMessage());
            return getDefaultFlagValue();
        }
    }
}
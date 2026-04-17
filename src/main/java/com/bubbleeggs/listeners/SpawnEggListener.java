package com.bubbleeggs.listeners;

import com.bubbleeggs.BubbleEggs;
import com.bubbleeggs.managers.ConfigManager;
import com.bubbleeggs.utils.ItemUtil;
import com.bubbleeggs.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnEggListener implements Listener {
    
    private final BubbleEggs plugin;
    private final ConfigManager configManager;
    private final MessageUtil messageUtil;
    private final ItemUtil itemUtil;
    
    public SpawnEggListener(BubbleEggs plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtil = plugin.getMessageUtil();
        this.itemUtil = plugin.getItemUtil();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnEggUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !itemUtil.isBubbleEggsSpawnEgg(item)) {
            return;
        }
        
        // Check permission
        if (!player.hasPermission("bubbleeggs.spawn")) {
            messageUtil.sendLangMessage(player, "spawn-egg.no-permission-spawn");
            event.setCancelled(true);
            return;
        }
        
        // Get spawn location
        Location spawnLocation;
        if (event.getClickedBlock() != null) {
            spawnLocation = event.getClickedBlock().getLocation().add(0, 1, 0);
        } else {
            spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        }
        
        // Check if location is safe for spawning
        boolean enforceLocation = configManager.getConfig().getBoolean("spawn-eggs.enforce-valid-spawn-locations", true);
        if (enforceLocation && !isValidSpawnLocation(spawnLocation)) {
            messageUtil.sendLangMessage(player, "spawn-egg.invalid-location");
            event.setCancelled(true);
            return;
        }
        
        // Get mob data from spawn egg
        ItemUtil.MobData mobData = itemUtil.getMobDataFromSpawnEgg(item);
        if (mobData == null) {
            messageUtil.sendLangMessage(player, "error.mob-not-found", "%mob%", "unknown");
            event.setCancelled(true);
            return;
        }
        
        try {
            EntityType entityType = EntityType.valueOf(mobData.getMobType());
            
            // Spawn the mob
            LivingEntity spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, entityType);
            
            // Restore mob data
            if (configManager.isMobDataSavingEnabled()) {
                if (mobData.getMaxHealth() > 0) {
                    try { spawnedMob.setMaxHealth(mobData.getMaxHealth()); } catch (Throwable ignored) {}
                }
                if (mobData.getHealth() > 0) {
                    try { spawnedMob.setHealth(Math.min(mobData.getHealth(), spawnedMob.getMaxHealth())); } catch (Throwable ignored) {}
                }
                if (mobData.getMobName() != null) {
                    spawnedMob.setCustomName(mobData.getMobName());
                    spawnedMob.setCustomNameVisible(true);
                }
            }
            
            // Remove one spawn egg from player's hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            messageUtil.sendLangMessage(player, "spawn-egg.spawned", "%mob%", mobData.getMobType());
            event.setCancelled(true);
            
        } catch (IllegalArgumentException e) {
            messageUtil.sendLangMessage(player, "error.mob-not-found", "%mob%", mobData.getMobType());
            event.setCancelled(true);
        } catch (Exception e) {
            messageUtil.sendLangMessage(player, "spawn-egg.invalid-location");
            event.setCancelled(true);
        }
    }
    
    private boolean isValidSpawnLocation(Location location) {
        // Check if location is in a valid world
        if (location.getWorld() == null) {
            return false;
        }
        
        // Check if there's enough space (2 blocks high)
        if (!location.getBlock().isEmpty() || !location.clone().add(0, 1, 0).getBlock().isEmpty()) {
            return false;
        }
        
        // Check if there's solid ground below
        if (location.clone().add(0, -1, 0).getBlock().isEmpty()) {
            return false;
        }
        
        return true;
    }
}
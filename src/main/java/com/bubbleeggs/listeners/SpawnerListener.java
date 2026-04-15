package com.bubbleeggs.listeners;

import com.bubbleeggs.BubbleEggs;
import com.bubbleeggs.managers.ConfigManager;
import com.bubbleeggs.utils.ItemUtil;
import com.bubbleeggs.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerListener implements Listener {
    
    private final BubbleEggs plugin;
    private final ConfigManager configManager;
    private final MessageUtil messageUtil;
    private final ItemUtil itemUtil;
    
    public SpawnerListener(BubbleEggs plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtil = plugin.getMessageUtil();
        this.itemUtil = plugin.getItemUtil();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractWithSpawner(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !itemUtil.isBubbleEggsSpawnEgg(item)) {
            return;
        }
        
        // Check if spawner changing is enabled
        if (!configManager.getConfig().getBoolean("spawner.allow-spawner-change", true)) {
            messageUtil.sendLangMessage(player, "spawner.disabled");
            event.setCancelled(true);
            return;
        }
        
        // Check permission
        boolean requirePermission = configManager.getConfig().getBoolean("spawner.require-permission", true);
        String permission = configManager.getConfig().getString("spawner.permission", "bubbleeggs.spawner.change");
        
        if (requirePermission && !player.hasPermission(permission)) {
            messageUtil.sendLangMessage(player, "spawner.no-permission");
            event.setCancelled(true);
            return;
        }
        
        // Get mob type from spawn egg
        String mobType = itemUtil.getMobTypeFromSpawnEgg(item);
        if (mobType == null) {
            messageUtil.sendLangMessage(player, "error.mob-not-found", "%mob%", "unknown");
            event.setCancelled(true);
            return;
        }

        // Check if this specific mob type is allowed to change spawners
        if (!configManager.getMobAllowsSpawnerChange(mobType)) {
            messageUtil.sendLangMessage(player, "spawner.mob-disabled");
            event.setCancelled(true);
            return;
        }

        try {
            EntityType entityType = EntityType.valueOf(mobType);
            
            // Change spawner type
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            spawner.setSpawnedType(entityType);
            spawner.update();
            
            // Remove one spawn egg from player's hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            messageUtil.sendLangMessage(player, "spawner.changed", "%mob%", mobType);
            event.setCancelled(true);
            
        } catch (IllegalArgumentException e) {
            messageUtil.sendLangMessage(player, "error.mob-not-found", "%mob%", mobType);
            event.setCancelled(true);
        }
    }
}
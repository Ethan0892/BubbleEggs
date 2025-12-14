package com.bubbleeggs.listeners;

import com.bubbleeggs.BubbleEggs;
import com.bubbleeggs.managers.ConfigManager;
import com.bubbleeggs.utils.CatchUtil;
import com.bubbleeggs.utils.MessageUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class ProjectileListener implements Listener {
    
    private final BubbleEggs plugin;
    private final ConfigManager configManager;
    private final MessageUtil messageUtil;
    private final CatchUtil catchUtil;
    
    public ProjectileListener(BubbleEggs plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtil = plugin.getMessageUtil();
        this.catchUtil = new CatchUtil(plugin);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        
        // Check if projectile was launched by a player
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        
        Player player = (Player) projectile.getShooter();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        
        // Check if the item in hand is a catch capsule
        if (catchUtil.isCatchCapsule(handItem)) {
            // Mark the projectile as a catch capsule
            projectile.setMetadata("bubbleeggs.catch_capsule", new FixedMetadataValue(plugin, true));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        // Check if projectile was thrown by a player
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        
        Player player = (Player) projectile.getShooter();
        
        // Check if projectile is a catch capsule
        if (!catchUtil.isCatchCapsule(projectile)) {
            return;
        }
        
        // Check if projectile hit an entity
        Entity hitEntity = event.getHitEntity();
        if (hitEntity == null || !(hitEntity instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity mob = (LivingEntity) hitEntity;
        
        // Don't catch players
        if (mob instanceof Player) {
            return;
        }
        
        // Prevent projectile from causing damage
        event.setCancelled(true);
        
        // Attempt to catch the mob
        catchUtil.attemptCatch(player, mob, projectile.getLocation());
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Handle punch-catching
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        
        // Check if player is holding a catch capsule
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (!catchUtil.isCatchCapsule(handItem)) {
            return;
        }
        
        // Check if usage mode allows punching
        String usageMode = configManager.getConfig().getString("catch-capsule.usage-mode", "BOTH");
        if (!usageMode.equals("PUNCH") && !usageMode.equals("BOTH")) {
            return;
        }
        
        // Check if target is a valid mob
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity mob = (LivingEntity) event.getEntity();
        
        // Don't catch players
        if (mob instanceof Player) {
            return;
        }
        
        // Cancel the damage
        event.setCancelled(true);
        
        // Consume one catch capsule
        if (handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        
        // Attempt to catch the mob
        catchUtil.attemptCatch(player, mob, mob.getLocation());
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !catchUtil.isCatchCapsule(item)) {
            return;
        }
        
        // Check if usage mode allows throwing
        String usageMode = configManager.getConfig().getString("catch-capsule.usage-mode", "BOTH");
        if (!usageMode.equals("THROW") && !usageMode.equals("BOTH")) {
            event.setCancelled(true);
            return;
        }
        
        // Let the projectile be thrown normally - it will be handled by ProjectileHitEvent
    }
}
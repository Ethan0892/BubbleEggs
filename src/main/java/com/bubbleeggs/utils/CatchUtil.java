package com.bubbleeggs.utils;

import com.bubbleeggs.BubbleEggs;
import com.bubbleeggs.managers.ConfigManager;
import com.bubbleeggs.managers.EconomyManager;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CatchUtil {
    
    private final BubbleEggs plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final MessageUtil messageUtil;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    public CatchUtil(BubbleEggs plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.economyManager = plugin.getEconomyManager();
        this.messageUtil = plugin.getMessageUtil();
    }
    
    public boolean isCatchCapsule(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // PDC primary
        if (DataStorageUtil.isCatchCapsule(item)) return true;
        if (NBTCompat.ENABLED) {
            try {
                NBTItem nbtItem = new NBTItem(item);
                return nbtItem.getBoolean("bubbleeggs.catch_capsule");
            } catch (Throwable t) { NBTCompat.ENABLED = false; NBTCompat.DISABLE_REASON = t.getClass().getSimpleName(); }
        }
        
        String capsuleMaterial = configManager.getCatchCapsuleMaterial();
        String capsuleName = messageUtil.colorize(configManager.getCatchCapsuleName());
        
        if (!item.getType().toString().equals(capsuleMaterial)) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(capsuleName);
    }
    
    public boolean isCatchCapsule(Projectile projectile) {
        try {
            // Check if projectile has custom NBT data
            if (projectile.hasMetadata("bubbleeggs.catch_capsule")) {
                return projectile.getMetadata("bubbleeggs.catch_capsule").get(0).asBoolean();
            }
            return false;
        } catch (Exception e) {
            // Fallback method - check projectile type
            String capsuleMaterial = configManager.getCatchCapsuleMaterial();
            return projectile.getType().toString().contains(capsuleMaterial.split("_")[0]);
        }
    }
    
    public void attemptCatch(Player player, LivingEntity mob, Location location) {
        // Check basic permissions and conditions
        if (!canCatch(player, mob, location)) {
            return;
        }
        
        String mobType = mob.getType().toString();
        
        // Check mob-specific conditions
        if (!canCatchMob(player, mob, mobType)) {
            return;
        }
        
        // Check and handle costs
        if (!handleCosts(player, mobType)) {
            return;
        }
        
        // Calculate catch chance
        double catchChance = configManager.getMobCatchChance(mobType);
        boolean success = Math.random() < catchChance;
        
        if (success) {
            handleSuccessfulCatch(player, mob, mobType, location);
        } else {
            handleFailedCatch(player, mob, mobType, location, catchChance);
        }
        
        // Update cooldown
        updateCooldown(player);
    }
    
    private boolean canCatch(Player player, LivingEntity mob, Location location) {
        // Check permission
        if (!player.hasPermission("bubbleeggs.use")) {
            messageUtil.sendLangMessage(player, "catching.no-permission");
            return false;
        }
        
        // Check if catching is enabled globally
        if (!configManager.getConfig().getBoolean("catching.enabled-by-default", true)) {
            messageUtil.sendLangMessage(player, "catching.disabled");
            return false;
        }
        
        // Check world permissions
        if (!canCatchInWorld(player, location.getWorld())) {
            messageUtil.sendLangMessage(player, "catching.disabled-world");
            return false;
        }
        
        // Check region permissions (WorldGuard)
        if (!canCatchInRegion(player, location)) {
            messageUtil.sendLangMessage(player, "catching.disabled-region");
            return false;
        }
        
        // Check cooldown
        if (isOnCooldown(player)) {
            long remaining = getRemainingCooldown(player);
            messageUtil.sendLangMessage(player, "catching.cooldown", "%time%", String.valueOf(remaining));
            return false;
        }
        
        // Check distance
        double distance = player.getLocation().distance(location);
        double maxDistance = configManager.getMaxCatchDistance();
        double minDistance = configManager.getMinCatchDistance();
        
        if (distance > maxDistance) {
            messageUtil.sendLangMessage(player, "catching.too-far");
            return false;
        }
        
        if (distance < minDistance) {
            messageUtil.sendLangMessage(player, "catching.too-close");
            return false;
        }
        
        return true;
    }
    
    private boolean canCatchMob(Player player, LivingEntity mob, String mobType) {
        // Check if mob type is enabled
        if (!configManager.isMobEnabled(mobType)) {
            messageUtil.sendLangMessage(player, "catching.disabled-mob", "%mob%", mobType);
            return false;
        }
        
        // Check boss mobs
        if (isBossMob(mob) && !configManager.getConfig().getBoolean("catching.allow-boss-mobs", false)) {
            messageUtil.sendLangMessage(player, "catching.boss-mob");
            return false;
        }
        
        // Check named mobs
        if (mob.getCustomName() != null && !configManager.getConfig().getBoolean("catching.allow-named-mobs", true)) {
            messageUtil.sendLangMessage(player, "catching.named-mob");
            return false;
        }
        
        // Check tamed mobs
        if (isTamedMob(mob) && !configManager.getConfig().getBoolean("catching.allow-tamed-mobs", false)) {
            messageUtil.sendLangMessage(player, "catching.tamed-mob");
            return false;
        }
        
        // Check health percentage
        double healthPercentage = mob.getHealth() / mob.getMaxHealth();
        double maxHealthPercentage = configManager.getMobMaxHealthPercentage(mobType);
        
        if (healthPercentage > maxHealthPercentage) {
            messageUtil.sendLangMessage(player, "catching.too-healthy", "%max%", String.valueOf((int)(maxHealthPercentage * 100)));
            return false;
        }
        
        return true;
    }
    
    private boolean handleCosts(Player player, String mobType) {
        // Check economy cost
        if (configManager.isEconomyEnabled() && economyManager.isEconomyEnabled()) {
            double cost = configManager.getMobMoneyCost(mobType);
            if (cost > 0 && !player.hasPermission("bubbleeggs.bypass.cost")) {
                if (!economyManager.hasEnoughMoney(player, cost)) {
                    messageUtil.sendLangMessage(player, "economy.insufficient-funds", "%cost%", String.valueOf(cost));
                    return false;
                }
                economyManager.withdrawMoney(player, cost);
                messageUtil.sendLangMessage(player, "economy.cost-paid", "%cost%", String.valueOf(cost));
            }
        }
        
        // Check item cost
        if (configManager.isItemCostEnabled()) {
            String itemCost = configManager.getMobItemCost(mobType);
            if (!itemCost.isEmpty() && !player.hasPermission("bubbleeggs.bypass.cost")) {
                String[] parts = itemCost.split(":");
                if (parts.length == 2) {
                    try {
                        Material material = Material.valueOf(parts[0]);
                        int amount = Integer.parseInt(parts[1]);
                        
                        if (!hasEnoughItems(player, material, amount)) {
                            messageUtil.sendLangMessage(player, "item-cost.insufficient-items", 
                                "%amount%", String.valueOf(amount), "%item%", material.toString());
                            return false;
                        }
                        
                        removeItems(player, material, amount);
                        messageUtil.sendLangMessage(player, "item-cost.items-consumed", 
                            "%amount%", String.valueOf(amount), "%item%", material.toString());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid item cost format for " + mobType + ": " + itemCost);
                    }
                }
            }
        }
        
        return true;
    }
    
    private void handleSuccessfulCatch(Player player, LivingEntity mob, String mobType, Location location) {
        // Create spawn egg
        ItemStack spawnEgg = createSpawnEgg(mob, mobType);
        
        // Give spawn egg to player or drop it
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(spawnEgg);
        } else {
            location.getWorld().dropItemNaturally(location, spawnEgg);
            messageUtil.sendLangMessage(player, "catching.inventory-full");
        }
        
        // Remove the mob
        mob.remove();
        
        // Send success message
        messageUtil.sendLangMessage(player, "catching.success", "%mob%", mobType);
        
        // Play effects
        playSuccessEffects(player, location);
    }
    
    private void handleFailedCatch(Player player, LivingEntity mob, String mobType, Location location, double catchChance) {
        // Send failure message
        int chancePercent = (int) (catchChance * 100);
        messageUtil.sendLangMessage(player, "catching.failure", "%mob%", mobType, "%chance%", String.valueOf(chancePercent));
        
        // Play failure effects
        playFailureEffects(player, location);
    }
    
    private ItemStack createSpawnEgg(LivingEntity mob, String mobType) {
        // Get appropriate spawn egg material
        Material eggMaterial = getSpawnEggMaterial(mobType);
    ItemStack spawnEgg = new ItemStack(eggMaterial);
        
        // Set custom name and lore
        ItemMeta meta = spawnEgg.getItemMeta();
        if (meta != null) {
            String nameFormat = configManager.getConfig().getString("spawn-eggs.name-format", "&#FF6B6B%mob_name% &#FFFFFFSpawn Egg");
            String name = nameFormat.replace("%mob_name%", mobType);
            meta.setDisplayName(messageUtil.colorize(name));
            
            List<String> loreFormat = configManager.getConfig().getStringList("spawn-eggs.lore");
            if (!loreFormat.isEmpty()) {
                List<String> lore = messageUtil.colorizeLore(loreFormat);
                // Replace placeholders
                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    line = line.replace("%mob_name%", mobType);
                    line = line.replace("%mob_health%", String.valueOf((int) mob.getHealth()));
                    line = line.replace("%mob_max_health%", String.valueOf((int) mob.getMaxHealth()));
                    lore.set(i, line);
                }
                meta.setLore(lore);
            }
            
            // Set custom model data
            int customModelData = configManager.getConfig().getInt("spawn-eggs.custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            
            spawnEgg.setItemMeta(meta);
        }
        
        // Store mob data via PDC primary
        if (configManager.isMobDataSavingEnabled()) {
            spawnEgg = DataStorageUtil.storeMobData(spawnEgg, mob, mobType);
            if (NBTCompat.ENABLED) {
                try {
                    NBTItem nbtItem = new NBTItem(spawnEgg);
                    nbtItem.setString("bubbleeggs.mob_type", mobType);
                    nbtItem.setDouble("bubbleeggs.mob_health", mob.getHealth());
                    nbtItem.setDouble("bubbleeggs.mob_max_health", mob.getMaxHealth());
                    if (mob.getCustomName() != null) nbtItem.setString("bubbleeggs.mob_name", mob.getCustomName());
                    spawnEgg = nbtItem.getItem();
                } catch (Throwable t) { NBTCompat.ENABLED = false; NBTCompat.DISABLE_REASON = t.getClass().getSimpleName(); plugin.getLogger().info("Disabling mob data saving: " + t.getMessage()); }
            }
        }
        
        return spawnEgg;
    }
    
    private Material getSpawnEggMaterial(String mobType) {
        try {
            return Material.valueOf(mobType + "_SPAWN_EGG");
        } catch (IllegalArgumentException e) {
            // Fallback for mobs without spawn eggs
            return Material.CHICKEN_SPAWN_EGG;
        }
    }
    
    private void playSuccessEffects(Player player, Location location) {
        if (configManager.areParticlesEnabled()) {
            String particleType = configManager.getConfig().getString("effects.particles.success.type", "HAPPY_VILLAGER");
            int count = configManager.getConfig().getInt("effects.particles.success.count", 10);
            double speed = configManager.getConfig().getDouble("effects.particles.success.speed", 0.1);
            double offsetX = configManager.getConfig().getDouble("effects.particles.success.offset-x", 0.5);
            double offsetY = configManager.getConfig().getDouble("effects.particles.success.offset-y", 0.5);
            double offsetZ = configManager.getConfig().getDouble("effects.particles.success.offset-z", 0.5);
            
            try {
                Particle particle = Particle.valueOf(particleType);
                location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type: " + particleType);
            }
        }
        
        if (configManager.areSoundsEnabled()) {
            String soundType = configManager.getConfig().getString("effects.sounds.success.type", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) configManager.getConfig().getDouble("effects.sounds.success.volume", 1.0);
            float pitch = (float) configManager.getConfig().getDouble("effects.sounds.success.pitch", 1.5);
            
            try {
                Sound sound = Sound.valueOf(soundType);
                player.playSound(location, sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound type: " + soundType);
            }
        }
    }
    
    private void playFailureEffects(Player player, Location location) {
        if (configManager.areParticlesEnabled()) {
            String particleType = configManager.getConfig().getString("effects.particles.failure.type", "SMOKE_NORMAL");
            int count = configManager.getConfig().getInt("effects.particles.failure.count", 5);
            double speed = configManager.getConfig().getDouble("effects.particles.failure.speed", 0.1);
            double offsetX = configManager.getConfig().getDouble("effects.particles.failure.offset-x", 0.3);
            double offsetY = configManager.getConfig().getDouble("effects.particles.failure.offset-y", 0.3);
            double offsetZ = configManager.getConfig().getDouble("effects.particles.failure.offset-z", 0.3);
            
            try {
                Particle particle = Particle.valueOf(particleType);
                location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type: " + particleType);
            }
        }
        
        if (configManager.areSoundsEnabled()) {
            String soundType = configManager.getConfig().getString("effects.sounds.failure.type", "ENTITY_VILLAGER_NO");
            float volume = (float) configManager.getConfig().getDouble("effects.sounds.failure.volume", 1.0);
            float pitch = (float) configManager.getConfig().getDouble("effects.sounds.failure.pitch", 0.8);
            
            try {
                Sound sound = Sound.valueOf(soundType);
                player.playSound(location, sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound type: " + soundType);
            }
        }
    }
    
    private boolean canCatchInWorld(Player player, World world) {
        if (player.hasPermission("bubbleeggs.bypass.world")) {
            return true;
        }
        
        List<String> disabledWorlds = configManager.getConfig().getStringList("catching.disabled-worlds");
        return !disabledWorlds.contains(world.getName());
    }
    
    private boolean canCatchInRegion(Player player, Location location) {
        // Check if WorldGuard is available
        if (plugin.getWorldGuardManager() == null) {
            return true; // No WorldGuard, allow catching
        }
        
        return plugin.getWorldGuardManager().canCatchInRegion(player, location);
    }
    
    private boolean isBossMob(LivingEntity mob) {
        EntityType type = mob.getType();
        return type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.ELDER_GUARDIAN || type == EntityType.WARDEN;
    }
    
    private boolean isTamedMob(LivingEntity mob) {
        // Check if mob has an owner (for tameable mobs)
        try {
            return mob.getClass().getMethod("isTamed").invoke(mob).equals(true);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isOnCooldown(Player player) {
        if (player.hasPermission("bubbleeggs.bypass.cooldown")) {
            return false;
        }
        
        int cooldownSeconds = configManager.getCooldown();
        if (cooldownSeconds <= 0) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId)) {
            return false;
        }
        
        long lastCatch = cooldowns.get(playerId);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastCatch) < (cooldownSeconds * 1000L);
    }
    
    private long getRemainingCooldown(Player player) {
        int cooldownSeconds = configManager.getCooldown();
        UUID playerId = player.getUniqueId();
        long lastCatch = cooldowns.get(playerId);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastCatch;
        long remaining = (cooldownSeconds * 1000L) - elapsed;
        return Math.max(0, remaining / 1000L);
    }
    
    private void updateCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    private boolean hasEnoughItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
                if (count >= amount) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && remaining > 0) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
    }
}
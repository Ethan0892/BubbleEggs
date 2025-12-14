package com.bubbleeggs.utils;

import com.bubbleeggs.BubbleEggs;
import com.bubbleeggs.managers.ConfigManager;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemUtil {
    
    private final BubbleEggs plugin;
    private final ConfigManager configManager;
    private final MessageUtil messageUtil;
    
    public ItemUtil(BubbleEggs plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtil = plugin.getMessageUtil();
    }
    
    /**
     * Create a catch capsule item with configured properties
     */
    public ItemStack createCatchCapsule() {
        String materialName = configManager.getCatchCapsuleMaterial();
        Material material;
        
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid catch capsule material: " + materialName + ". Using SNOWBALL.");
            material = Material.SNOWBALL;
        }
        
    ItemStack capsule = new ItemStack(material);
        ItemMeta meta = capsule.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String name = configManager.getCatchCapsuleName();
            meta.setDisplayName(messageUtil.colorize(name));
            
            // Set lore
            List<String> loreConfig = configManager.getConfig().getStringList("catch-capsule.lore");
            if (!loreConfig.isEmpty()) {
                List<String> lore = messageUtil.colorizeLore(loreConfig);
                meta.setLore(lore);
            }
            
            // Set custom model data
            int customModelData = configManager.getConfig().getInt("catch-capsule.custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            
            // Add glow effect
            boolean glow = configManager.getConfig().getBoolean("catch-capsule.glow", true);
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            capsule.setItemMeta(meta);
        }
        
        // Tag with PDC always
        capsule = DataStorageUtil.tagCatchCapsule(capsule);

        // Try to add NBT data only if enabled (legacy)
        if (NBTCompat.ENABLED) {
            capsule = addNBTDataSafely(capsule, plugin);
        }
        
        return capsule;
    }
    
    /**
     * Safely add NBT data to an item, with fallback if NBT-API fails
     */
    private static ItemStack addNBTDataSafely(ItemStack item, BubbleEggs pluginInstance) {
        if (!NBTCompat.ENABLED) return item; // hard guard
        try {
            NBTItem nbtItem = new NBTItem(item);
            nbtItem.setBoolean("bubbleeggs.catch_capsule", true);
            nbtItem.setString("bubbleeggs.version", pluginInstance.getDescription().getVersion());
            return nbtItem.getItem();
        } catch (Throwable e) {
            NBTCompat.ENABLED = false;
            NBTCompat.DISABLE_REASON = e.getClass().getSimpleName();
            pluginInstance.getLogger().warning("Disabling NBT features due to error: " + e.getMessage());
            return item;
        }
    }
    
    /**
     * Create a spawn egg with mob data
     */
    public ItemStack createSpawnEgg(String mobType, String mobName, double health, double maxHealth) {
        Material eggMaterial = getSpawnEggMaterial(mobType);
        ItemStack spawnEgg = new ItemStack(eggMaterial);
        ItemMeta meta = spawnEgg.getItemMeta();
        
        if (meta != null) {
            // Set custom name
            String nameFormat = configManager.getConfig().getString("spawn-eggs.name-format", "&#FF6B6B%mob_name% &#FFFFFFSpawn Egg");
            String name = nameFormat.replace("%mob_name%", mobName != null ? mobName : mobType);
            meta.setDisplayName(messageUtil.colorize(name));
            
            // Set lore
            List<String> loreFormat = configManager.getConfig().getStringList("spawn-eggs.lore");
            if (!loreFormat.isEmpty()) {
                List<String> lore = messageUtil.colorizeLore(loreFormat);
                // Replace placeholders
                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    line = line.replace("%mob_name%", mobName != null ? mobName : mobType);
                    line = line.replace("%mob_health%", String.valueOf((int) health));
                    line = line.replace("%mob_max_health%", String.valueOf((int) maxHealth));
                    lore.set(i, line);
                }
                meta.setLore(lore);
            }
            
            // Set custom model data
            int customModelData = configManager.getConfig().getInt("spawn-eggs.custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            
            // Add glow effect
            boolean glow = configManager.getConfig().getBoolean("spawn-eggs.glow", false);
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            spawnEgg.setItemMeta(meta);
        }
        
        // Store mob data in PDC always (primary storage)
        if (configManager.isMobDataSavingEnabled()) {
            spawnEgg = DataStorageUtil.storeMobData(spawnEgg, mobType, mobName, health, maxHealth);
            // Additionally attempt NBT for backwards compatibility
            if (NBTCompat.ENABLED) {
                try {
                    NBTItem nbtItem = new NBTItem(spawnEgg);
                    nbtItem.setString("bubbleeggs.mob_type", mobType);
                    nbtItem.setDouble("bubbleeggs.mob_health", health);
                    nbtItem.setDouble("bubbleeggs.mob_max_health", maxHealth);
                    if (mobName != null) {
                        nbtItem.setString("bubbleeggs.mob_name", mobName);
                    }
                    spawnEgg = nbtItem.getItem();
                } catch (Throwable t) {
                    NBTCompat.ENABLED = false; NBTCompat.DISABLE_REASON = t.getClass().getSimpleName();
                    plugin.getLogger().info("Mob egg NBT disabled: " + t.getMessage());
                }
            }
        }
        
        return spawnEgg;
    }
    
    /**
     * Get the appropriate spawn egg material for a mob type
     */
    private Material getSpawnEggMaterial(String mobType) {
        try {
            return Material.valueOf(mobType + "_SPAWN_EGG");
        } catch (IllegalArgumentException e) {
            // Fallback for mobs without spawn eggs (like giants, boss mobs, etc.)
            return Material.CHICKEN_SPAWN_EGG;
        }
    }
    
    /**
     * Check if an item is a catch capsule
     */
    public boolean isCatchCapsule(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // First PDC check
        if (DataStorageUtil.isCatchCapsule(item)) return true;
        try {
            if (NBTCompat.ENABLED) {
                NBTItem nbtItem = new NBTItem(item);
                return nbtItem.getBoolean("bubbleeggs.catch_capsule");
            }
        } catch (Throwable e) {
            NBTCompat.ENABLED = false; NBTCompat.DISABLE_REASON = e.getClass().getSimpleName();
        }
        // Fallback heuristic
        String capsuleMaterial = configManager.getCatchCapsuleMaterial();
        String capsuleName = messageUtil.colorize(configManager.getCatchCapsuleName());
        if (!item.getType().toString().equals(capsuleMaterial)) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(capsuleName);
    }
    
    /**
     * Check if an item is a BubbleEggs spawn egg
     */
    public boolean isBubbleEggsSpawnEgg(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        // PDC primary detection
        if (DataStorageUtil.isMobEgg(item)) return true;
        if (NBTCompat.ENABLED) {
            try {
                NBTItem nbtItem = new NBTItem(item);
                if (nbtItem.hasKey("bubbleeggs.mob_type")) return true;
            } catch (Throwable t) {
                NBTCompat.ENABLED = false; NBTCompat.DISABLE_REASON = t.getClass().getSimpleName();
            }
        }
        // Fallback heuristic: display name contains 'Spawn Egg' & custom model data matches config (if any)
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String name = meta.getDisplayName().toLowerCase();
        if (!name.contains("spawn") || !name.contains("egg")) return false;
        int expected = configManager.getConfig().getInt("spawn-eggs.custom-model-data", 0);
        if (expected > 0 && (!meta.hasCustomModelData() || meta.getCustomModelData() != expected)) return false;
        return true;
    }

    /**
     * Get mob type from a BubbleEggs spawn egg
     */
    public String getMobTypeFromSpawnEgg(ItemStack item) {
        if (!isBubbleEggsSpawnEgg(item)) return null;
        // Try PDC first
        String pdcType = DataStorageUtil.getMobType(item);
        if (pdcType != null) return pdcType;
        if (NBTCompat.ENABLED) {
            try {
                NBTItem nbtItem = new NBTItem(item);
                return nbtItem.getString("bubbleeggs.mob_type");
            } catch (Throwable t) { NBTCompat.ENABLED = false; NBTCompat.DISABLE_REASON = t.getClass().getSimpleName(); }
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String raw = meta.getDisplayName().replaceAll("§[0-9A-FK-ORa-fk-or]", "");
            String lowered = raw.toLowerCase();
            if (lowered.contains("spawn egg")) {
                String base = raw.substring(0, lowered.indexOf("spawn egg")).trim();
                return base.isEmpty() ? null : base.toUpperCase().replace(' ', '_');
            }
        }
        return null;
    }

    /**
     * Get saved mob data from a spawn egg
     */
    public MobData getMobDataFromSpawnEgg(ItemStack item) {
        if (!isBubbleEggsSpawnEgg(item)) return null;
        // PDC path
        String type = DataStorageUtil.getMobType(item);
        if (type != null) {
            String name = DataStorageUtil.getMobName(item);
            Double hp = DataStorageUtil.getMobHealth(item);
            Double max = DataStorageUtil.getMobMaxHealth(item);
            return new MobData(type, name, hp != null ? hp : -1, max != null ? max : -1);
        }
        if (NBTCompat.ENABLED) {
            try {
                NBTItem nbtItem = new NBTItem(item);
                String nType = nbtItem.getString("bubbleeggs.mob_type");
                String nName = nbtItem.getString("bubbleeggs.mob_name");
                double hp = nbtItem.getDouble("bubbleeggs.mob_health");
                double max = nbtItem.getDouble("bubbleeggs.mob_max_health");
                return new MobData(nType, nName, hp, max);
            } catch (Throwable t) { NBTCompat.ENABLED = false; NBTCompat.DISABLE_REASON = t.getClass().getSimpleName(); }
        }
        String fallbackType = getMobTypeFromSpawnEgg(item);
        if (fallbackType == null) return null;
        return new MobData(fallbackType, null, -1, -1);
    }
    
    /**
     * Parse item cost string (MATERIAL:AMOUNT)
     */
    public ItemCost parseItemCost(String costString) {
        if (costString == null || costString.isEmpty()) {
            return null;
        }
        
        String[] parts = costString.split(":");
        if (parts.length != 2) {
            return null;
        }
        
        try {
            Material material = Material.valueOf(parts[0]);
            int amount = Integer.parseInt(parts[1]);
            return new ItemCost(material, amount);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if player has enough of a specific item
     */
    public boolean hasEnoughItems(org.bukkit.entity.Player player, Material material, int amount) {
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
    
    /**
     * Remove items from player inventory
     */
    public boolean removeItems(org.bukkit.entity.Player player, Material material, int amount) {
        if (!hasEnoughItems(player, material, amount)) {
            return false;
        }
        
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == material && remaining > 0) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
        
        return remaining == 0;
    }
    
    // Helper classes
    public static class MobData {
        private final String mobType;
        private final String mobName;
        private final double health;
        private final double maxHealth;
        
        public MobData(String mobType, String mobName, double health, double maxHealth) {
            this.mobType = mobType;
            this.mobName = mobName;
            this.health = health;
            this.maxHealth = maxHealth;
        }
        
        public String getMobType() { return mobType; }
        public String getMobName() { return mobName; }
        public double getHealth() { return health; }
        public double getMaxHealth() { return maxHealth; }
    }
    
    public static class ItemCost {
        private final Material material;
        private final int amount;
        
        public ItemCost(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }
        
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
    }
}
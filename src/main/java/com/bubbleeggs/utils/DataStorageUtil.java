package com.bubbleeggs.utils;

import com.bubbleeggs.BubbleEggs;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Unified storage abstraction using PersistentDataContainer first, optional NBT second.
 */
public final class DataStorageUtil {

    private static BubbleEggs plugin;

    // Keys
    private static NamespacedKey KEY_CAPSULE;
    private static NamespacedKey KEY_MOB_TYPE;
    private static NamespacedKey KEY_MOB_NAME;
    private static NamespacedKey KEY_MOB_HEALTH;
    private static NamespacedKey KEY_MOB_MAX_HEALTH;

    private DataStorageUtil() {}

    public static void init(BubbleEggs pl) {
        plugin = pl;
        KEY_CAPSULE = new NamespacedKey(pl, "catch_capsule");
        KEY_MOB_TYPE = new NamespacedKey(pl, "mob_type");
        KEY_MOB_NAME = new NamespacedKey(pl, "mob_name");
        KEY_MOB_HEALTH = new NamespacedKey(pl, "mob_health");
        KEY_MOB_MAX_HEALTH = new NamespacedKey(pl, "mob_max_health");
    }

    // Catch Capsule ------------------------------------------------------------------
    public static ItemStack tagCatchCapsule(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CAPSULE, PersistentDataType.BYTE, (byte)1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isCatchCapsule(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(KEY_CAPSULE, PersistentDataType.BYTE);
    }

    // Mob Data ----------------------------------------------------------------------
    public static ItemStack storeMobData(ItemStack egg, String type, String name, double health, double maxHealth) {
        ItemMeta meta = egg.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (type != null) pdc.set(KEY_MOB_TYPE, PersistentDataType.STRING, type);
            if (name != null) pdc.set(KEY_MOB_NAME, PersistentDataType.STRING, name);
            if (health >= 0) pdc.set(KEY_MOB_HEALTH, PersistentDataType.DOUBLE, health);
            if (maxHealth >= 0) pdc.set(KEY_MOB_MAX_HEALTH, PersistentDataType.DOUBLE, maxHealth);
            egg.setItemMeta(meta);
        }
        return egg;
    }

    public static boolean isMobEgg(ItemStack egg) {
        if (egg == null) return false;
        ItemMeta meta = egg.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(KEY_MOB_TYPE, PersistentDataType.STRING);
    }

    public static String getMobType(ItemStack egg) {
        ItemMeta meta = egg.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY_MOB_TYPE, PersistentDataType.STRING);
    }

    public static String getMobName(ItemStack egg) {
        ItemMeta meta = egg.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY_MOB_NAME, PersistentDataType.STRING);
    }

    public static Double getMobHealth(ItemStack egg) {
        ItemMeta meta = egg.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY_MOB_HEALTH, PersistentDataType.DOUBLE);
    }

    public static Double getMobMaxHealth(ItemStack egg) {
        ItemMeta meta = egg.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY_MOB_MAX_HEALTH, PersistentDataType.DOUBLE);
    }

    // Convenience from entity
    public static ItemStack storeMobData(ItemStack egg, LivingEntity mob, String type) {
        return storeMobData(egg, type, mob.getCustomName(), mob.getHealth(), mob.getMaxHealth());
    }
}

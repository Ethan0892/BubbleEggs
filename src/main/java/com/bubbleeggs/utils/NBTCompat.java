package com.bubbleeggs.utils;

import org.bukkit.Bukkit;

/**
 * Central compatibility gate for NBT usage. Avoids repeated reflective class loading
 * and prevents ExceptionInInitializerError crashes on unsupported server versions.
 */
public final class NBTCompat {

    public static boolean ENABLED = false; // set during plugin enable
    public static String DISABLE_REASON = "";

    private NBTCompat() {}

    /**
     * Attempt lightweight detection. We purposely DO NOT reference classes that
     * trigger deep static initialization until we are sure it's safe.
     */
    public static void detect(org.bukkit.plugin.Plugin plugin) {
        String version = Bukkit.getServer().getBukkitVersion();
        try {
            // Only attempt if the shaded nbtapi package is present in our jar
            ClassLoader cl = plugin.getClass().getClassLoader();
            // Use NBTItem (lighter) instead of NBTReflectionUtil which crashes early
            Class.forName("de.tr7zw.changeme.nbtapi.NBTItem", false, cl);
            ENABLED = true; // We'll still guard every actual call with try/catch
        } catch (Throwable t) {
            ENABLED = false;
            DISABLE_REASON = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
        }
    }
}

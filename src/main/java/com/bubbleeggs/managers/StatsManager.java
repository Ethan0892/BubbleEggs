package com.bubbleeggs.managers;

import com.bubbleeggs.BubbleEggs;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StatsManager {

    private final BubbleEggs plugin;
    private FileConfiguration statsConfig;
    private File statsFile;

    public StatsManager(BubbleEggs plugin) {
        this.plugin = plugin;
        loadStats();
    }

    public void loadStats() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create stats.yml: " + e.getMessage());
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void saveStats() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats.yml: " + e.getMessage());
        }
    }

    public int getPlayerCatches(UUID uuid) {
        return statsConfig.getInt("players." + uuid + ".catches", 0);
    }

    public int getPlayerRareCatches(UUID uuid) {
        return statsConfig.getInt("players." + uuid + ".rare-catches", 0);
    }

    public void incrementCatches(UUID uuid, boolean rare) {
        String path = "players." + uuid;
        statsConfig.set(path + ".catches", getPlayerCatches(uuid) + 1);
        if (rare) {
            statsConfig.set(path + ".rare-catches", getPlayerRareCatches(uuid) + 1);
        }
        saveStats();
    }

    public List<Map.Entry<String, Integer>> getTopCatches(int limit) {
        return getTopEntries("catches", limit);
    }

    public List<Map.Entry<String, Integer>> getTopRareCatches(int limit) {
        return getTopEntries("rare-catches", limit);
    }

    private List<Map.Entry<String, Integer>> getTopEntries(String stat, int limit) {
        if (!statsConfig.contains("players")) {
            return Collections.emptyList();
        }

        Map<String, Integer> entries = new LinkedHashMap<>();
        for (String uuidStr : statsConfig.getConfigurationSection("players").getKeys(false)) {
            int value = statsConfig.getInt("players." + uuidStr + "." + stat, 0);
            if (value > 0) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String name = op.getName() != null ? op.getName() : uuidStr;
                    entries.put(name, value);
                } catch (IllegalArgumentException e) {
                    entries.put(uuidStr, value);
                }
            }
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(entries.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }
}

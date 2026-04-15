package com.bubbleeggs.hooks;

import com.bubbleeggs.BubbleEggs;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final BubbleEggs plugin;

    public PlaceholderAPIHook(BubbleEggs plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "bubbleeggs";
    }

    @Override
    public String getAuthor() {
        return "BubbleCraft";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        switch (identifier) {
            case "player_catches":
                return String.valueOf(plugin.getStatsManager().getPlayerCatches(player.getUniqueId()));
            case "rare_catches":
                return String.valueOf(plugin.getStatsManager().getPlayerRareCatches(player.getUniqueId()));
            default:
                return null;
        }
    }
}

package com.bubbleeggs.commands;

import com.bubbleeggs.BubbleEggs;
import com.bubbleeggs.managers.ConfigManager;
import com.bubbleeggs.managers.StatsManager;
import com.bubbleeggs.utils.ItemUtil;
import com.bubbleeggs.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final BubbleEggs plugin;
    private final ConfigManager configManager;
    private final MessageUtil messageUtil;
    private final ItemUtil itemUtil;
    
    public MainCommand(BubbleEggs plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtil = plugin.getMessageUtil();
        this.itemUtil = new ItemUtil(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show plugin information
            showPluginInfo(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                showHelp(sender);
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            case "give":
                handleGive(sender, args);
                break;
                
            case "bulkupdate":
                handleBulkUpdate(sender, args);
                break;

            case "stats":
                handleStats(sender, args);
                break;

            case "top":
                handleTop(sender, args);
                break;
                
            default:
                messageUtil.sendLangMessage(sender, "commands.invalid-usage", "%usage%", "/mte help");
                break;
        }
        
        return true;
    }
    
    private void showPluginInfo(CommandSender sender) {
        String name = configManager.getLangConfig().getString("plugin.name", "BubbleEggs");
        String version = configManager.getLangConfig().getString("plugin.version", "Version: %version%")
            .replace("%version%", plugin.getDescription().getVersion());
        String author = configManager.getLangConfig().getString("plugin.author", "Author: %author%")
            .replace("%author%", String.join(", ", plugin.getDescription().getAuthors()));
        String description = configManager.getLangConfig().getString("plugin.description", "Catch mobs and give players spawn eggs!");
        
        sender.sendMessage(messageUtil.colorize(name));
        sender.sendMessage(messageUtil.colorize(version));
        sender.sendMessage(messageUtil.colorize(author));
        sender.sendMessage(messageUtil.colorize(description));
        sender.sendMessage(messageUtil.colorize("&#FFFFFFUse &#FF6B6B/mte help &#FFFFFFfor commands."));
    }
    
    private void showHelp(CommandSender sender) {
        String header = configManager.getLangConfig().getString("plugin.help-header", "=== BubbleEggs Help ===");
        String footer = configManager.getLangConfig().getString("plugin.help-footer", "========================");
        
        sender.sendMessage(messageUtil.colorize(header));
        
        List<String> helpMessages = configManager.getLangConfig().getStringList("commands.help");
        for (String message : helpMessages) {
            sender.sendMessage(messageUtil.colorize(message));
        }
        
        sender.sendMessage(messageUtil.colorize(footer));
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("bubbleeggs.reload")) {
            messageUtil.sendLangMessage(sender, "commands.no-permission");
            return;
        }
        
        try {
            configManager.reloadConfigs();
            plugin.registerCraftingRecipes();
            messageUtil.sendLangMessage(sender, "plugin.reload-success");
        } catch (Exception e) {
            messageUtil.sendLangMessage(sender, "plugin.reload-error", "%error%", e.getMessage());
        }
    }
    
    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bubbleeggs.give")) {
            messageUtil.sendLangMessage(sender, "commands.no-permission");
            return;
        }
        
        if (args.length < 2) {
            messageUtil.sendLangMessage(sender, "commands.invalid-usage", "%usage%", "/mte give <amount> [player]");
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                messageUtil.sendLangMessage(sender, "commands.give.invalid-amount");
                return;
            }
        } catch (NumberFormatException e) {
            messageUtil.sendLangMessage(sender, "commands.give.invalid-amount");
            return;
        }
        
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                messageUtil.sendLangMessage(sender, "commands.give.player-not-found", "%player%", args[2]);
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                messageUtil.sendLangMessage(sender, "commands.player-only");
                return;
            }
            target = (Player) sender;
        }
        
        // Create catch capsules
        ItemStack capsule = itemUtil.createCatchCapsule();
        capsule.setAmount(amount);
        
        // Give items to target
        target.getInventory().addItem(capsule);
        
        // Send messages
        if (target.equals(sender)) {
            messageUtil.sendLangMessage(sender, "commands.give.success-self", "%amount%", String.valueOf(amount));
        } else {
            messageUtil.sendLangMessage(sender, "commands.give.success-other", 
                "%amount%", String.valueOf(amount), "%player%", target.getName());
            messageUtil.sendLangMessage(target, "commands.give.success-target", 
                "%amount%", String.valueOf(amount), "%sender%", sender.getName());
        }
    }
    
    private void handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bubbleeggs.stats")) {
            messageUtil.sendLangMessage(sender, "stats.no-permission");
            return;
        }

        OfflinePlayer target;
        String targetName;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
            targetName = args[1];
        } else {
            if (!(sender instanceof Player)) {
                messageUtil.sendLangMessage(sender, "commands.player-only");
                return;
            }
            target = (Player) sender;
            targetName = sender.getName();
        }

        StatsManager stats = plugin.getStatsManager();
        int catches = stats.getPlayerCatches(target.getUniqueId());
        int rareCatches = stats.getPlayerRareCatches(target.getUniqueId());

        if (catches == 0 && rareCatches == 0) {
            messageUtil.sendLangMessage(sender, "stats.no-stats", "%player%", targetName);
            return;
        }

        String header = configManager.getLangConfig().getString("stats.header", "=== %player%'s Stats ===")
                .replace("%player%", targetName);
        sender.sendMessage(messageUtil.colorize(header));

        String catchesLine = configManager.getLangConfig().getString("stats.catches", "Total Catches: %catches%")
                .replace("%catches%", String.valueOf(catches));
        sender.sendMessage(messageUtil.colorize(catchesLine));

        String rareLine = configManager.getLangConfig().getString("stats.rare-catches", "Rare Catches: %rare%")
                .replace("%rare%", String.valueOf(rareCatches));
        sender.sendMessage(messageUtil.colorize(rareLine));
    }

    private void handleTop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bubbleeggs.top")) {
            messageUtil.sendLangMessage(sender, "leaderboard.no-permission");
            return;
        }

        String type = args.length >= 2 ? args[1].toLowerCase() : "catches";
        if (!type.equals("catches") && !type.equals("rare")) {
            messageUtil.sendLangMessage(sender, "leaderboard.invalid-type");
            return;
        }

        int page;
        try {
            page = args.length >= 3 ? Integer.parseInt(args[2]) : 1;
            if (page < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            messageUtil.sendLangMessage(sender, "leaderboard.invalid-page");
            return;
        }

        int perPage = 10;
        StatsManager stats = plugin.getStatsManager();
        List<Map.Entry<String, Integer>> entries = type.equals("rare")
                ? stats.getTopRareCatches(Integer.MAX_VALUE)
                : stats.getTopCatches(Integer.MAX_VALUE);

        if (entries.isEmpty()) {
            messageUtil.sendLangMessage(sender, "leaderboard.no-entries");
            return;
        }

        int totalPages = (int) Math.ceil(entries.size() / (double) perPage);
        if (page > totalPages) {
            messageUtil.sendLangMessage(sender, "leaderboard.invalid-page");
            return;
        }

        String displayType = type.equals("rare") ? "Rare Catches" : "Catches";
        String header = configManager.getLangConfig().getString("leaderboard.header", "=== Top %type% (Page %page%/%pages%) ===")
                .replace("%type%", displayType)
                .replace("%page%", String.valueOf(page))
                .replace("%pages%", String.valueOf(totalPages));
        sender.sendMessage(messageUtil.colorize(header));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, entries.size());
        String entryFormat = configManager.getLangConfig().getString("leaderboard.entry", "#%rank% %player%: %value%");

        for (int i = start; i < end; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            String line = entryFormat
                    .replace("%rank%", String.valueOf(i + 1))
                    .replace("%player%", entry.getKey())
                    .replace("%value%", String.valueOf(entry.getValue()));
            sender.sendMessage(messageUtil.colorize(line));
        }
    }

    private void handleBulkUpdate(CommandSender sender, String[] args) {        if (!sender.hasPermission("bubbleeggs.*")) {
            messageUtil.sendLangMessage(sender, "commands.no-permission");
            return;
        }
        
        if (args.length < 3) {
            messageUtil.sendLangMessage(sender, "commands.invalid-usage", "%usage%", "/mte bulkupdate <chance|money|item> <value>");
            return;
        }
        
        String type = args[1].toLowerCase();
        String value = args[2];
        
        switch (type) {
            case "chance":
                updateAllMobChances(sender, value);
                break;
                
            case "money":
                updateAllMobMoneyCosts(sender, value);
                break;
                
            case "item":
                updateAllMobItemCosts(sender, value);
                break;
                
            default:
                messageUtil.sendLangMessage(sender, "commands.bulkupdate.invalid-type");
                break;
        }
    }
    
    private void updateAllMobChances(CommandSender sender, String value) {
        try {
            double chance = Double.parseDouble(value);
            if (chance < 0.0 || chance > 1.0) {
                messageUtil.sendLangMessage(sender, "commands.bulkupdate.invalid-value", "%type%", "chance");
                return;
            }
            
            // Get all mob types from mobs.yml
            for (String mobType : configManager.getMobsConfig().getKeys(false)) {
                configManager.setMobCatchChance(mobType, chance);
            }
            
            configManager.saveMobsConfig();
            messageUtil.sendLangMessage(sender, "commands.bulkupdate.success", "%type%", "catch chance", "%value%", value);
            
        } catch (NumberFormatException e) {
            messageUtil.sendLangMessage(sender, "commands.bulkupdate.invalid-value", "%type%", "chance");
        }
    }
    
    private void updateAllMobMoneyCosts(CommandSender sender, String value) {
        try {
            double cost = Double.parseDouble(value);
            if (cost < 0.0) {
                messageUtil.sendLangMessage(sender, "commands.bulkupdate.invalid-value", "%type%", "money");
                return;
            }
            
            // Get all mob types from mobs.yml
            for (String mobType : configManager.getMobsConfig().getKeys(false)) {
                configManager.setMobMoneyCost(mobType, cost);
            }
            
            configManager.saveMobsConfig();
            messageUtil.sendLangMessage(sender, "commands.bulkupdate.success", "%type%", "money cost", "%value%", value);
            
        } catch (NumberFormatException e) {
            messageUtil.sendLangMessage(sender, "commands.bulkupdate.invalid-value", "%type%", "money");
        }
    }
    
    private void updateAllMobItemCosts(CommandSender sender, String value) {
        // Validate item cost format (MATERIAL:AMOUNT)
        String[] parts = value.split(":");
        if (parts.length != 2) {
            messageUtil.sendLangMessage(sender, "commands.bulkupdate.invalid-value", "%type%", "item");
            return;
        }
        
        try {
            // Validate material
            org.bukkit.Material.valueOf(parts[0]);
            // Validate amount
            int amount = Integer.parseInt(parts[1]);
            if (amount < 0) {
                messageUtil.sendLangMessage(sender, "commands.bulkupdate.invalid-value", "%type%", "item");
                return;
            }
            
            // Get all mob types from mobs.yml
            for (String mobType : configManager.getMobsConfig().getKeys(false)) {
                configManager.setMobItemCost(mobType, value);
            }
            
            configManager.saveMobsConfig();
            messageUtil.sendLangMessage(sender, "commands.bulkupdate.success", "%type%", "item cost", "%value%", value);
            
        } catch (IllegalArgumentException e) {
            messageUtil.sendLangMessage(sender, "commands.bulkupdate.invalid-value", "%type%", "item");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "reload", "give", "bulkupdate", "stats", "top");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("bulkupdate")) {
                List<String> types = Arrays.asList("chance", "money", "item");
                for (String type : types) {
                    if (type.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if (args[0].equalsIgnoreCase("top")) {
                List<String> types = Arrays.asList("catches", "rare");
                for (String type : types) {
                    if (type.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if (args[0].equalsIgnoreCase("stats")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                // Tab complete player names
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
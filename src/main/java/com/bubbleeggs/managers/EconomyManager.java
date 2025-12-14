package com.bubbleeggs.managers;

import com.bubbleeggs.BubbleEggs;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    
    private final BubbleEggs plugin;
    private Economy economy;
    private boolean economyEnabled = false;
    
    public EconomyManager(BubbleEggs plugin) {
        this.plugin = plugin;
    }
    
    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found! Economy features disabled.");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().info("No economy plugin found! Economy features disabled.");
            return false;
        }
        
        economy = rsp.getProvider();
        economyEnabled = (economy != null);
        
        if (economyEnabled) {
            plugin.getLogger().info("Economy system initialized with " + economy.getName());
        } else {
            plugin.getLogger().warning("Failed to initialize economy system!");
        }
        
        return economyEnabled;
    }
    
    public boolean isEconomyEnabled() {
        return economyEnabled && economy != null;
    }
    
    public double getBalance(Player player) {
        if (!isEconomyEnabled()) {
            return 0.0;
        }
        return economy.getBalance(player);
    }
    
    public boolean hasEnoughMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return true;
        }
        return economy.getBalance(player) >= amount;
    }
    
    public boolean withdrawMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return true;
        }
        
        if (!hasEnoughMoney(player, amount)) {
            return false;
        }
        
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public boolean depositMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return true;
        }
        
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    public String format(double amount) {
        if (!isEconomyEnabled()) {
            return String.valueOf(amount);
        }
        return economy.format(amount);
    }
    
    public String getCurrencyName(boolean plural) {
        if (!isEconomyEnabled()) {
            return plural ? "coins" : "coin";
        }
        return plural ? economy.currencyNamePlural() : economy.currencyNameSingular();
    }
}
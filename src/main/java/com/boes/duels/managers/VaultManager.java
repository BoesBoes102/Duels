package com.boes.duels.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

public class VaultManager {
    private final JavaPlugin plugin;
    private Economy economy;

    public VaultManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    public boolean isEnabled() {
        if (economy == null) {
            setupEconomy();
        }
        return economy != null;
    }

    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.has(player, amount);
    }

    public void withdrawMoney(Player player, double amount) {
        if (isEnabled()) {
            economy.withdrawPlayer(player, amount);
        }
    }

    public void depositMoney(Player player, double amount) {
        if (isEnabled()) {
            economy.depositPlayer(player, amount);
        }
    }

    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
    }
}

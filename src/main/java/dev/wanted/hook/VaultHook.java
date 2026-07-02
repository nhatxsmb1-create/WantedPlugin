package dev.wanted.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private Economy economy;

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public double getBalance(OfflinePlayer player) {
        return economy == null ? 0 : economy.getBalance(player);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        return economy != null && economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean isReady() { return economy != null; }
}

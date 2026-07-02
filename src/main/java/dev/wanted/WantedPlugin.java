package dev.wanted;

import dev.wanted.command.WantedCommand;
import dev.wanted.hook.AmbientHook;
import dev.wanted.hook.VaultHook;
import dev.wanted.listener.PlayerListener;
import dev.wanted.manager.ActionBarManager;
import dev.wanted.manager.GlowManager;
import dev.wanted.manager.WantedManager;
import dev.wanted.radar.RadarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class WantedPlugin extends JavaPlugin {

    private VaultHook vaultHook;
    private AmbientHook ambientHook;
    private WantedManager wantedManager;
    private GlowManager glowManager;
    private RadarManager radarManager;
    private ActionBarManager actionBarManager;
    private BukkitTask moneyTask;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        vaultHook = new VaultHook();
        if (!vaultHook.setup()) {
            getLogger().severe("Không tìm thấy Vault! Plugin tắt.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ambientHook = new AmbientHook(getLogger());
        glowManager = new GlowManager();
        wantedManager = new WantedManager(this, vaultHook);
        radarManager = new RadarManager(this);
        actionBarManager = new ActionBarManager(this);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        WantedCommand cmd = new WantedCommand(this);
        getCommand("wanted").setExecutor(cmd);
        getCommand("wanted").setTabCompleter(cmd);

        int moneyInterval = getConfig().getInt("settings.money-update-interval", 300) * 20;
        moneyTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) wantedManager.updateMoneyScore(p);
        }, moneyInterval, moneyInterval);

        int saveInterval = getConfig().getInt("settings.save-interval", 600) * 20;
        saveTask = Bukkit.getScheduler().runTaskTimer(this,
                () -> wantedManager.saveData(), saveInterval, saveInterval);

        getLogger().info("WantedPlugin V2 đã khởi động!");
        getLogger().info("Vault: ✔ | AmbientHorror: " + (ambientHook.isAvailable() ? "✔" : "✘"));
    }

    @Override
    public void onDisable() {
        if (moneyTask != null) moneyTask.cancel();
        if (saveTask != null) saveTask.cancel();
        if (radarManager != null) radarManager.shutdown();
        if (actionBarManager != null) actionBarManager.shutdown();
        if (wantedManager != null) wantedManager.saveData();
        getLogger().info("WantedPlugin V2 đã tắt, data đã lưu.");
    }

    public VaultHook getVaultHook() { return vaultHook; }
    public AmbientHook getAmbientHook() { return ambientHook; }
    public WantedManager getWantedManager() { return wantedManager; }
    public GlowManager getGlowManager() { return glowManager; }
}

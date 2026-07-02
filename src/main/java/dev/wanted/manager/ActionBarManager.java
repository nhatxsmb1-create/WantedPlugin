package dev.wanted.manager;

import dev.wanted.WantedPlugin;
import dev.wanted.data.WantedData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class ActionBarManager {

    private final WantedPlugin plugin;
    private BukkitTask task;

    public ActionBarManager(WantedPlugin plugin) {
        this.plugin = plugin;
        start();
    }

    private void start() {
        int interval = plugin.getConfig().getInt("settings.actionbar-interval-ticks", 40);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, interval, interval);
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            WantedData d = plugin.getWantedManager().getData(player.getUniqueId());
            if (d == null || !d.isWanted()) continue;

            int level = d.getWantedLevel();
            double reward = plugin.getWantedManager().getTotalReward(player.getUniqueId(), level);
            String color = plugin.getConfig().getString("wanted-levels." + level + ".color", "§c");
            String title = plugin.getConfig().getString("wanted-levels." + level + ".title", "");

            String bar = color + "⚠ BỊ TRUY NÃ " + title
                    + " §8| §fĐiểm: §e" + d.getScore()
                    + " §8| §fThưởng: §a" + String.format("%.0f", reward) + " Pin";

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(bar));
        }
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }
}

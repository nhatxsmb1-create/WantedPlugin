package dev.wanted.radar;

import dev.wanted.WantedPlugin;
import dev.wanted.data.WantedData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;

public class RadarManager {

    private final WantedPlugin plugin;
    private BukkitTask task;

    public RadarManager(WantedPlugin plugin) {
        this.plugin = plugin;
        start();
    }

    private void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllCompasses, 40L, 40L);
    }

    private void updateAllCompasses() {
        if (!plugin.getConfig().getBoolean("settings.compass-radar", true)) return;
        int minLevel = plugin.getConfig().getInt("settings.compass-radar-min-level", 3);

        for (Player player : Bukkit.getOnlinePlayers()) {
            WantedData selfData = plugin.getWantedManager().getData(player.getUniqueId());
            if (selfData != null && selfData.isWanted()) continue;

            Player nearestWanted = findNearestWanted(player, minLevel);
            if (nearestWanted != null) {
                pointCompass(player, nearestWanted.getLocation());
            }
        }
    }

    private Player findNearestWanted(Player hunter, int minLevel) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(hunter)) continue;
            if (!other.getWorld().equals(hunter.getWorld())) continue;

            WantedData d = plugin.getWantedManager().getData(other.getUniqueId());
            if (d == null || d.getWantedLevel() < minLevel) continue;

            double dist = hunter.getLocation().distanceSquared(other.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = other;
            }
        }
        return nearest;
    }

    private void pointCompass(Player player, Location target) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.COMPASS) {
            CompassMeta meta = (CompassMeta) hand.getItemMeta();
            meta.setLodestone(target);
            meta.setLodestoneTracked(false);
            hand.setItemMeta(meta);
            player.getInventory().setItemInMainHand(hand);
            return;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.COMPASS) {
            CompassMeta meta = (CompassMeta) offhand.getItemMeta();
            meta.setLodestone(target);
            meta.setLodestoneTracked(false);
            offhand.setItemMeta(meta);
            player.getInventory().setItemInOffHand(offhand);
        }
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }
}

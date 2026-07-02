package dev.wanted.listener;

import dev.wanted.WantedPlugin;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private final WantedPlugin plugin;

    public PlayerListener(WantedPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (event.getEntity() instanceof Player victim) {
            plugin.getWantedManager().handleWantedKill(killer, victim);
            plugin.getWantedManager().onPlayerDeath(victim);
            plugin.getWantedManager().addPvpKill(killer);
            return;
        }

        if (event.getEntity() instanceof Monster || event.getEntity() instanceof Animals) {
            plugin.getWantedManager().addMobKill(killer);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        plugin.getWantedManager().getOrCreate(p);
        plugin.getWantedManager().updateMoneyScore(p);

        var data = plugin.getWantedManager().getData(p.getUniqueId());
        if (data != null && data.isWanted()) {
            boolean shouldGlow = plugin.getConfig()
                    .getBoolean("wanted-levels." + data.getWantedLevel() + ".glow", false);
            if (shouldGlow) plugin.getGlowManager().applyGlow(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getGlowManager().removeGlow(event.getPlayer());
    }
}

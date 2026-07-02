package dev.wanted.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class AmbientHook {

    private boolean available = false;
    private Object presenceManager = null;
    private Method addPresenceMethod = null;
    private final Logger logger;

    public AmbientHook(Logger logger) {
        this.logger = logger;
        setup();
    }

    private void setup() {
        Plugin ambientPlugin = Bukkit.getPluginManager().getPlugin("AmbientHorror");
        if (ambientPlugin == null) {
            logger.info("[WantedPlugin] AmbientHorror không có — integration tắt.");
            return;
        }
        try {
            Class<?> apiClass = Class.forName("dev.ambienthorror.api.AmbientAPI");
            Method getPresence = apiClass.getMethod("getPresenceManager");
            presenceManager = getPresence.invoke(null);
            if (presenceManager != null) {
                addPresenceMethod = presenceManager.getClass()
                        .getMethod("addPresence", Player.class, double.class);
                available = true;
                logger.info("[WantedPlugin] AmbientHorror integration OK.");
            }
        } catch (Exception e) {
            logger.warning("[WantedPlugin] Không hook được AmbientHorror: " + e.getMessage());
        }
    }

    public void addPresenceBonus(Player player, double amount) {
        if (!available || presenceManager == null || addPresenceMethod == null) return;
        try {
            addPresenceMethod.invoke(presenceManager, player, amount);
        } catch (Exception ignored) {}
    }

    public boolean isAvailable() { return available; }
}

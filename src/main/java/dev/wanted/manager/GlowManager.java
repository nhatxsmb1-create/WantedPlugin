package dev.wanted.manager;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlowManager {

    private final Set<UUID> glowingPlayers = new HashSet<>();

    public void applyGlow(Player player) {
        if (glowingPlayers.contains(player.getUniqueId())) return;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING,
                Integer.MAX_VALUE,
                0,
                false,
                false,
                false
        ));
        glowingPlayers.add(player.getUniqueId());
    }

    public void removeGlow(Player player) {
        player.removePotionEffect(PotionEffectType.GLOWING);
        glowingPlayers.remove(player.getUniqueId());
    }

    public boolean isGlowing(UUID uuid) {
        return glowingPlayers.contains(uuid);
    }
}

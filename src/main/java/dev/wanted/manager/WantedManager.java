package dev.wanted.manager;

import dev.wanted.WantedPlugin;
import dev.wanted.data.WantedData;
import dev.wanted.hook.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class WantedManager {

    private final WantedPlugin plugin;
    private final VaultHook vault;
    private final Map<UUID, WantedData> dataMap = new HashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public WantedManager(WantedPlugin plugin, VaultHook vault) {
        this.plugin = plugin;
        this.vault = vault;
        loadData();
    }

    public WantedData getData(UUID uuid) { return dataMap.get(uuid); }

    public WantedData getOrCreate(Player player) {
        WantedData d = dataMap.computeIfAbsent(player.getUniqueId(),
                k -> new WantedData(player.getUniqueId(), player.getName()));
        d.setName(player.getName());
        return d;
    }

    public Collection<WantedData> getAll() { return dataMap.values(); }

    public void addPvpKill(Player player) {
        if (player.hasPermission("wanted.bypass")) return;
        WantedData d = getOrCreate(player);
        d.addPvpKill();
        d.addScore(plugin.getConfig().getInt("scoring.pvp-kill", 20));
        d.addSeasonScore(plugin.getConfig().getInt("scoring.pvp-kill", 20));
        checkAndUpdateLevel(player, d);
    }

    public void addMobKill(Player player) {
        if (player.hasPermission("wanted.bypass")) return;
        WantedData d = getOrCreate(player);
        d.addMobKill();
        d.addScore(plugin.getConfig().getInt("scoring.mob-kill", 1));
        checkAndUpdateLevel(player, d);
    }

    public void updateMoneyScore(Player player) {
        if (!vault.isReady() || player.hasPermission("wanted.bypass")) return;
        WantedData d = getOrCreate(player);
        double balance = vault.getBalance(player);
        int ppm = plugin.getConfig().getInt("scoring.money-per-1000", 2);
        int moneyPts = (int) ((balance / 1000.0) * ppm);
        int baseScore = d.getPvpKills() * plugin.getConfig().getInt("scoring.pvp-kill", 20)
                + d.getMobKills() * plugin.getConfig().getInt("scoring.mob-kill", 1);
        d.setScore(baseScore + moneyPts);
        checkAndUpdateLevel(player, d);
    }

    public void onPlayerDeath(Player player) {
        WantedData d = getData(player.getUniqueId());
        if (d == null) return;
        d.addScore(-plugin.getConfig().getInt("settings.score-reduce-on-death", 25));
        checkAndUpdateLevel(player, d);
    }

    private void checkAndUpdateLevel(Player player, WantedData data) {
        int newLevel = calculateLevel(data.getScore());
        int oldLevel = data.getWantedLevel();
        if (newLevel == oldLevel) return;
        data.setWantedLevel(newLevel);
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (newLevel == 0 && oldLevel > 0) {
            plugin.getGlowManager().removeGlow(player);
            if (plugin.getConfig().getBoolean("settings.broadcast-on-wanted", true))
                Bukkit.broadcastMessage(prefix + plugin.getConfig()
                        .getString("messages.no-longer-wanted", "")
                        .replace("%player%", player.getName()));
        } else if (newLevel > 0 && oldLevel == 0) {
            applyWantedEffects(player, newLevel);
            if (plugin.getConfig().getBoolean("settings.broadcast-on-wanted", true))
                broadcastWanted(player, newLevel, "player-wanted");
        } else if (newLevel > oldLevel) {
            applyWantedEffects(player, newLevel);
            if (plugin.getConfig().getBoolean("settings.broadcast-on-wanted", true))
                broadcastWanted(player, newLevel, "player-level-up");
        } else {
            applyWantedEffects(player, newLevel);
        }
    }

    private void broadcastWanted(Player player, int level, String msgKey) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        double reward = getTotalReward(player.getUniqueId(), level);
        String msg = prefix + plugin.getConfig().getString("messages." + msgKey, "")
                .replace("%player%", player.getName())
                .replace("%level%", String.valueOf(level))
                .replace("%reward%", String.format("%.0f", reward));
        Bukkit.broadcastMessage(msg);
        player.sendTitle(
                plugin.getConfig().getString("wanted-levels." + level + ".color", "§c") + "⚠ TRUY NÃ CẤP " + level + " ⚠",
                "§fTiền thưởng: §a" + String.format("%.0f", reward) + " Pin",
                10, 70, 20);
    }

    private void applyWantedEffects(Player player, int level) {
        boolean shouldGlow = plugin.getConfig().getBoolean("wanted-levels." + level + ".glow", false)
                && plugin.getConfig().getBoolean("settings.glow-effect", true);
        if (shouldGlow) plugin.getGlowManager().applyGlow(player);
        else plugin.getGlowManager().removeGlow(player);

        if (plugin.getConfig().getBoolean("settings.ambient-integration", true)) {
            double bonus = plugin.getConfig().getDouble("settings.ambient-presence-bonus-per-level", 8.0) * level;
            plugin.getAmbientHook().addPresenceBonus(player, bonus);
        }
    }

    public int calculateLevel(int score) {
        int max = 0;
        for (int lvl = 1; lvl <= 5; lvl++)
            if (score >= plugin.getConfig().getInt("wanted-levels." + lvl + ".score", Integer.MAX_VALUE))
                max = lvl;
        return max;
    }

    public double getBaseReward(int level) {
        return plugin.getConfig().getDouble("wanted-levels." + level + ".reward", 0);
    }

    public double getTotalReward(UUID uuid, int level) {
        double base = getBaseReward(level);
        WantedData d = dataMap.get(uuid);
        return base + (d != null ? d.getExtraBounty() : 0);
    }

    public String getTitleForLevel(int level) {
        return plugin.getConfig().getString("wanted-levels." + level + ".title", "");
    }

    public void handleWantedKill(Player killer, Player victim) {
        WantedData vData = getData(victim.getUniqueId());
        if (vData == null || !vData.isWanted()) return;
        int level = vData.getWantedLevel();
        double reward = getTotalReward(victim.getUniqueId(), level);
        if (vault.isReady()) vault.deposit(killer, reward);
        getOrCreate(killer).addWantedKill();
        vData.clearExtraBounty();
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        Bukkit.broadcastMessage(prefix + plugin.getConfig().getString("messages.player-killed", "")
                .replace("%killer%", killer.getName())
                .replace("%target%", victim.getName())
                .replace("%reward%", String.format("%.0f", reward)));
        killer.sendTitle("§a✔ TIÊU DIỆT", "§f+" + String.format("%.0f", reward) + " Pin", 10, 60, 20);
        if (plugin.getConfig().getBoolean("settings.kill-particles", true))
            victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                    victim.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        plugin.getGlowManager().removeGlow(victim);
    }

    public String placeBounty(Player placer, Player target, double amount) {
        if (!vault.isReady()) return "§cKhông có hệ thống kinh tế.";
        double min = plugin.getConfig().getDouble("bounty.min-amount", 50);
        double max = plugin.getConfig().getDouble("bounty.max-amount", 5000);
        if (amount < min) return "§cTối thiểu §f" + min + " Pin.";
        if (amount > max) return "§cTối đa §f" + max + " Pin.";
        double tax = amount * plugin.getConfig().getDouble("bounty.tax-percent", 10) / 100.0;
        double total = amount + tax;
        if (!vault.has(placer, total)) return "§cKhông đủ Pin. Cần §f" + String.format("%.0f", total) + " Pin.";
        vault.withdraw(placer, total);
        getOrCreate(target).addExtraBounty(amount);
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        Bukkit.broadcastMessage(prefix + plugin.getConfig().getString("messages.bounty-placed", "")
                .replace("%placer%", placer.getName())
                .replace("%target%", target.getName())
                .replace("%amount%", String.format("%.0f", amount)));
        return null;
    }

    public void forceSetWanted(Player player, int level) {
        WantedData d = getOrCreate(player);
        d.setScore(plugin.getConfig().getInt("wanted-levels." + level + ".score", 0));
        d.setWantedLevel(level);
        applyWantedEffects(player, level);
        Bukkit.broadcastMessage(plugin.getConfig().getString("messages.prefix", "")
                + "§c" + player.getName() + " §fbị admin set truy nã cấp §c" + level);
    }

    public void clearWanted(UUID uuid, String name) {
        WantedData d = dataMap.get(uuid);
        if (d != null) { d.setWantedLevel(0); d.setScore(0); d.clearExtraBounty(); }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) plugin.getGlowManager().removeGlow(online);
        Bukkit.broadcastMessage(plugin.getConfig().getString("messages.prefix", "")
                + plugin.getConfig().getString("messages.no-longer-wanted", "")
                .replace("%player%", name));
    }

    public List<WantedData> getWantedList() {
        return dataMap.values().stream().filter(WantedData::isWanted)
                .sorted(Comparator.comparingInt(WantedData::getWantedLevel).reversed()
                        .thenComparingInt(WantedData::getScore).reversed())
                .collect(Collectors.toList());
    }

    public List<WantedData> getTopScore(int limit) {
        return dataMap.values().stream()
                .sorted(Comparator.comparingInt(WantedData::getScore).reversed())
                .limit(limit).collect(Collectors.toList());
    }

    public List<WantedData> getTopSeason(int limit) {
        return dataMap.values().stream()
                .sorted(Comparator.comparingInt(WantedData::getSeasonScore).reversed())
                .limit(limit).collect(Collectors.toList());
    }

    public List<WantedData> getTopHunters(int limit) {
        return dataMap.values().stream()
                .sorted(Comparator.comparingInt(WantedData::getTotalWantedKills).reversed())
                .limit(limit).collect(Collectors.toList());
    }

    public void endSeason() {
        List<WantedData> top = getTopSeason(3);
        double[] rewards = {
                plugin.getConfig().getDouble("season.top1-reward", 10000),
                plugin.getConfig().getDouble("season.top2-reward", 5000),
                plugin.getConfig().getDouble("season.top3-reward", 2000)
        };
        String[] ranks = {"§6§l[TOP 1]", "§7§l[TOP 2]", "§c§l[TOP 3]"};
        Bukkit.broadcastMessage("§8§m══════════════════════════════════");
        Bukkit.broadcastMessage("§f        §4§l★ KẾT THÚC MÙA TRUY NÃ ★");
        Bukkit.broadcastMessage("§8§m══════════════════════════════════");
        for (int i = 0; i < Math.min(top.size(), 3); i++) {
            WantedData d = top.get(i);
            double reward = rewards[i];
            Bukkit.broadcastMessage("  " + ranks[i] + " §f" + d.getName()
                    + " §8| §eĐiểm: " + d.getSeasonScore()
                    + " §8| §aNhận: " + String.format("%.0f", reward) + " Pin");
            Player online = Bukkit.getPlayer(d.getUuid());
            if (online != null && vault.isReady()) {
                vault.deposit(online, reward);
                online.sendTitle("§6§l★ PHẦN THƯỞNG MÙA ★",
                        "§fBạn nhận §a" + String.format("%.0f", reward) + " Pin", 10, 80, 20);
            }
        }
        dataMap.values().forEach(WantedData::resetSeason);
        int newSeason = plugin.getConfig().getInt("season.current-season", 1) + 1;
        plugin.getConfig().set("season.current-season", newSeason);
        plugin.saveConfig();
        Bukkit.broadcastMessage("  §fMùa §e" + newSeason + " §fbắt đầu!");
        Bukkit.broadcastMessage("§8§m══════════════════════════════════");
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.contains("players")) return;
        for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "players." + uuidStr;
                WantedData d = new WantedData(uuid, dataConfig.getString(path + ".name", "Unknown"));
                d.setScore(dataConfig.getInt(path + ".score", 0));
                d.setPvpKills(dataConfig.getInt(path + ".pvp-kills", 0));
                d.setMobKills(dataConfig.getInt(path + ".mob-kills", 0));
                d.setWantedLevel(dataConfig.getInt(path + ".wanted-level", 0));
                d.addExtraBounty(dataConfig.getDouble(path + ".extra-bounty", 0));
                d.setSeasonScore(dataConfig.getInt(path + ".season-score", 0));
                dataMap.put(uuid, d);
            } catch (Exception e) {
                plugin.getLogger().warning("Không load được UUID: " + uuidStr);
            }
        }
        plugin.getLogger().info("[WantedPlugin] Loaded " + dataMap.size() + " profiles.");
    }

    public void saveData() {
        if (dataFile == null) return;
        dataConfig = new YamlConfiguration();
        for (WantedData d : dataMap.values()) {
            String path = "players." + d.getUuid();
            dataConfig.set(path + ".name", d.getName());
            dataConfig.set(path + ".score", d.getScore());
            dataConfig.set(path + ".pvp-kills", d.getPvpKills());
            dataConfig.set(path + ".mob-kills", d.getMobKills());
            dataConfig.set(path + ".wanted-level", d.getWantedLevel());
            dataConfig.set(path + ".extra-bounty", d.getExtraBounty());
            dataConfig.set(path + ".season-score", d.getSeasonScore());
        }
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("Không lưu được data.yml!"); }
    }
            }

package dev.wanted.command;

import dev.wanted.WantedPlugin;
import dev.wanted.data.WantedData;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WantedCommand implements CommandExecutor, TabCompleter {

    private final WantedPlugin plugin;

    public WantedCommand(WantedPlugin plugin) {
        this.plugin = plugin;
    }

    private String p() { return plugin.getConfig().getString("messages.prefix", "§8[§cTRUY NÃ§8] §r"); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        return switch (args[0].toLowerCase()) {
            case "list" -> cmdList(sender);
            case "top" -> cmdTop(sender, args);
            case "info" -> cmdInfo(sender, args);
            case "set" -> cmdSet(sender, args);
            case "clear" -> cmdClear(sender, args);
            case "bounty" -> cmdBounty(sender, args);
            case "season" -> cmdSeason(sender, args);
            case "reload" -> cmdReload(sender);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean cmdList(CommandSender sender) {
        List<WantedData> list = plugin.getWantedManager().getWantedList();
        header(sender, "DANH SÁCH TRUY NÃ");
        if (list.isEmpty()) {
            sender.sendMessage("  §7Hiện không có ai bị truy nã.");
        } else {
            int i = 1;
            for (WantedData d : list) {
                double reward = plugin.getWantedManager().getTotalReward(d.getUuid(), d.getWantedLevel());
                String extra = d.getExtraBounty() > 0 ? " §e(+§a" + String.format("%.0f", d.getExtraBounty()) + "§e Pin bounty)" : "";
                sender.sendMessage("  §f" + i++ + ". §c" + d.getName()
                        + " " + plugin.getWantedManager().getTitleForLevel(d.getWantedLevel())
                        + " §8| §fĐiểm: §e" + d.getScore()
                        + " §8| §fThưởng: §a" + String.format("%.0f", reward) + " Pin" + extra);
            }
        }
        footer(sender);
        return true;
    }

    private boolean cmdTop(CommandSender sender, String[] args) {
        String mode = args.length >= 2 ? args[1].toLowerCase() : "score";
        List<WantedData> top;
        String title;
        switch (mode) {
            case "season" -> { top = plugin.getWantedManager().getTopSeason(10); title = "TOP ĐIỂM MÙA " + plugin.getConfig().getInt("season.current-season", 1); }
            case "hunter" -> { top = plugin.getWantedManager().getTopHunters(10); title = "TOP THỢ SĂN"; }
            default -> { top = plugin.getWantedManager().getTopScore(10); title = "TOP ĐIỂM TRUY NÃ"; }
        }
        header(sender, title);
        String[] medals = {"§6[1]", "§7[2]", "§c[3]"};
        int i = 0;
        for (WantedData d : top) {
            String medal = i < 3 ? medals[i] : "§8[" + (i+1) + "]";
            String val = switch (mode) {
                case "season" -> "§eĐiểm mùa: " + d.getSeasonScore();
                case "hunter" -> "§aSăn được: " + d.getTotalWantedKills() + " người";
                default -> "§eĐiểm: " + d.getScore() + (d.isWanted() ? " " + plugin.getWantedManager().getTitleForLevel(d.getWantedLevel()) : "");
            };
            sender.sendMessage("  " + medal + " §f" + d.getName() + " §8| " + val);
            i++;
        }
        footer(sender);
        return true;
    }

    private boolean cmdInfo(CommandSender sender, String[] args) {
        WantedData d;
        if (args.length < 2) {
            if (!(sender instanceof Player p)) { sender.sendMessage(p() + "§cNhập tên người chơi."); return true; }
            d = plugin.getWantedManager().getOrCreate(p);
        } else {
            d = findData(args[1]);
            if (d == null) { sender.sendMessage(p() + "§cKhông tìm thấy §f" + args[1]); return true; }
        }
        header(sender, "HỒ SƠ: " + d.getName());
        sender.sendMessage("  §fTrạng thái: " + (d.isWanted() ? plugin.getWantedManager().getTitleForLevel(d.getWantedLevel()) : "§a[BÌNH THƯỜNG]"));
        sender.sendMessage("  §fĐiểm: §e" + d.getScore() + " §8| §fMùa: §e" + d.getSeasonScore());
        sender.sendMessage("  §fPvP Kill: §c" + d.getPvpKills() + " §8| §fMob Kill: §a" + d.getMobKills());
        sender.sendMessage("  §fSăn được: §6" + d.getTotalWantedKills() + " §fngười bị truy nã");
        if (d.isWanted()) {
            double reward = plugin.getWantedManager().getTotalReward(d.getUuid(), d.getWantedLevel());
            sender.sendMessage("  §fTiền thưởng: §a" + String.format("%.0f", reward) + " Pin"
                    + (d.getExtraBounty() > 0 ? " §8(§e+" + String.format("%.0f", d.getExtraBounty()) + " bounty§8)" : ""));
        }
        footer(sender);
        return true;
    }

    private boolean cmdSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wanted.admin")) { sender.sendMessage(p() + "§cKhông có quyền."); return true; }
        if (args.length < 3) { sender.sendMessage(p() + "§cCú pháp: /wanted set <player> <1-5>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(p() + "§c" + args[1] + " không online."); return true; }
        try {
            int level = Integer.parseInt(args[2]);
            if (level < 1 || level > 5) throw new NumberFormatException();
            plugin.getWantedManager().forceSetWanted(target, level);
            sender.sendMessage(p() + "§aĐã set §c" + target.getName() + " §fthành cấp §c" + level);
        } catch (NumberFormatException e) {
            sender.sendMessage(p() + "§cLevel phải từ 1–5.");
        }
        return true;
    }

    private boolean cmdClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wanted.admin")) { sender.sendMessage(p() + "§cKhông có quyền."); return true; }
        if (args.length < 2) { sender.sendMessage(p() + "§cCú pháp: /wanted clear <player>"); return true; }
        WantedData d = findData(args[1]);
        if (d == null) { sender.sendMessage(p() + "§cKhông tìm thấy §f" + args[1]); return true; }
        plugin.getWantedManager().clearWanted(d.getUuid(), d.getName());
        sender.sendMessage(p() + "§aĐã xóa truy nã của §f" + d.getName());
        return true;
    }

    private boolean cmdBounty(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("bounty.enabled", true)) { sender.sendMessage(p() + "§cBounty đang tắt."); return true; }
        if (!(sender instanceof Player placer)) { sender.sendMessage(p() + "§cChỉ người chơi dùng được."); return true; }
        if (!placer.hasPermission("wanted.bounty.place")) { sender.sendMessage(p() + "§cKhông có quyền."); return true; }
        if (args.length < 3) { sender.sendMessage(p() + "§cCú pháp: /wanted bounty <player> <amount>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(p() + "§c" + args[1] + " không online."); return true; }
        if (target.equals(placer)) { sender.sendMessage(p() + "§cKhông thể đặt bounty cho chính mình."); return true; }
        try {
            double amount = Double.parseDouble(args[2]);
            String err = plugin.getWantedManager().placeBounty(placer, target, amount);
            if (err != null) sender.sendMessage(p() + err);
        } catch (NumberFormatException e) {
            sender.sendMessage(p() + "§cSố tiền không hợp lệ.");
        }
        return true;
    }

    private boolean cmdSeason(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("end")) {
            if (!sender.hasPermission("wanted.admin")) { sender.sendMessage(p() + "§cKhông có quyền."); return true; }
            plugin.getWantedManager().endSeason();
            return true;
        }
        int season = plugin.getConfig().getInt("season.current-season", 1);
        List<WantedData> top3 = plugin.getWantedManager().getTopSeason(3);
        header(sender, "MÙA " + season);
        String[] medals = {"§6★ Top 1", "§7★ Top 2", "§c★ Top 3"};
        double[] rewards = {
                plugin.getConfig().getDouble("season.top1-reward", 10000),
                plugin.getConfig().getDouble("season.top2-reward", 5000),
                plugin.getConfig().getDouble("season.top3-reward", 2000)
        };
        for (int i = 0; i < Math.min(top3.size(), 3); i++) {
            WantedData d = top3.get(i);
            sender.sendMessage("  " + medals[i] + " §f" + d.getName()
                    + " §8| §eĐiểm: " + d.getSeasonScore()
                    + " §8| §aThưởng: " + String.format("%.0f", rewards[i]) + " Pin");
        }
        if (sender.hasPermission("wanted.admin"))
            sender.sendMessage("  §7[Admin] /wanted season end — kết thúc mùa & trao thưởng");
        footer(sender);
        return true;
    }

    private boolean cmdReload(CommandSender sender) {
        if (!sender.hasPermission("wanted.admin")) { sender.sendMessage(p() + "§cKhông có quyền."); return true; }
        plugin.reloadConfig();
        sender.sendMessage(p() + "§aReload xong.");
        return true;
    }

    private WantedData findData(String name) {
        for (WantedData d : plugin.getWantedManager().getAll())
            if (d.getName().equalsIgnoreCase(name)) return d;
        return null;
    }

    private void header(CommandSender s, String title) {
        s.sendMessage("§8§m══════════════════════════════════");
        s.sendMessage("§c§l  " + title);
        s.sendMessage("§8§m══════════════════════════════════");
    }

    private void footer(CommandSender s) {
        s.sendMessage("§8§m══════════════════════════════════");
    }

    private void sendHelp(CommandSender sender) {
        header(sender, "TRUY NÃ — LỆNH");
        sender.sendMessage("  §f/wanted list §8- §7Danh sách bị truy nã");
        sender.sendMessage("  §f/wanted top [score|season|hunter] §8- §7Bảng xếp hạng");
        sender.sendMessage("  §f/wanted info [player] §8- §7Hồ sơ người chơi");
        sender.sendMessage("  §f/wanted bounty <player> <Pin> §8- §7Đặt tiền thưởng");
        sender.sendMessage("  §f/wanted season §8- §7Thông tin mùa hiện tại");
        if (sender.hasPermission("wanted.admin")) {
            sender.sendMessage("  §f/wanted set <player> <1-5> §8- §7[Admin] Set cấp");
            sender.sendMessage("  §f/wanted clear <player> §8- §7[Admin] Xóa truy nã");
            sender.sendMessage("  §f/wanted season end §8- §7[Admin] Kết thúc mùa");
            sender.sendMessage("  §f/wanted reload §8- §7[Admin] Reload config");
        }
        footer(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return List.of("list","top","info","set","clear","bounty","season","reload")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2)
            return switch (args[0].toLowerCase()) {
                case "top" -> List.of("score", "season", "hunter");
                case "set", "clear", "bounty", "info" -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "season" -> List.of("end");
                default -> List.of();
            };
        if (args.length == 3 && args[0].equalsIgnoreCase("set"))
            return List.of("1","2","3","4","5");
        return List.of();
    }
              }

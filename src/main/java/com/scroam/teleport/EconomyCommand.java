package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EconomyCommand implements CommandExecutor {

    private final TeleportPlugin plugin;

    public EconomyCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("balance").setExecutor(this);
        plugin.getCommand("bal").setExecutor(this);
        plugin.getCommand("pay").setExecutor(this);
        plugin.getCommand("givemoney").setExecutor(this);
        plugin.getCommand("rtpinfo").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;
        DatabaseManager db = plugin.getDatabaseManager();

        if (db == null) {
            player.sendMessage(ChatColor.RED + "数据库未启用!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("balance") || cmd.getName().equalsIgnoreCase("bal")) {
            double balance = db.getBalance(player.getUniqueId());
            int freeCount = db.getRtpFreeCount(player.getUniqueId());
            double rtpPrice = db.getUser(player.getUniqueId()).rtpPrice;
            
            player.sendMessage(ChatColor.GREEN + "你的余额: " + ChatColor.GOLD + String.format("%.2f", balance));
            player.sendMessage(ChatColor.YELLOW + "今日剩余免费RTP次数: " + ChatColor.GOLD + freeCount);
            player.sendMessage(ChatColor.YELLOW + "RTP价格: " + ChatColor.GOLD + String.format("%.2f", rtpPrice));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("pay")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "用法: /pay <玩家名> <金额>");
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "玩家不在线!");
                return true;
            }

            if (player.equals(target)) {
                player.sendMessage(ChatColor.RED + "不能给自己转账!");
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "无效的金额!");
                return true;
            }

            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "金额必须大于0!");
                return true;
            }

            // 计算税收
            double taxRate = plugin.getConfigManager().getTransferTax();
            double tax = amount * taxRate;
            double actualAmount = amount - tax;
            double totalCost = amount; // 发送者需要支付全额（含税）

            if (!db.withdrawBalance(player.getUniqueId(), totalCost)) {
                player.sendMessage(ChatColor.RED + "余额不足! 需要 " + String.format("%.2f", totalCost) + " 金币");
                return true;
            }

            // 接收者收到扣除税后的金额
            db.depositBalance(target.getUniqueId(), actualAmount);
            
            // 显示转账信息
            player.sendMessage(ChatColor.GREEN + "已向 " + target.getName() + " 转账 " + String.format("%.2f", actualAmount));
            if (tax > 0) {
                player.sendMessage(ChatColor.YELLOW + "手续费: " + ChatColor.RED + String.format("%.2f", tax) + " (税率 " + (taxRate * 100) + "%)");
            }
            target.sendMessage(ChatColor.GREEN + player.getName() + " 向你转账了 " + String.format("%.2f", actualAmount));
            if (tax > 0) {
                target.sendMessage(ChatColor.YELLOW + "系统收取了 " + String.format("%.2f", tax) + " 手续费");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("givemoney")) {
            if (!player.hasPermission("teleport.admin")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "用法: /givemoney <玩家名> <金额>");
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "玩家不在线!");
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "无效的金额!");
                return true;
            }

            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "金额必须大于0!");
                return true;
            }

            db.depositBalance(target.getUniqueId(), amount);
            player.sendMessage(ChatColor.GREEN + "已向 " + target.getName() + " 发放 " + String.format("%.2f", amount));
            target.sendMessage(ChatColor.GREEN + "管理员给你发放了 " + String.format("%.2f", amount));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("rtpinfo")) {
            int freeCount = db.getRtpFreeCount(player.getUniqueId());
            DatabaseManager.UserData user = db.getUser(player.getUniqueId());
            
            player.sendMessage(ChatColor.YELLOW + "=== RTP 信息 ===");
            player.sendMessage(ChatColor.GREEN + "今日免费次数: " + ChatColor.GOLD + freeCount + "/" + user.rtpMaxFreeCount);
            player.sendMessage(ChatColor.GREEN + "RTP价格: " + ChatColor.GOLD + String.format("%.2f", user.rtpPrice));
            player.sendMessage(ChatColor.GREEN + "你的余额: " + ChatColor.GOLD + String.format("%.2f", user.balance));
            
            if (freeCount > 0) {
                player.sendMessage(ChatColor.GREEN + "下次RTP将使用免费次数!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "下次RTP将扣除 " + String.format("%.2f", user.rtpPrice) + " 金币!");
            }
            return true;
        }

        return false;
    }
}

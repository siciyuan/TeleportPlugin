package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class AdminCommand implements CommandExecutor {

    private final TeleportPlugin plugin;

    public AdminCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("teleportadmin").setExecutor(this);
        plugin.getCommand("setrtpprice").setExecutor(this);
        plugin.getCommand("setrtpfree").setExecutor(this);
        plugin.getCommand("setmaxhomes").setExecutor(this);
        plugin.getCommand("settax").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("teleport.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        String commandName = cmd.getName().toLowerCase();

        switch (commandName) {
            case "teleportadmin":
                return handleAdminCommand(sender, args);
            case "setrtpprice":
                return setRtpPrice(sender, args);
            case "setrtpfree":
                return setRtpFreeCount(sender, args);
            case "setmaxhomes":
                return setMaxHomes(sender, args);
            case "settax":
                return setTransferTax(sender, args);
            default:
                return false;
        }
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "=== TeleportPlugin 管理命令 ===");
            sender.sendMessage(ChatColor.YELLOW + "/setrtpprice <价格> - 设置RTP价格");
            sender.sendMessage(ChatColor.YELLOW + "/setrtpfree <次数> - 设置每日免费RTP次数");
            sender.sendMessage(ChatColor.YELLOW + "/setmaxhomes <数量> - 设置玩家最大家数量");
            sender.sendMessage(ChatColor.YELLOW + "/settax <税率> - 设置转账税率 (0-1之间, 如0.05表示5%)");
            sender.sendMessage(ChatColor.YELLOW + "/teleportadmin reload - 重载配置");
            sender.sendMessage(ChatColor.YELLOW + "/teleportadmin info - 查看当前配置");
            sender.sendMessage(ChatColor.YELLOW + "/teleportadmin migrate - 强制重新迁移旧数据");
            sender.sendMessage(ChatColor.YELLOW + "/teleportadmin status - 查看迁移状态");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                plugin.getConfigManager().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "配置已重新加载!");
                return true;
            case "info":
                FileConfiguration config = plugin.getConfig();
                sender.sendMessage(ChatColor.GREEN + "=== 当前配置 ===");
                sender.sendMessage(ChatColor.YELLOW + "RTP价格: " + ChatColor.GOLD + config.getDouble("rtp.price", 100.0));
                sender.sendMessage(ChatColor.YELLOW + "每日免费RTP次数: " + ChatColor.GOLD + config.getInt("rtp.daily-free-count", 3));
                sender.sendMessage(ChatColor.YELLOW + "最大家数量: " + ChatColor.GOLD + config.getInt("homes.max-homes", 3));
                sender.sendMessage(ChatColor.YELLOW + "转账税率: " + ChatColor.GOLD + (config.getDouble("economy.transfer-tax", 0.05) * 100) + "%");
                sender.sendMessage(ChatColor.YELLOW + "数据库: " + ChatColor.GOLD + (config.getBoolean("database.enabled", true) ? "启用" : "禁用"));
                sender.sendMessage(ChatColor.YELLOW + "经济系统: " + ChatColor.GOLD + (config.getBoolean("economy.enabled", true) ? "启用" : "禁用"));
                return true;
            case "migrate":
                if (plugin.getMigrationManager() == null) {
                    sender.sendMessage(ChatColor.RED + "数据库未启用，无法执行迁移!");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "正在执行数据迁移...");
                plugin.getMigrationManager().forceMigrate();
                sender.sendMessage(ChatColor.GREEN + "数据迁移完成!");
                return true;
            case "status":
                sender.sendMessage(ChatColor.GREEN + "=== 插件状态 ===");
                sender.sendMessage(ChatColor.YELLOW + "版本: " + ChatColor.GOLD + plugin.getDescription().getVersion());
                if (plugin.getMigrationManager() != null) {
                    sender.sendMessage(ChatColor.YELLOW + "数据迁移状态: " + ChatColor.GOLD + (plugin.getMigrationManager().isMigrated() ? "已完成" : "未完成"));
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "数据迁移状态: " + ChatColor.GOLD + "数据库未启用");
                }
                sender.sendMessage(ChatColor.YELLOW + "数据库: " + ChatColor.GOLD + (plugin.getDatabaseManager() != null ? "已连接" : "未连接"));
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "未知子命令! 使用 /teleportadmin 查看帮助");
                return true;
        }
    }

    private boolean setRtpPrice(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /setrtpprice <价格>");
            return true;
        }

        try {
            double price = Double.parseDouble(args[0]);
            if (price < 0) {
                sender.sendMessage(ChatColor.RED + "价格不能为负数!");
                return true;
            }

            FileConfiguration config = plugin.getConfig();
            config.set("rtp.price", price);
            plugin.saveConfig();
            plugin.getConfigManager().reloadConfig();

            sender.sendMessage(ChatColor.GREEN + "RTP价格已设置为: " + ChatColor.GOLD + price);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的价格数值!");
            return true;
        }
    }

    private boolean setRtpFreeCount(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /setrtpfree <次数>");
            return true;
        }

        try {
            int count = Integer.parseInt(args[0]);
            if (count < 0) {
                sender.sendMessage(ChatColor.RED + "次数不能为负数!");
                return true;
            }

            FileConfiguration config = plugin.getConfig();
            config.set("rtp.daily-free-count", count);
            plugin.saveConfig();
            plugin.getConfigManager().reloadConfig();

            sender.sendMessage(ChatColor.GREEN + "每日免费RTP次数已设置为: " + ChatColor.GOLD + count);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的次数数值!");
            return true;
        }
    }

    private boolean setMaxHomes(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /setmaxhomes <数量>");
            return true;
        }

        try {
            int max = Integer.parseInt(args[0]);
            if (max < 1) {
                sender.sendMessage(ChatColor.RED + "数量必须大于0!");
                return true;
            }

            FileConfiguration config = plugin.getConfig();
            config.set("homes.max-homes", max);
            plugin.saveConfig();
            plugin.getConfigManager().reloadConfig();

            sender.sendMessage(ChatColor.GREEN + "最大家数量已设置为: " + ChatColor.GOLD + max);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的数量数值!");
            return true;
        }
    }

    private boolean setTransferTax(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /settax <税率>");
            sender.sendMessage(ChatColor.YELLOW + "税率范围: 0-1 (如 0.05 表示 5%)");
            return true;
        }

        try {
            double tax = Double.parseDouble(args[0]);
            if (tax < 0 || tax > 1) {
                sender.sendMessage(ChatColor.RED + "税率必须在 0-1 之间!");
                sender.sendMessage(ChatColor.YELLOW + "如: 0.05 表示 5%, 0.1 表示 10%");
                return true;
            }

            FileConfiguration config = plugin.getConfig();
            config.set("economy.transfer-tax", tax);
            plugin.saveConfig();
            plugin.getConfigManager().reloadConfig();

            sender.sendMessage(ChatColor.GREEN + "转账税率已设置为: " + ChatColor.GOLD + (tax * 100) + "%");
            sender.sendMessage(ChatColor.YELLOW + "例如: 转账100金币，接收者将收到 " + (100 * (1 - tax)) + " 金币");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的税率数值!");
            return true;
        }
    }
}
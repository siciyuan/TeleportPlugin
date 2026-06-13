package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 保护区管理命令
 */
public class ProtectionCommand implements CommandExecutor {

    private final TeleportPlugin plugin;
    private final WorldProtectionListener protection;

    public ProtectionCommand(TeleportPlugin plugin, WorldProtectionListener protection) {
        this.plugin = plugin;
        this.protection = protection;
        plugin.getCommand("protection").setExecutor(this);
        plugin.getCommand("prot").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("teleport.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                return handleAdd(sender, args);
            case "remove":
            case "del":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender);
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender);
            case "enable":
                return handleEnable(sender, true);
            case "disable":
                return handleEnable(sender, false);
            case "setradius":
                return handleSetRadius(sender, args);
            case "toggle":
                return handleToggle(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== 世界主城保护管理 ===");
        sender.sendMessage(ChatColor.YELLOW + "/prot add <玩家> " + ChatColor.WHITE + "- 添加豁免玩家");
        sender.sendMessage(ChatColor.YELLOW + "/prot remove <玩家> " + ChatColor.WHITE + "- 移除豁免玩家");
        sender.sendMessage(ChatColor.YELLOW + "/prot list " + ChatColor.WHITE + "- 查看豁免列表");
        sender.sendMessage(ChatColor.YELLOW + "/prot info " + ChatColor.WHITE + "- 查看保护区信息");
        sender.sendMessage(ChatColor.YELLOW + "/prot setradius <半径> " + ChatColor.WHITE + "- 设置保护半径");
        sender.sendMessage(ChatColor.YELLOW + "/prot toggle <类型> " + ChatColor.WHITE + "- 切换保护开关");
        sender.sendMessage(ChatColor.YELLOW + "/prot enable " + ChatColor.WHITE + "- 启用保护");
        sender.sendMessage(ChatColor.YELLOW + "/prot disable " + ChatColor.WHITE + "- 禁用保护");
        sender.sendMessage(ChatColor.YELLOW + "/prot reload " + ChatColor.WHITE + "- 重载配置");
        sender.sendMessage(ChatColor.GOLD + "--- 保护类型 ---");
        sender.sendMessage(ChatColor.YELLOW + "block-break " + ChatColor.WHITE + "- 破坏方块");
        sender.sendMessage(ChatColor.YELLOW + "block-place " + ChatColor.WHITE + "- 放置方块");
        sender.sendMessage(ChatColor.YELLOW + "pvp " + ChatColor.WHITE + "- PvP战斗");
        sender.sendMessage(ChatColor.YELLOW + "entity-damage " + ChatColor.WHITE + "- 生物伤害");
        sender.sendMessage(ChatColor.YELLOW + "chest-access " + ChatColor.WHITE + "- 打开箱子");
        sender.sendMessage(ChatColor.YELLOW + "door-interact " + ChatColor.WHITE + "- 使用门/按钮");
        sender.sendMessage(ChatColor.YELLOW + "bucket-use " + ChatColor.WHITE + "- 使用桶/容器");
        sender.sendMessage(ChatColor.YELLOW + "explosion-damage " + ChatColor.WHITE + "- 爆炸伤害");
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /prot add <玩家名或UUID>");
            return true;
        }

        String playerName = args[1];
        if (protection.addAllowedPlayer(playerName)) {
            sender.sendMessage(ChatColor.GREEN + "已添加 " + ChatColor.GOLD + playerName + ChatColor.GREEN + " 到豁免列表");
        } else {
            sender.sendMessage(ChatColor.RED + "玩家已在豁免列表中!");
        }
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /prot remove <玩家名或UUID>");
            return true;
        }

        String playerName = args[1];
        if (protection.removeAllowedPlayer(playerName)) {
            sender.sendMessage(ChatColor.GREEN + "已从豁免列表移除 " + ChatColor.GOLD + playerName);
        } else {
            sender.sendMessage(ChatColor.RED + "玩家不在豁免列表中!");
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<String> allowedPlayers = protection.getAllowedPlayers();
        
        sender.sendMessage(ChatColor.GREEN + "=== 豁免玩家列表 ===");
        if (allowedPlayers.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "暂无豁免玩家（只有OP可以操作）");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "共 " + allowedPlayers.size() + " 名玩家:");
            for (String player : allowedPlayers) {
                sender.sendMessage(ChatColor.GOLD + "- " + player);
            }
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "配置已重新加载!");
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== 世界主城保护区信息 ===");
        
        boolean enabled = plugin.getConfig().getBoolean("world-protection.enabled", true);
        sender.sendMessage(ChatColor.YELLOW + "状态: " + (enabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        
        int radius = plugin.getConfig().getInt("world-protection.spawn-protection-radius", 100);
        sender.sendMessage(ChatColor.YELLOW + "保护半径: " + ChatColor.GOLD + radius + " 方块");
        
        var spawnLoc = plugin.getConfigManager().getSpawnLocation();
        if (spawnLoc != null) {
            sender.sendMessage(ChatColor.YELLOW + "主城位置: " + ChatColor.GOLD + 
                spawnLoc.getWorld().getName() + " (" + 
                spawnLoc.getBlockX() + ", " + spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ() + ")");
        } else {
            sender.sendMessage(ChatColor.RED + "主城未设置！请先使用 /setspawn");
        }
        
        sender.sendMessage(ChatColor.GOLD + "--- 保护开关 ---");
        sender.sendMessage(ChatColor.YELLOW + "破坏方块: " + getToggleStatus("block-break"));
        sender.sendMessage(ChatColor.YELLOW + "放置方块: " + getToggleStatus("block-place"));
        sender.sendMessage(ChatColor.YELLOW + "PVP: " + getToggleStatus("pvp"));
        sender.sendMessage(ChatColor.YELLOW + "生物伤害: " + getToggleStatus("entity-damage"));
        sender.sendMessage(ChatColor.YELLOW + "打开箱子: " + getToggleStatus("chest-access"));
        sender.sendMessage(ChatColor.YELLOW + "使用门/按钮: " + getToggleStatus("door-interact"));
        sender.sendMessage(ChatColor.YELLOW + "使用桶: " + getToggleStatus("bucket-use"));
        sender.sendMessage(ChatColor.YELLOW + "爆炸伤害: " + getToggleStatus("explosion-damage"));
        
        return true;
    }

    private String getToggleStatus(String key) {
        boolean enabled = plugin.getConfig().getBoolean("world-protection.protection." + key, true);
        return enabled ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭";
    }

    private boolean handleEnable(CommandSender sender, boolean enable) {
        plugin.getConfig().set("world-protection.enabled", enable);
        plugin.saveConfig();
        
        if (enable) {
            sender.sendMessage(ChatColor.GREEN + "世界主城保护已启用!");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "世界主城保护已禁用!");
        }
        return true;
    }

    private boolean handleSetRadius(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /prot setradius <半径>");
            return true;
        }

        try {
            int radius = Integer.parseInt(args[1]);
            if (radius < 0) {
                sender.sendMessage(ChatColor.RED + "半径不能为负数!");
                return true;
            }

            plugin.getConfig().set("world-protection.spawn-protection-radius", radius);
            plugin.saveConfig();
            sender.sendMessage(ChatColor.GREEN + "保护半径已设置为: " + ChatColor.GOLD + radius);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的半径数值!");
            return true;
        }
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /prot toggle <保护类型>");
            sender.sendMessage(ChatColor.YELLOW + "可用类型: block-break, block-place, pvp, entity-damage, chest-access, door-interact, bucket-use, explosion-damage");
            return true;
        }

        String protectionType = args[1].toLowerCase();
        String configPath = "world-protection.protection." + protectionType;
        
        if (!plugin.getConfig().contains(configPath)) {
            sender.sendMessage(ChatColor.RED + "未知的保护类型: " + protectionType);
            return true;
        }

        boolean current = plugin.getConfig().getBoolean(configPath, true);
        boolean newValue = !current;
        
        plugin.getConfig().set(configPath, newValue);
        plugin.saveConfig();
        
        String status = newValue ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭";
        sender.sendMessage(ChatColor.GREEN + protectionType + " 已设置为: " + status);
        
        return true;
    }
}
package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {

    private final TeleportPlugin plugin;

    public HomeCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("home").setExecutor(this);
        plugin.getCommand("sethome").setExecutor(this);
        plugin.getCommand("delhome").setExecutor(this);
        plugin.getCommand("homes").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("sethome")) {
            String homeName = args.length > 0 ? args[0] : "home";
            
            if (homeName == null || homeName.isEmpty()) {
                player.sendMessage(ChatColor.RED + "家名不能为空!");
                return true;
            }

            if (homeName.length() > 32) {
                player.sendMessage(ChatColor.RED + "家名不能超过32个字符!");
                return true;
            }

            if (!player.hasPermission("teleport.sethome")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }

            Location location = player.getLocation();
            if (location.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "无效的位置!");
                return true;
            }

            if (plugin.getHomeManager().setHome(player.getUniqueId(), homeName, location)) {
                player.sendMessage(ChatColor.GREEN + "家 \"" + homeName + "\" 已设置!");
            } else {
                player.sendMessage(ChatColor.RED + "家的数量已达到上限!");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("home")) {
            String homeName = args.length > 0 ? args[0] : "home";

            if (homeName == null || homeName.isEmpty()) {
                player.sendMessage(ChatColor.RED + "家名不能为空!");
                return true;
            }

            if (!player.hasPermission("teleport.home")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }

            Location home = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
            if (home == null) {
                player.sendMessage(ChatColor.RED + "家 \"" + homeName + "\" 不存在!");
                return true;
            }

            if (home.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "家所在的世界不存在!");
                plugin.getHomeManager().removeHome(player.getUniqueId(), homeName);
                return true;
            }

            try {
                player.teleport(home);
                player.sendMessage(ChatColor.GREEN + "已传送到家 \"" + homeName + "\"!");
            } catch (Exception e) {
                plugin.getLogger().warning("Home teleport failed: " + e.getMessage());
                player.sendMessage(ChatColor.RED + "传送失败!");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("delhome")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "用法: /delhome <家名>");
                return true;
            }
            String homeName = args[0];

            if (homeName == null || homeName.isEmpty()) {
                player.sendMessage(ChatColor.RED + "家名不能为空!");
                return true;
            }

            if (!player.hasPermission("teleport.sethome")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }

            if (plugin.getHomeManager().removeHome(player.getUniqueId(), homeName)) {
                player.sendMessage(ChatColor.GREEN + "家 \"" + homeName + "\" 已删除!");
            } else {
                player.sendMessage(ChatColor.RED + "家 \"" + homeName + "\" 不存在!");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("homes")) {
            if (!player.hasPermission("teleport.home")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }
            var homes = plugin.getHomeManager().getHomes(player.getUniqueId());
            synchronized (homes) {
                if (homes.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "你没有设置任何家!");
                } else {
                    player.sendMessage(ChatColor.GREEN + "你的家: " + String.join(", ", homes.keySet()));
                }
            }
            return true;
        }

        return false;
    }
}
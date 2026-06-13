package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final TeleportPlugin plugin;

    public SpawnCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("spawn").setExecutor(this);
        plugin.getCommand("setspawn").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("spawn")) {
            Location spawn = plugin.getConfigManager().getSpawnLocation();
            if (spawn == null) {
                player.sendMessage(ChatColor.RED + "尚未设置主城!");
                return true;
            }

            if (spawn.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "主城世界不存在!");
                return true;
            }

            try {
                player.teleport(spawn);
                player.sendMessage(ChatColor.GREEN + "已传送到主城!");
            } catch (Exception e) {
                plugin.getLogger().warning("Spawn teleport failed: " + e.getMessage());
                player.sendMessage(ChatColor.RED + "传送失败!");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("setspawn")) {
            if (!player.hasPermission("teleport.admin")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }
            Location location = player.getLocation();
            if (location.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "无效的位置!");
                return true;
            }
            plugin.getConfigManager().saveSpawnLocation(location);
            location.getWorld().setSpawnLocation(location);
            player.sendMessage(ChatColor.GREEN + "主城已设置!");
            return true;
        }

        return false;
    }
}
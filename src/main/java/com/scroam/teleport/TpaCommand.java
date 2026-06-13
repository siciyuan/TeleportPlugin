package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpaCommand implements CommandExecutor {

    private final TeleportPlugin plugin;

    public TpaCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("tpa").setExecutor(this);
        plugin.getCommand("tpahere").setExecutor(this);
        plugin.getCommand("tpaccept").setExecutor(this);
        plugin.getCommand("tpdeny").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("tpa")) {
            if (!player.hasPermission("teleport.tpa")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }

            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "用法: /tpa <玩家名>");
                return true;
            }

            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                player.sendMessage(ChatColor.RED + "玩家不存在或已离线!");
                return true;
            }

            if (target.equals(player)) {
                player.sendMessage(ChatColor.RED + "你不能向自己发送传送请求!");
                return true;
            }

            // 检查是否有待处理的请求
            if (plugin.getTpaManager().hasPendingRequest(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "你还有一个待处理的传送请求，请等待处理。");
                return true;
            }

            // 创建传送请求
            plugin.getTpaManager().addRequest(player.getUniqueId(), target.getUniqueId(), false);
            player.sendMessage(ChatColor.GREEN + "已向 " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + " 发送传送请求!");
            
            // 发送给目标玩家（带按钮提示）
            target.sendMessage("");
            target.sendMessage(ChatColor.GOLD + "玩家 " + player.getName() + " 请求传送到你身边!");
            target.sendMessage(ChatColor.GRAY + "点击按钮或使用命令: " + 
                             ChatColor.GREEN + "[接受] " + ChatColor.RED + "[拒绝]");
            target.sendMessage(ChatColor.YELLOW + "使用 " + ChatColor.WHITE + "/tpaccept" + 
                             ChatColor.YELLOW + " 接受 或 " + ChatColor.WHITE + "/tpdeny" + 
                             ChatColor.YELLOW + " 拒绝");
            target.sendMessage("");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpahere")) {
            if (!player.hasPermission("teleport.tpahere")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }

            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "用法: /tpahere <玩家名>");
                return true;
            }

            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                player.sendMessage(ChatColor.RED + "玩家不存在或已离线!");
                return true;
            }

            if (target.equals(player)) {
                player.sendMessage(ChatColor.RED + "你不能向自己发送传送请求!");
                return true;
            }

            // 检查是否有待处理的请求
            if (plugin.getTpaManager().hasPendingRequest(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "你还有一个待处理的传送请求，请等待处理。");
                return true;
            }

            // 创建传送请求
            plugin.getTpaManager().addRequest(player.getUniqueId(), target.getUniqueId(), true);
            player.sendMessage(ChatColor.GREEN + "已请求 " + ChatColor.GOLD + target.getName() + 
                             ChatColor.GREEN + " 传送到你的位置!");
            
            // 发送给目标玩家
            target.sendMessage("");
            target.sendMessage(ChatColor.GOLD + "玩家 " + player.getName() + " 请求传送到你的位置!");
            target.sendMessage(ChatColor.GRAY + "点击按钮或使用命令: " + 
                             ChatColor.GREEN + "[接受] " + ChatColor.RED + "[拒绝]");
            target.sendMessage(ChatColor.YELLOW + "使用 " + ChatColor.WHITE + "/tpaccept" + 
                             ChatColor.YELLOW + " 接受 或 " + ChatColor.WHITE + "/tpdeny" + 
                             ChatColor.YELLOW + " 拒绝");
            target.sendMessage("");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpaccept")) {
            if (!player.hasPermission("teleport.tpaccept")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }

            TpaManager.TpaRequest request = plugin.getTpaManager().getRequest(player.getUniqueId());
            if (request == null) {
                player.sendMessage(ChatColor.RED + "没有待处理的传送请求!");
                return true;
            }

            Player from = Bukkit.getPlayer(request.getRequesterId());
            if (from == null) {
                player.sendMessage(ChatColor.RED + "发送请求的玩家已离线!");
                plugin.getTpaManager().removeRequest(player.getUniqueId());
                return true;
            }

            if (request.isTpHere()) {
                // 目标传送到请求者
                from.sendMessage(ChatColor.GREEN + player.getName() + " 已接受你的请求，正在传送...");
                from.teleport(player.getLocation());
            } else {
                // 请求者传送到目标
                player.sendMessage(ChatColor.GREEN + "已接受 " + from.getName() + " 的传送请求!");
                player.sendMessage(ChatColor.YELLOW + "正在传送到 " + from.getName() + "...");
                player.teleport(from.getLocation());
            }

            plugin.getTpaManager().removeRequest(player.getUniqueId());
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpdeny")) {
            if (!player.hasPermission("teleport.tpdeny")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }

            TpaManager.TpaRequest request = plugin.getTpaManager().getRequest(player.getUniqueId());
            if (request == null) {
                player.sendMessage(ChatColor.RED + "没有待处理的传送请求!");
                return true;
            }

            Player from = Bukkit.getPlayer(request.getRequesterId());
            if (from != null) {
                from.sendMessage(ChatColor.RED + player.getName() + " 拒绝了你的传送请求!");
            }

            player.sendMessage(ChatColor.YELLOW + "已拒绝传送请求。");
            plugin.getTpaManager().removeRequest(player.getUniqueId());
            return true;
        }

        return true;
    }
}

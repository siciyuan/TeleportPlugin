package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

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
            if (targetName == null || targetName.isEmpty() || targetName.length() > 16) {
                player.sendMessage(ChatColor.RED + "无效的玩家名!");
                return true;
            }

            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "玩家 " + targetName + " 不存在或不在线!");
                return true;
            }

            if (player.equals(target)) {
                player.sendMessage(ChatColor.RED + "你不能传送到自己!");
                return true;
            }

            if (plugin.getTpaManager().hasPendingRequest(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "该玩家已有待处理的传送请求!");
                return true;
            }

            plugin.getTpaManager().addRequest(player.getUniqueId(), target.getUniqueId(), false);
            player.sendMessage(ChatColor.GREEN + "传送请求已发送给 " + target.getName() + "!");
            
            // 发送带点击按钮的消息
            sendTpaRequestMessage(target, player.getName(), false);
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
            if (targetName == null || targetName.isEmpty() || targetName.length() > 16) {
                player.sendMessage(ChatColor.RED + "无效的玩家名!");
                return true;
            }

            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "玩家 " + targetName + " 不存在或不在线!");
                return true;
            }

            if (player.equals(target)) {
                player.sendMessage(ChatColor.RED + "你不能让自己传送到自己身边!");
                return true;
            }

            if (plugin.getTpaManager().hasPendingRequest(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "该玩家已有待处理的传送请求!");
                return true;
            }

            plugin.getTpaManager().addRequest(player.getUniqueId(), target.getUniqueId(), true);
            player.sendMessage(ChatColor.GREEN + "传送请求已发送给 " + target.getName() + "!");
            
            // 发送带点击按钮的消息
            sendTpaRequestMessage(target, player.getName(), true);
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

            Player requester = Bukkit.getPlayer(request.requesterId);
            if (requester == null || !requester.isOnline()) {
                player.sendMessage(ChatColor.RED + "请求者已离线!");
                plugin.getTpaManager().removeRequest(player.getUniqueId());
                return true;
            }

            if (requester.equals(player)) {
                player.sendMessage(ChatColor.RED + "你不能传送到自己!");
                plugin.getTpaManager().removeRequest(player.getUniqueId());
                return true;
            }

            Location targetLoc = request.isTpHere ? requester.getLocation() : player.getLocation();
            if (targetLoc.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "目标位置无效!");
                plugin.getTpaManager().removeRequest(player.getUniqueId());
                return true;
            }

            plugin.getTpaManager().removeRequest(player.getUniqueId());

            try {
                if (request.isTpHere) {
                    player.teleport(targetLoc);
                    player.sendMessage(ChatColor.GREEN + "已传送到 " + requester.getName() + " 身边!");
                    requester.sendMessage(ChatColor.GREEN + player.getName() + " 已传送到你身边!");
                } else {
                    requester.teleport(targetLoc);
                    requester.sendMessage(ChatColor.GREEN + "传送请求已被接受!");
                    player.sendMessage(ChatColor.GREEN + "已允许 " + requester.getName() + " 传送到你身边!");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Teleport failed: " + e.getMessage());
                player.sendMessage(ChatColor.RED + "传送失败!");
                if (requester.isOnline()) {
                    requester.sendMessage(ChatColor.RED + "传送失败!");
                }
            }

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

            Player requester = Bukkit.getPlayer(request.requesterId);
            if (requester != null && requester.isOnline()) {
                requester.sendMessage(ChatColor.RED + "传送请求被拒绝!");
            }

            plugin.getTpaManager().removeRequest(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "已拒绝传送请求!");
            return true;
        }

        return false;
    }

    /**
     * 发送带点击按钮的TPA请求消息
     */
    private void sendTpaRequestMessage(Player target, String requesterName, boolean isTpHere) {
        // 消息前缀
        TextComponent prefix = new TextComponent(ChatColor.YELLOW + (isTpHere ? requesterName + " 请求你传送到他身边！" : requesterName + " 请求传送到你身边！"));
        
        // 接受按钮
        TextComponent acceptButton = new TextComponent(ChatColor.GREEN + "[接受]");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(ChatColor.GREEN + "点击接受传送请求").create()));
        
        // 分隔符
        TextComponent separator = new TextComponent(ChatColor.GRAY + " ");
        
        // 拒绝按钮
        TextComponent denyButton = new TextComponent(ChatColor.RED + "[拒绝]");
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"));
        denyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(ChatColor.RED + "点击拒绝传送请求").create()));
        
        // 组合消息
        TextComponent message = new TextComponent();
        message.addExtra(prefix);
        message.addExtra("\n");
        message.addExtra(ChatColor.YELLOW + "点击按钮或使用命令: ");
        message.addExtra(acceptButton);
        message.addExtra(separator);
        message.addExtra(denyButton);
        
        target.spigot().sendMessage(message);
    }
}
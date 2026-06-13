package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 地标命令处理类
 */
public class WarpCommand implements CommandExecutor {
    private final TeleportPlugin plugin;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public WarpCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        WarpManager warpManager = plugin.getWarpManager();
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCmd = args[0].toLowerCase();
        
        switch (subCmd) {
            case "create":
            case "set":
                return handleCreate(player, args, warpManager);
            case "delete":
            case "del":
            case "remove":
                return handleDelete(player, args, warpManager);
            case "tp":
            case "go":
            case "teleport":
                return handleTeleport(player, args, warpManager);
            case "list":
            case "ls":
                return handleList(player, args, warpManager);
            case "info":
                return handleInfo(player, args, warpManager);
            case "price":
            case "setprice":
                return handleSetPrice(player, args, warpManager);
            case "my":
            case "mine":
                return handleMyWarps(player, warpManager);
            default:
                sendHelp(player);
                return true;
        }
    }
    
    /**
     * 创建地标
     */
    private boolean handleCreate(Player player, String[] args, WarpManager warpManager) {
        if (!player.hasPermission("teleport.warp.create")) {
            player.sendMessage(ChatColor.RED + "你没有权限创建地标！");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /warp create <名称>");
            return true;
        }
        
        String name = args[1];
        String result = warpManager.createWarp(player, name);
        
        if (result.contains("成功")) {
            player.sendMessage(ChatColor.GREEN + result);
        } else {
            player.sendMessage(ChatColor.RED + result);
        }
        
        return true;
    }
    
    /**
     * 删除地标
     */
    private boolean handleDelete(Player player, String[] args, WarpManager warpManager) {
        if (!player.hasPermission("teleport.warp.delete")) {
            player.sendMessage(ChatColor.RED + "你没有权限删除地标！");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /warp delete <名称>");
            return true;
        }
        
        String name = args[1];
        String result = warpManager.deleteWarp(player, name);
        
        if (result.contains("已删除")) {
            player.sendMessage(ChatColor.GREEN + result);
        } else {
            player.sendMessage(ChatColor.RED + result);
        }
        
        return true;
    }
    
    /**
     * 传送到地标
     */
    private boolean handleTeleport(Player player, String[] args, WarpManager warpManager) {
        if (!player.hasPermission("teleport.warp.teleport")) {
            player.sendMessage(ChatColor.RED + "你没有权限传送到地标！");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /warp tp <名称>");
            return true;
        }
        
        String name = args[1];
        String result = warpManager.teleportToWarp(player, name);
        
        if (result.contains("已传送")) {
            player.sendMessage(ChatColor.GREEN + result);
        } else {
            player.sendMessage(ChatColor.RED + result);
        }
        
        return true;
    }
    
    /**
     * 列出地标
     */
    private boolean handleList(Player player, String[] args, WarpManager warpManager) {
        if (!player.hasPermission("teleport.warp.list")) {
            player.sendMessage(ChatColor.RED + "你没有权限查看地标列表！");
            return true;
        }
        
        List<Warp> warps = warpManager.getPublicWarps();
        
        if (warps.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "暂无公开地标！");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "========== 公开地标列表 ==========");
        for (Warp warp : warps) {
            String priceStr = warp.getTeleportPrice() > 0 ? 
                    ChatColor.GOLD + " (" + String.format("%.2f", warp.getTeleportPrice()) + " 金币)" : 
                    ChatColor.GREEN + " (免费)";
            player.sendMessage(ChatColor.WHITE + "- " + warp.getName() + 
                    ChatColor.GRAY + " [" + warp.getOwnerName() + "]" + priceStr);
        }
        
        return true;
    }
    
    /**
     * 查看地标信息
     */
    private boolean handleInfo(Player player, String[] args, WarpManager warpManager) {
        if (!player.hasPermission("teleport.warp.info")) {
            player.sendMessage(ChatColor.RED + "你没有权限查看地标信息！");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /warp info <名称>");
            return true;
        }
        
        String name = args[1];
        Warp warp = warpManager.getWarp(name);
        
        if (warp == null) {
            player.sendMessage(ChatColor.RED + "地标 '" + name + "' 不存在！");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "========== 地标信息 ==========");
        player.sendMessage(ChatColor.YELLOW + "名称: " + ChatColor.WHITE + warp.getName());
        player.sendMessage(ChatColor.YELLOW + "创建者: " + ChatColor.WHITE + warp.getOwnerName());
        player.sendMessage(ChatColor.YELLOW + "世界: " + ChatColor.WHITE + warp.getWorld().getName());
        player.sendMessage(ChatColor.YELLOW + "坐标: " + ChatColor.WHITE + 
                String.format("%.2f, %.2f, %.2f", warp.getX(), warp.getY(), warp.getZ()));
        player.sendMessage(ChatColor.YELLOW + "传送价格: " + 
                (warp.getTeleportPrice() > 0 ? ChatColor.GOLD + String.format("%.2f", warp.getTeleportPrice()) + " 金币" : ChatColor.GREEN + "免费"));
        player.sendMessage(ChatColor.YELLOW + "创建时间: " + ChatColor.GRAY + sdf.format(new Date(warp.getCreatedTime())));
        
        return true;
    }
    
    /**
     * 设置地标价格
     */
    private boolean handleSetPrice(Player player, String[] args, WarpManager warpManager) {
        if (!player.hasPermission("teleport.warp.setprice")) {
            player.sendMessage(ChatColor.RED + "你没有权限设置地标价格！");
            return true;
        }
        
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "用法: /warp price <名称> <价格>");
            return true;
        }
        
        String name = args[1];
        double price;
        
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "价格必须是数字！");
            return true;
        }
        
        String result = warpManager.setWarpPrice(player, name, price);
        
        if (result.contains("已设置")) {
            player.sendMessage(ChatColor.GREEN + result);
        } else {
            player.sendMessage(ChatColor.RED + result);
        }
        
        return true;
    }
    
    /**
     * 查看自己的地标
     */
    private boolean handleMyWarps(Player player, WarpManager warpManager) {
        if (!player.hasPermission("teleport.warp.my")) {
            player.sendMessage(ChatColor.RED + "你没有权限查看自己的地标！");
            return true;
        }
        
        Set<String> myWarps = warpManager.getPlayerWarps(player.getUniqueId());
        
        if (myWarps.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "你还没有创建任何地标！");
            player.sendMessage(ChatColor.YELLOW + "创建免费次数: " + warpManager.getCreateFreeCount(player.getUniqueId()));
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "========== 我的地标 ==========");
        for (String warpName : myWarps) {
            Warp warp = warpManager.getWarp(warpName);
            if (warp != null) {
                String priceStr = warp.getTeleportPrice() > 0 ? 
                        ChatColor.GOLD + " (" + String.format("%.2f", warp.getTeleportPrice()) + " 金币)" : 
                        ChatColor.GREEN + " (免费)";
                player.sendMessage(ChatColor.WHITE + "- " + warp.getName() + priceStr);
            }
        }
        
        player.sendMessage(ChatColor.YELLOW + "创建免费次数: " + warpManager.getCreateFreeCount(player.getUniqueId()));
        player.sendMessage(ChatColor.YELLOW + "传送免费次数: " + warpManager.getTeleportFreeCount(player.getUniqueId()));
        
        return true;
    }
    
    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== 地标系统帮助 ==========");
        player.sendMessage(ChatColor.YELLOW + "/warp create <名称> " + ChatColor.GRAY + "- 创建地标");
        player.sendMessage(ChatColor.YELLOW + "/warp delete <名称> " + ChatColor.GRAY + "- 删除地标");
        player.sendMessage(ChatColor.YELLOW + "/warp tp <名称> " + ChatColor.GRAY + "- 传送到地标");
        player.sendMessage(ChatColor.YELLOW + "/warp list " + ChatColor.GRAY + "- 查看公开地标列表");
        player.sendMessage(ChatColor.YELLOW + "/warp info <名称> " + ChatColor.GRAY + "- 查看地标信息");
        player.sendMessage(ChatColor.YELLOW + "/warp price <名称> <价格> " + ChatColor.GRAY + "- 设置传送价格");
        player.sendMessage(ChatColor.YELLOW + "/warp my " + ChatColor.GRAY + "- 查看自己的地标");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "--- 费用说明 ---");
        player.sendMessage(ChatColor.GRAY + "创建地标: " + plugin.getConfig().getDouble("warp.create.price", 100.0) + " 金币");
        player.sendMessage(ChatColor.GRAY + "创建免费次数: " + plugin.getConfig().getInt("warp.create.free_count", 0));
        player.sendMessage(ChatColor.GRAY + "传送免费次数: " + plugin.getConfig().getInt("warp.teleport.free_count", 3));
    }
}
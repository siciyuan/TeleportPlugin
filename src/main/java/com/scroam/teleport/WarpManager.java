package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 地标管理类
 */
public class WarpManager {
    private final TeleportPlugin plugin;
    private final Map<String, Warp> warps;
    private final Map<UUID, Set<String>> playerWarps;
    private final Map<UUID, Integer> playerCreateFreeCount;
    private final Map<UUID, Integer> playerTeleportFreeCount;
    
    public WarpManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        this.warps = new ConcurrentHashMap<>();
        this.playerWarps = new ConcurrentHashMap<>();
        this.playerCreateFreeCount = new ConcurrentHashMap<>();
        this.playerTeleportFreeCount = new ConcurrentHashMap<>();
        loadWarps();
    }
    
    /**
     * 从数据库加载地标
     */
    private void loadWarps() {
        List<Warp> loadedWarps = plugin.getDatabaseManager().loadWarps();
        for (Warp warp : loadedWarps) {
            warps.put(warp.getName().toLowerCase(), warp);
            playerWarps.computeIfAbsent(warp.getOwnerId(), k -> new HashSet<>()).add(warp.getName().toLowerCase());
        }
        plugin.getLogger().info("Loaded " + warps.size() + " warps from database.");
    }
    
    /**
     * 创建地标
     */
    public String createWarp(Player player, String name) {
        if (warps.containsKey(name.toLowerCase())) {
            return "地标名称 '" + name + "' 已存在！";
        }
        
        // 检查免费次数
        int freeCount = getCreateFreeCount(player.getUniqueId());
        double createPrice = plugin.getConfig().getDouble("warp.create.price", 100.0);
        
        if (freeCount <= 0 && createPrice > 0) {
            // 需要付费
            if (!plugin.getEconomy().has(player, createPrice)) {
                return "你没有足够的金币创建地标！需要 " + String.format("%.2f", createPrice) + " 金币。";
            }
            plugin.getEconomy().withdrawPlayer(player, createPrice);
        } else {
            // 使用免费次数
            useCreateFreeCount(player.getUniqueId());
        }
        
        Warp warp = new Warp(name, player.getLocation(), player.getUniqueId(), player.getName());
        warps.put(name.toLowerCase(), warp);
        playerWarps.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(name.toLowerCase());
        
        // 保存到数据库
        plugin.getDatabaseManager().saveWarp(warp);
        
        String message = "地标 '" + name + "' 创建成功！";
        if (freeCount > 0) {
            message += " (免费次数剩余: " + (freeCount - 1) + ")";
        } else if (createPrice > 0) {
            message += " (花费: " + String.format("%.2f", createPrice) + " 金币)";
        }
        return message;
    }
    
    /**
     * 删除地标
     */
    public String deleteWarp(Player player, String name) {
        Warp warp = warps.get(name.toLowerCase());
        if (warp == null) {
            return "地标 '" + name + "' 不存在！";
        }
        
        if (!warp.isOwner(player.getUniqueId()) && !player.hasPermission("teleport.admin")) {
            return "你只能删除自己的地标！";
        }
        
        warps.remove(name.toLowerCase());
        Set<String> playerWarpSet = playerWarps.get(warp.getOwnerId());
        if (playerWarpSet != null) {
            playerWarpSet.remove(name.toLowerCase());
        }
        
        plugin.getDatabaseManager().deleteWarp(name);
        
        return "地标 '" + name + "' 已删除！";
    }
    
    /**
     * 传送到地标
     */
    public String teleportToWarp(Player player, String name) {
        Warp warp = warps.get(name.toLowerCase());
        if (warp == null) {
            return "地标 '" + name + "' 不存在！";
        }
        
        // 检查免费次数
        int freeCount = getTeleportFreeCount(player.getUniqueId());
        double teleportPrice = warp.getTeleportPrice();
        
        if (freeCount <= 0 && teleportPrice > 0) {
            // 需要付费
            if (!plugin.getEconomy().has(player, teleportPrice)) {
                return "你没有足够的金币传送到此地标！需要 " + String.format("%.2f", teleportPrice) + " 金币。";
            }
            plugin.getEconomy().withdrawPlayer(player, teleportPrice);
        } else {
            // 使用免费次数
            useTeleportFreeCount(player.getUniqueId());
        }
        
        player.teleport(warp.getLocation());
        
        String message = "已传送到地标 '" + warp.getName() + "'！";
        if (freeCount > 0) {
            message += " (免费次数剩余: " + (freeCount - 1) + ")";
        } else if (teleportPrice > 0) {
            message += " (花费: " + String.format("%.2f", teleportPrice) + " 金币)";
        }
        return message;
    }
    
    /**
     * 设置地标传送价格
     */
    public String setWarpPrice(Player player, String name, double price) {
        Warp warp = warps.get(name.toLowerCase());
        if (warp == null) {
            return "地标 '" + name + "' 不存在！";
        }
        
        if (!warp.isOwner(player.getUniqueId()) && !player.hasPermission("teleport.admin")) {
            return "你只能设置自己的地标价格！";
        }
        
        warp.setTeleportPrice(price);
        plugin.getDatabaseManager().saveWarp(warp);
        
        return "地标 '" + name + "' 的传送价格已设置为 " + String.format("%.2f", price) + " 金币！";
    }
    
    /**
     * 获取所有公开地标
     */
    public List<Warp> getPublicWarps() {
        List<Warp> publicWarps = new ArrayList<>();
        for (Warp warp : warps.values()) {
            if (warp.isPublic()) {
                publicWarps.add(warp);
            }
        }
        return publicWarps;
    }
    
    /**
     * 获取玩家的地标
     */
    public Set<String> getPlayerWarps(UUID playerId) {
        return playerWarps.getOrDefault(playerId, new HashSet<>());
    }
    
    /**
     * 获取地标
     */
    public Warp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }
    
    /**
     * 获取创建免费次数
     */
    public int getCreateFreeCount(UUID playerId) {
        int defaultFree = plugin.getConfig().getInt("warp.create.free_count", 0);
        int used = playerCreateFreeCount.getOrDefault(playerId, 0);
        return defaultFree - used;
    }
    
    /**
     * 使用创建免费次数
     */
    public void useCreateFreeCount(UUID playerId) {
        playerCreateFreeCount.merge(playerId, 1, Integer::sum);
        plugin.getDatabaseManager().saveWarpFreeCount(playerId, 
                playerCreateFreeCount.getOrDefault(playerId, 0),
                playerTeleportFreeCount.getOrDefault(playerId, 0));
    }
    
    /**
     * 获取传送免费次数
     */
    public int getTeleportFreeCount(UUID playerId) {
        int defaultFree = plugin.getConfig().getInt("warp.teleport.free_count", 3);
        int used = playerTeleportFreeCount.getOrDefault(playerId, 0);
        return defaultFree - used;
    }
    
    /**
     * 使用传送免费次数
     */
    public void useTeleportFreeCount(UUID playerId) {
        playerTeleportFreeCount.merge(playerId, 1, Integer::sum);
        plugin.getDatabaseManager().saveWarpFreeCount(playerId,
                playerCreateFreeCount.getOrDefault(playerId, 0),
                playerTeleportFreeCount.getOrDefault(playerId, 0));
    }
    
    /**
     * 加载玩家的免费次数
     */
    public void loadPlayerFreeCount(UUID playerId, int createUsed, int teleportUsed) {
        playerCreateFreeCount.put(playerId, createUsed);
        playerTeleportFreeCount.put(playerId, teleportUsed);
    }
}
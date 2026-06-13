package com.scroam.teleport;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 地标管理类
 */
public class LandmarkManager {
    private final TeleportPlugin plugin;
    private final Map<String, Landmark> landmarks; // id -> Landmark
    private final Map<UUID, Set<String>> playerLandmarks; // playerId -> Set<landmarkId>

    public LandmarkManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        this.landmarks = new ConcurrentHashMap<>();
        this.playerLandmarks = new ConcurrentHashMap<>();
        
        // 从数据库加载地标
        loadLandmarks();
    }

    /**
     * 从数据库加载地标
     */
    private void loadLandmarks() {
        if (plugin.getDatabaseManager() != null) {
            List<Landmark> loadedLandmarks = plugin.getDatabaseManager().loadLandmarks();
            for (Landmark landmark : loadedLandmarks) {
                landmarks.put(landmark.getId(), landmark);
                
                // 添加到玩家的地标集合
                Set<String> playerLm = playerLandmarks.computeIfAbsent(landmark.getOwnerId(), k -> new HashSet<>());
                playerLm.add(landmark.getId());
            }
            plugin.getLogger().info("Loaded " + landmarks.size() + " landmarks from database.");
        }
    }

    /**
     * 创建地标
     */
    public String createLandmark(Player player, String name, double teleportPrice) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        
        if (world == null) {
            return "无效的世界!";
        }
        
        // 检查名称是否已存在
        for (Landmark lm : landmarks.values()) {
            if (lm.getName().equalsIgnoreCase(name)) {
                return "地标名称已存在!";
            }
        }
        
        // 生成唯一ID
        String id = UUID.randomUUID().toString().substring(0, 8);
        
        // 创建地标
        Landmark landmark = new Landmark(id, player.getUniqueId(), name, world, 
                loc.getX(), loc.getY(), loc.getZ(), teleportPrice);
        
        landmarks.put(id, landmark);
        
        // 添加到玩家的地标集合
        Set<String> playerLm = playerLandmarks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        playerLm.add(id);
        
        // 保存到数据库
        if (plugin.getDatabaseManager() != null) {
            plugin.getDatabaseManager().saveLandmark(landmark);
        }
        
        return "地标创建成功! ID: " + id + ", 名称: " + name;
    }

    /**
     * 删除地标
     */
    public String deleteLandmark(Player player, String name) {
        Landmark landmark = getLandmarkByName(name);
        
        if (landmark == null) {
            return "地标不存在!";
        }
        
        // 检查权限（只有地主或管理员可以删除）
        if (!landmark.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("teleport.landmark.admin")) {
            return "你没有权限删除这个地标!";
        }
        
        landmarks.remove(landmark.getId());
        
        // 从玩家的地标集合中移除
        Set<String> playerLm = playerLandmarks.get(landmark.getOwnerId());
        if (playerLm != null) {
            playerLm.remove(landmark.getId());
        }
        
        // 从数据库删除
        if (plugin.getDatabaseManager() != null) {
            plugin.getDatabaseManager().deleteLandmark(landmark.getId());
        }
        
        return "地标已删除: " + name;
    }

    /**
     * 传送到地标
     */
    public Landmark teleportToLandmark(Player player, String name) {
        Landmark landmark = getLandmarkByName(name);
        
        if (landmark == null) {
            return null;
        }
        
        Location loc = landmark.getLocation();
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        
        player.teleport(loc);
        return landmark;
    }

    /**
     * 根据名称获取地标
     */
    public Landmark getLandmarkByName(String name) {
        for (Landmark lm : landmarks.values()) {
            if (lm.getName().equalsIgnoreCase(name)) {
                return lm;
            }
        }
        return null;
    }

    /**
     * 根据ID获取地标
     */
    public Landmark getLandmarkById(String id) {
        return landmarks.get(id);
    }

    /**
     * 获取所有地标
     */
    public Collection<Landmark> getAllLandmarks() {
        return landmarks.values();
    }

    /**
     * 获取玩家的地标
     */
    public Set<Landmark> getPlayerLandmarks(UUID playerId) {
        Set<String> ids = playerLandmarks.get(playerId);
        if (ids == null) {
            return new HashSet<>();
        }
        
        Set<Landmark> result = new HashSet<>();
        for (String id : ids) {
            Landmark lm = landmarks.get(id);
            if (lm != null) {
                result.add(lm);
            }
        }
        return result;
    }

    /**
     * 设置地标传送价格
     */
    public String setTeleportPrice(Player player, String name, double price) {
        Landmark landmark = getLandmarkByName(name);
        
        if (landmark == null) {
            return "地标不存在!";
        }
        
        // 检查权限
        if (!landmark.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("teleport.landmark.admin")) {
            return "你没有权限修改这个地标!";
        }
        
        landmark.setTeleportPrice(price);
        
        // 保存到数据库
        if (plugin.getDatabaseManager() != null) {
            plugin.getDatabaseManager().saveLandmark(landmark);
        }
        
        return "传送价格已设置为: " + String.format("%.2f", price);
    }

    /**
     * 重命名地标
     */
    public String renameLandmark(Player player, String oldName, String newName) {
        Landmark landmark = getLandmarkByName(oldName);
        
        if (landmark == null) {
            return "地标不存在!";
        }
        
        // 检查权限
        if (!landmark.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("teleport.landmark.admin")) {
            return "你没有权限修改这个地标!";
        }
        
        // 检查新名称是否已存在
        for (Landmark lm : landmarks.values()) {
            if (lm.getName().equalsIgnoreCase(newName) && !lm.getId().equals(landmark.getId())) {
                return "地标名称已存在!";
            }
        }
        
        landmark.setName(newName);
        
        // 保存到数据库
        if (plugin.getDatabaseManager() != null) {
            plugin.getDatabaseManager().saveLandmark(landmark);
        }
        
        return "地标已重命名为: " + newName;
    }
}
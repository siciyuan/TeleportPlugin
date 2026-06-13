package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 地标管理器
 */
public class WaypointManager {
    private final TeleportPlugin plugin;
    private final Map<String, Waypoint> waypoints = new ConcurrentHashMap<>();  // waypointId -> Waypoint
    private final Map<String, Set<String>> playerWaypoints = new ConcurrentHashMap<>();  // playerId -> Set<waypointId>
    private final Map<String, Set<UUID>> pendingPermissions = new ConcurrentHashMap<>();  // waypointId -> Set<playerId>

    public WaypointManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        loadWaypoints();
    }

    private void loadWaypoints() {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) {
            plugin.getLogger().warning("Database not available, waypoints will not be loaded!");
            return;
        }

        try {
            ResultSet rs = db.getWaypoints();
            while (rs != null && rs.next()) {
                String waypointId = rs.getString("id");
                Waypoint waypoint = new Waypoint(
                        waypointId,
                        rs.getString("name"),
                        UUID.fromString(rs.getString("creator_uuid")),
                        rs.getString("world_name"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"),
                        rs.getInt("create_count"),
                        rs.getInt("teleport_count"),
                        rs.getDouble("create_price"),
                        rs.getDouble("teleport_price"),
                        rs.getBoolean("requires_permission"),
                        rs.getLong("created_time")
                );

                waypoints.put(waypoint.getId(), waypoint);
                playerWaypoints.computeIfAbsent(waypoint.getCreator().toString(), k -> ConcurrentHashMap.newKeySet())
                        .add(waypointId);

                // 加载待批准权限
                String pending = rs.getString("pending_permissions");
                if (pending != null && !pending.isEmpty()) {
                    Set<UUID> pendingSet = new HashSet<>();
                    for (String uuidStr : pending.split(",")) {
                        if (!uuidStr.isEmpty()) {
                            pendingSet.add(UUID.fromString(uuidStr.trim()));
                        }
                    }
                    pendingPermissions.put(waypoint.getId(), pendingSet);
                }
            }
            plugin.getLogger().info("Loaded " + waypoints.size() + " waypoints!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load waypoints: " + e.getMessage());
        }
    }

    public boolean createWaypoint(Player player, String name, Location location) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) {
            player.sendMessage("§c数据库未启用，无法创建地标！");
            return false;
        }

        // 检查名称是否重复
        String waypointId = generateWaypointId(name, player.getWorld().getName());
        if (waypoints.containsKey(waypointId)) {
            player.sendMessage("§c该位置已存在同名地标！");
            return false;
        }

        Waypoint waypoint = new Waypoint(waypointId, name, player.getUniqueId(), location);
        waypoints.put(waypointId, waypoint);
        playerWaypoints.computeIfAbsent(player.getUniqueId().toString(), k -> ConcurrentHashMap.newKeySet())
                .add(waypointId);

        db.saveWaypoint(waypoint);
        player.sendMessage("§a地标 §e" + name + " §a已创建！");
        player.sendMessage("§7创建免费次数: §e" + waypoint.getCreateCount() + 
                          " §7| 传送免费次数: §e" + waypoint.getTeleportCount());
        return true;
    }

    public boolean deleteWaypoint(Player player, String waypointId) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            player.sendMessage("§c地标不存在！");
            return false;
        }

        // 只有创建者或管理员可以删除
        if (!waypoint.getCreator().equals(player.getUniqueId()) && 
            !player.hasPermission("teleport.admin")) {
            player.sendMessage("§c你不是这个地标的创建者！");
            return false;
        }

        waypoints.remove(waypointId);
        playerWaypoints.get(waypoint.getCreator().toString()).remove(waypointId);
        pendingPermissions.remove(waypointId);

        DatabaseManager db = plugin.getDatabaseManager();
        if (db != null) {
            db.deleteWaypoint(waypointId);
        }

        player.sendMessage("§a地标已删除！");
        return true;
    }

    public boolean teleportToWaypoint(Player player, String waypointId) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            player.sendMessage("§c地标不存在！");
            return false;
        }

        if (!waypoint.isValid()) {
            player.sendMessage("§c该地标所在世界不存在！");
            return false;
        }

        // 检查权限
        if (waypoint.isRequiresPermission() && 
            !waypoint.getCreator().equals(player.getUniqueId())) {
            
            Set<UUID> pending = pendingPermissions.get(waypointId);
            if (pending == null || !pending.contains(player.getUniqueId())) {
                player.sendMessage("§e该地标需要创建者授权，请先申请权限：§c/warp permit " + waypoint.getName());
                return false;
            }
        }

        // 处理传送次数
        if (waypoint.getTeleportCount() > 0) {
            // 使用免费次数
            waypoint.setTeleportCount(waypoint.getTeleportCount() - 1);
            player.sendMessage("§a已传送到 §e" + waypoint.getName() + " §a(剩余免费次数: " + waypoint.getTeleportCount() + ")");
            saveWaypoint(waypoint);
        } else if (waypoint.getTeleportPrice() > 0) {
            // 收费传送
            double balance = getPlayerBalance(player);
            if (balance < waypoint.getTeleportPrice()) {
                player.sendMessage("§c余额不足！需要 §e" + waypoint.getTeleportPrice() + " §c金币。");
                return false;
            }

            // 扣除费用（给创建者或系统）
            if (plugin.getConfig().getBoolean("economy.waypoint.feeToCreator", true)) {
                UUID creator = waypoint.getCreator();
                if (!creator.equals(player.getUniqueId())) {
                    dbAddBalance(creator, waypoint.getTeleportPrice() * 0.9); // 90%给创建者
                    dbAddBalance(null, waypoint.getTeleportPrice() * 0.1);   // 10%给系统
                }
            }
            dbRemoveBalance(player.getUniqueId(), waypoint.getTeleportPrice());
            player.sendMessage("§a已传送到 §e" + waypoint.getName() + " §7(费用: " + waypoint.getTeleportPrice() + " 金币)");
        } else {
            player.sendMessage("§a已传送到 §e" + waypoint.getName());
        }

        player.teleport(waypoint.getLocation());
        return true;
    }

    public boolean requestPermission(Player player, String waypointId) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            player.sendMessage("§c地标不存在！");
            return false;
        }

        if (!waypoint.isRequiresPermission()) {
            player.sendMessage("§c该地标不需要申请权限！");
            return false;
        }

        if (waypoint.getCreator().equals(player.getUniqueId())) {
            player.sendMessage("§c你是创建者，不需要申请权限！");
            return false;
        }

        pendingPermissions.computeIfAbsent(waypointId, k -> ConcurrentHashMap.newKeySet())
                .add(player.getUniqueId());
        saveWaypoint(waypoint);

        player.sendMessage("§a已向创建者 §e" + getPlayerName(waypoint.getCreator()) + 
                          " §a发送权限申请！");

        // 通知创建者
        Player creator = Bukkit.getPlayer(waypoint.getCreator());
        if (creator != null) {
            creator.sendMessage("§e玩家 " + player.getName() + " 申请使用你的地标 §c" + 
                               waypoint.getName());
            creator.sendMessage("§7使用 §e/warp approve " + waypoint.getName() + " §7同意");
            creator.sendMessage("§7使用 §e/warp deny " + waypoint.getName() + " §7拒绝");
        }

        return true;
    }

    public boolean approvePermission(Player player, String waypointId) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            player.sendMessage("§c地标不存在！");
            return false;
        }

        if (!waypoint.getCreator().equals(player.getUniqueId())) {
            player.sendMessage("§c你不是这个地标的创建者！");
            return false;
        }

        Set<UUID> pending = pendingPermissions.get(waypointId);
        if (pending == null || pending.isEmpty()) {
            player.sendMessage("§c没有待批准的申请！");
            return false;
        }

        // 通知第一个申请者
        UUID applicant = pending.iterator().next();
        pending.remove(applicant);
        saveWaypoint(waypoint);

        player.sendMessage("§a已批准申请！");
        
        Player target = Bukkit.getPlayer(applicant);
        if (target != null) {
            target.sendMessage("§a创建者 §e" + player.getName() + " §a已批准你使用地标 §e" + waypoint.getName());
        }

        return true;
    }

    public boolean denyPermission(Player player, String waypointId) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            player.sendMessage("§c地标不存在！");
            return false;
        }

        if (!waypoint.getCreator().equals(player.getUniqueId())) {
            player.sendMessage("§c你不是这个地标的创建者！");
            return false;
        }

        Set<UUID> pending = pendingPermissions.get(waypointId);
        if (pending == null || pending.isEmpty()) {
            player.sendMessage("§c没有待拒绝的申请！");
            return false;
        }

        UUID applicant = pending.iterator().next();
        pending.remove(applicant);
        saveWaypoint(waypoint);

        player.sendMessage("§c已拒绝申请！");
        
        Player target = Bukkit.getPlayer(applicant);
        if (target != null) {
            target.sendMessage("§c创建者 §e" + player.getName() + " §c拒绝了你使用地标 §e" + waypoint.getName());
        }

        return true;
    }

    public List<Waypoint> getAllWaypoints() {
        return new ArrayList<>(waypoints.values());
    }

    public List<Waypoint> getPlayerWaypoints(UUID playerId) {
        Set<String> ids = playerWaypoints.get(playerId.toString());
        if (ids == null) return new ArrayList<>();
        
        List<Waypoint> result = new ArrayList<>();
        for (String id : ids) {
            Waypoint wp = waypoints.get(id);
            if (wp != null) {
                result.add(wp);
            }
        }
        return result;
    }

    public List<Waypoint> getAvailableWaypoints(UUID playerId) {
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : waypoints.values()) {
            if (wp.isRequiresPermission()) {
                // 需要权限的地标，检查是否已批准
                if (wp.getCreator().equals(playerId)) {
                    result.add(wp);
                } else {
                    Set<UUID> pending = pendingPermissions.get(wp.getId());
                    if (pending != null && pending.contains(playerId)) {
                        result.add(wp); // 已申请，待批准
                    }
                }
            } else {
                result.add(wp); // 不需要权限的地标
            }
        }
        return result;
    }

    public Waypoint getWaypointById(String waypointId) {
        return waypoints.get(waypointId);
    }

    public Waypoint getWaypointByName(String name, String worldName) {
        String waypointId = generateWaypointId(name, worldName);
        return waypoints.get(waypointId);
    }

    public Set<UUID> getPendingPermissions(String waypointId) {
        return pendingPermissions.getOrDefault(waypointId, new HashSet<>());
    }

    public void setWaypointCreateCount(CommandSender sender, String waypointId, int count) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            sender.sendMessage("§c地标不存在！");
            return;
        }
        waypoint.setCreateCount(count);
        saveWaypoint(waypoint);
        sender.sendMessage("§a已设置创建免费次数为 §e" + count);
    }

    public void setWaypointTeleportCount(CommandSender sender, String waypointId, int count) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            sender.sendMessage("§c地标不存在！");
            return;
        }
        waypoint.setTeleportCount(count);
        saveWaypoint(waypoint);
        sender.sendMessage("§a已设置传送免费次数为 §e" + count);
    }

    public void setWaypointCreatePrice(CommandSender sender, String waypointId, double price) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            sender.sendMessage("§c地标不存在！");
            return;
        }
        waypoint.setCreatePrice(price);
        saveWaypoint(waypoint);
        sender.sendMessage("§a已设置创建费用为 §e" + price + " §a金币");
    }

    public void setWaypointTeleportPrice(CommandSender sender, String waypointId, double price) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            sender.sendMessage("§c地标不存在！");
            return;
        }
        waypoint.setTeleportPrice(price);
        saveWaypoint(waypoint);
        sender.sendMessage("§a已设置传送费用为 §e" + price + " §a金币");
    }

    public void setWaypointPermission(CommandSender sender, String waypointId, boolean required) {
        Waypoint waypoint = waypoints.get(waypointId);
        if (waypoint == null) {
            sender.sendMessage("§c地标不存在！");
            return;
        }
        waypoint.setRequiresPermission(required);
        saveWaypoint(waypoint);
        sender.sendMessage(required ? "§a已启用权限要求" : "§c已禁用权限要求");
    }

    private void saveWaypoint(Waypoint waypoint) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db != null) {
            db.saveWaypoint(waypoint);
        }
    }

    private String generateWaypointId(String name, String worldName) {
        return worldName.toLowerCase() + "_" + name.toLowerCase();
    }

    private double getPlayerBalance(Player player) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) return 0;
        var user = db.getUser(player.getUniqueId());
        return user != null ? user.balance : 0;
    }

    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : uuid.toString().substring(0, 8);
    }

    private void dbAddBalance(UUID target, double amount) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db != null && target != null) {
            var user = db.getUser(target);
            if (user != null) {
                user.balance += amount;
                db.saveUser(user);
            }
        }
        // null表示系统账户，存入税收池
    }

    private void dbRemoveBalance(UUID player, double amount) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db != null) {
            var user = db.getUser(player);
            if (user != null) {
                user.balance -= amount;
                db.saveUser(user);
            }
        }
    }
}

package com.scroam.teleport;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 领地管理器 - 处理所有领地相关逻辑
 */
public class LandManager {

    private final TeleportPlugin plugin;
    private final Map<String, Land> lands; // landId -> Land
    private final Map<UUID, Set<String>> playerLands; // playerId -> Set<landId>
    private final Map<String, String> chunkLands; // "world,x,z" -> landId (用于快速查找)

    public LandManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        this.lands = new HashMap<>();
        this.playerLands = new HashMap<>();
        this.chunkLands = new HashMap<>();
        
        loadLands();
    }

    /**
     * 加载所有领地数据
     */
    public void loadLands() {
        lands.clear();
        playerLands.clear();
        chunkLands.clear();
        
        // 从数据库加载
        plugin.getDatabaseManager().loadLands(this);
        
        plugin.getLogger().info("Loaded " + lands.size() + " lands");
    }

    /**
     * 保存所有领地数据
     */
    public void saveLands() {
        plugin.getDatabaseManager().saveLands(lands.values());
        plugin.getLogger().info("Saved " + lands.size() + " lands");
    }

    /**
     * 添加领地
     */
    public boolean addLand(Land land) {
        if (lands.containsKey(land.getId())) {
            return false;
        }
        
        lands.put(land.getId(), land);
        
        // 更新玩家领地列表
        playerLands.computeIfAbsent(land.getOwnerId(), k -> new HashSet<>()).add(land.getId());
        
        // 更新区块索引
        updateChunkIndex(land);
        
        // 保存到数据库
        plugin.getDatabaseManager().saveLand(land);
        
        return true;
    }

    /**
     * 移除领地
     */
    public boolean removeLand(String landId) {
        Land land = lands.remove(landId);
        if (land == null) {
            return false;
        }
        
        // 从玩家列表移除
        playerLands.get(land.getOwnerId()).remove(landId);
        
        // 移除区块索引
        removeChunkIndex(land);
        
        // 从数据库删除
        plugin.getDatabaseManager().deleteLand(landId);
        
        return true;
    }

    /**
     * 根据ID获取领地
     */
    public Land getLand(String landId) {
        return lands.get(landId);
    }

    /**
     * 根据坐标获取领地
     */
    public Land getLandAt(Location loc) {
        return getLandAt(loc.getWorld(), loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * 根据坐标获取领地
     */
    public Land getLandAt(World world, int x, int z) {
        String key = world.getName() + "," + x + "," + z;
        String landId = chunkLands.get(key);
        return landId != null ? lands.get(landId) : null;
    }

    /**
     * 获取玩家拥有的所有领地
     */
    public Set<Land> getPlayerLands(UUID playerId) {
        Set<String> landIds = playerLands.get(playerId);
        if (landIds == null) {
            return Collections.emptySet();
        }
        
        Set<Land> result = new HashSet<>();
        for (String landId : landIds) {
            Land land = lands.get(landId);
            if (land != null) {
                result.add(land);
            }
        }
        return result;
    }

    /**
     * 获取玩家作为成员的所有领地
     */
    public Set<Land> getPlayerMemberLands(UUID playerId) {
        Set<Land> result = new HashSet<>();
        for (Land land : lands.values()) {
            if (land.isMember(playerId) && !land.isOwner(playerId)) {
                result.add(land);
            }
        }
        return result;
    }

    /**
     * 检查坐标是否在任意领地内
     */
    public boolean isInAnyLand(World world, int x, int z) {
        return getLandAt(world, x, z) != null;
    }

    /**
     * 检查坐标是否在指定领地的边界上
     */
    public boolean isOnBorder(World world, int x, int z) {
        Land land = getLandAt(world, x, z);
        if (land == null) {
            return false;
        }
        
        // 检查是否在边界上
        return (x == land.getMinX() || x == land.getMaxX() || 
                z == land.getMinZ() || z == land.getMaxZ());
    }

    /**
     * 尝试购买领地
     */
    public String claimLand(Player player, int radius) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int centerX = loc.getBlockX();
        int centerZ = loc.getBlockZ();
        
        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;
        
        // 检查区域是否与其他领地重叠
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (isInAnyLand(world, x, z)) {
                    Land existing = getLandAt(world, x, z);
                    return "该区域与领地 [" + existing.getName() + "] 重叠，无法购买！";
                }
            }
        }
        
        // 生成领地ID
        String landId = generateLandId(player.getUniqueId());
        
        // 创建领地
        Land land = new Land(landId, player.getUniqueId(), 
                player.getName() + "的领地", 
                world, minX, maxX, minZ, maxZ);
        
        // 检查余额
        double price = land.getBuyPrice();
        double balance = plugin.getDatabaseManager().getBalance(player.getUniqueId());
        if (balance < price) {
            return "余额不足！购买此领地需要 " + String.format("%.2f", price) + " 金币，你只有 " + String.format("%.2f", balance) + " 金币。";
        }
        
        // 扣除费用
        plugin.getDatabaseManager().withdrawBalance(player.getUniqueId(), price);
        
        // 添加领地
        addLand(land);
        
        return null; // 成功
    }

    /**
     * 出售领地
     */
    public String sellLand(String landId, Player player) {
        Land land = getLand(landId);
        if (land == null) {
            return "领地不存在！";
        }
        
        if (!land.isOwner(player.getUniqueId())) {
            return "你不是这个领地的主人！";
        }
        
        // 退还50%价格
        double refund = land.getBuyPrice() * 0.5;
        plugin.getDatabaseManager().depositBalance(player.getUniqueId(), refund);
        
        // 删除领地
        removeLand(landId);
        
        return "领地已出售，获得 " + String.format("%.2f", refund) + " 金币。";
    }

    /**
     * 添加领地成员
     */
    public String addMember(String landId, Player owner, String targetName, String memberTypeStr) {
        Land land = getLand(landId);
        if (land == null) {
            return "领地不存在！";
        }
        
        if (!land.isOwner(owner.getUniqueId())) {
            return "你不是这个领地的主人！";
        }
        
        // 查找目标玩家
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            return "玩家 " + targetName + " 不在线！";
        }
        
        UUID targetId = target.getUniqueId();
        
        if (land.isMember(targetId)) {
            return "玩家 " + targetName + " 已经是领地成员！";
        }
        
        // 解析成员类型
        Land.MemberType memberType = Land.MemberType.fromKey(memberTypeStr);
        if (memberType == null) {
            return "未知的成员类型！可用类型: owner, member, guest";
        }
        
        if (!land.addMember(targetId, memberType)) {
            return "添加成员失败！";
        }
        
        // 保存
        plugin.getDatabaseManager().saveLand(land);
        
        return "已添加 " + targetName + " 为 [" + memberType.getName() + "]";
    }

    /**
     * 移除领地成员
     */
    public String removeMember(String landId, Player owner, String targetName) {
        Land land = getLand(landId);
        if (land == null) {
            return "领地不存在！";
        }
        
        if (!land.isOwner(owner.getUniqueId())) {
            return "你不是这个领地的主人！";
        }
        
        // 查找目标玩家
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            return "玩家 " + targetName + " 不在线！";
        }
        
        UUID targetId = target.getUniqueId();
        
        if (!land.isMember(targetId)) {
            return "玩家 " + targetName + " 不是领地成员！";
        }
        
        if (!land.removeMember(targetId)) {
            return "移除成员失败！";
        }
        
        // 保存
        plugin.getDatabaseManager().saveLand(land);
        
        return "已移除 " + targetName + " 从领地。";
    }

    /**
     * 设置成员权限
     */
    public String setMemberPermission(String landId, Player owner, String targetName, String permissionStr, boolean enable) {
        Land land = getLand(landId);
        if (land == null) {
            return "领地不存在！";
        }
        
        if (!land.isOwner(owner.getUniqueId())) {
            return "你不是这个领地的主人！";
        }
        
        // 查找目标玩家
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            return "玩家 " + targetName + " 不在线！";
        }
        
        UUID targetId = target.getUniqueId();
        
        if (!land.isMember(targetId)) {
            return "玩家 " + targetName + " 不是领地成员！";
        }
        
        // 解析权限
        Land.Permission permission = Land.Permission.fromKey(permissionStr);
        if (permission == null) {
            return "未知的权限类型！可用权限: build, destroy, interact, chest, pvp, animals, itemdrop, itempickup";
        }
        
        land.setMemberPermission(targetId, permission, enable);
        
        // 保存
        plugin.getDatabaseManager().saveLand(land);
        
        return (enable ? "已启用" : "已禁用") + " " + targetName + " 的 [" + permission.getDescription() + "]";
    }

    /**
     * 生成唯一的领地ID
     */
    private String generateLandId(UUID ownerId) {
        return ownerId.toString().substring(0, 8) + "_" + System.currentTimeMillis() % 100000;
    }

    /**
     * 更新区块索引
     */
    private void updateChunkIndex(Land land) {
        for (int x = land.getMinX(); x <= land.getMaxX(); x++) {
            for (int z = land.getMinZ(); z <= land.getMaxZ(); z++) {
                String key = land.getWorld().getName() + "," + x + "," + z;
                chunkLands.put(key, land.getId());
            }
        }
    }

    /**
     * 移除区块索引
     */
    private void removeChunkIndex(Land land) {
        for (int x = land.getMinX(); x <= land.getMaxX(); x++) {
            for (int z = land.getMinZ(); z <= land.getMaxZ(); z++) {
                String key = land.getWorld().getName() + "," + x + "," + z;
                chunkLands.remove(key);
            }
        }
    }

    /**
     * 获取所有领地数量
     */
    public int getLandCount() {
        return lands.size();
    }

    /**
     * 获取玩家拥有的领地数量
     */
    public int getPlayerLandCount(UUID playerId) {
        Set<String> set = playerLands.get(playerId);
        return set != null ? set.size() : 0;
    }
}

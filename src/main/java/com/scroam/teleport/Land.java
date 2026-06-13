package com.scroam.teleport;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * 领地数据类
 */
public class Land {
    
    // 权限类型
    public enum Permission {
        BUILD("build", "建设权限"),
        DESTROY("destroy", "破坏权限"),
        INTERACT("interact", "交互权限"),
        CHEST_ACCESS("chest", "箱子权限"),
        PVP("pvp", "PVP权限"),
        ANIMALS("animals", "动物交互权限"),
        ITEM_DROP("itemdrop", "物品掉落权限"),
        ITEM_PICKUP("itempickup", "物品拾取权限");

        private final String key;
        private final String description;

        Permission(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public String getKey() {
            return key;
        }

        public String getDescription() {
            return description;
        }

        public static Permission fromKey(String key) {
            for (Permission p : values()) {
                if (p.key.equalsIgnoreCase(key)) {
                    return p;
                }
            }
            return null;
        }
    }

    // 成员权限类型
    public enum MemberType {
        OWNER("owner", "地主", 100),
        MEMBER("member", "成员", 50),
        GUEST("guest", "访客", 10);

        private final String key;
        private final String name;
        private final int level;

        MemberType(String key, String name, int level) {
            this.key = key;
            this.name = name;
            this.level = level;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public int getLevel() {
            return level;
        }

        public static MemberType fromKey(String key) {
            for (MemberType t : values()) {
                if (t.key.equalsIgnoreCase(key)) {
                    return t;
                }
            }
            return null;
        }
    }

    private final String id;
    private final UUID ownerId;
    private String name;
    private final World world;
    private int minX, maxX, minZ, maxZ;
    private final Map<UUID, MemberType> members; // 成员及其类型
    private final Set<Permission> defaultPermissions; // 默认权限
    private final Map<UUID, Set<Permission>> memberPermissions; // 成员特殊权限
    private long createdTime;
    private double buyPrice;

    public Land(String id, UUID ownerId, String name, World world, int minX, int maxX, int minZ, int maxZ) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.members = new HashMap<>();
        this.defaultPermissions = new HashSet<>();
        this.memberPermissions = new HashMap<>();
        this.createdTime = System.currentTimeMillis();
        this.buyPrice = calculatePrice();
        
        // 默认给地主所有权限
        setDefaultPermission(Permission.BUILD);
        setDefaultPermission(Permission.DESTROY);
        setDefaultPermission(Permission.INTERACT);
        setDefaultPermission(Permission.CHEST_ACCESS);
        setDefaultPermission(Permission.ANIMALS);
        setDefaultPermission(Permission.ITEM_DROP);
        setDefaultPermission(Permission.ITEM_PICKUP);
    }

    /**
     * 计算领地价格（基于面积）
     */
    public double calculatePrice() {
        int area = getArea();
        // 每格0.5金币，最小100金币
        return Math.max(100, area * 0.5);
    }

    /**
     * 获取领地面积
     */
    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    /**
     * 检查坐标是否在领地内
     */
    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * 检查位置是否在领地内
     */
    public boolean contains(Location loc) {
        return loc.getWorld().equals(world) && contains(loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * 添加成员
     */
    public boolean addMember(UUID uuid, MemberType type) {
        if (members.containsKey(uuid)) {
            return false;
        }
        members.put(uuid, type);
        return true;
    }

    /**
     * 移除成员
     */
    public boolean removeMember(UUID uuid) {
        return members.remove(uuid) != null;
    }

    /**
     * 获取成员类型
     */
    public MemberType getMemberType(UUID uuid) {
        if (uuid.equals(ownerId)) {
            return MemberType.OWNER;
        }
        return members.get(uuid);
    }

    /**
     * 检查用户是否有权限
     */
    public boolean hasPermission(UUID uuid, Permission permission) {
        MemberType type = getMemberType(uuid);
        if (type == null) {
            return false;
        }

        // 地主拥有所有权限
        if (type == MemberType.OWNER) {
            return true;
        }

        // 检查成员特殊权限
        Set<Permission> perms = memberPermissions.get(uuid);
        if (perms != null && perms.contains(permission)) {
            return true;
        }

        // 检查默认权限
        if (defaultPermissions.contains(permission)) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否是领主
     */
    public boolean isOwner(UUID uuid) {
        return ownerId.equals(uuid);
    }

    /**
     * 检查是否是成员（包括领主）
     */
    public boolean isMember(UUID uuid) {
        return isOwner(uuid) || members.containsKey(uuid);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public World getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public Map<UUID, MemberType> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public Set<Permission> getDefaultPermissions() {
        return Collections.unmodifiableSet(defaultPermissions);
    }

    public void setDefaultPermission(Permission permission) {
        defaultPermissions.add(permission);
    }

    public void removeDefaultPermission(Permission permission) {
        defaultPermissions.remove(permission);
    }

    public Map<UUID, Set<Permission>> getMemberPermissions() {
        return Collections.unmodifiableMap(memberPermissions);
    }

    public void setMemberPermission(UUID uuid, Permission permission, boolean enable) {
        memberPermissions.computeIfAbsent(uuid, k -> new HashSet<>());
        if (enable) {
            memberPermissions.get(uuid).add(permission);
        } else {
            memberPermissions.get(uuid).remove(permission);
        }
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    @Override
    public String toString() {
        return "Land{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", owner=" + ownerId +
                ", area=" + getArea() +
                '}';
    }
}

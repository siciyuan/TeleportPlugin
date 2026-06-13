package com.scroam.teleport;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * 地标数据类
 */
public class Waypoint {
    private final String id;           // 地标唯一ID
    private final String name;         // 地标名称
    private final UUID creator;        // 创建者
    private final World world;         // 所在世界
    private final double x, y, z;      // 坐标
    private final float yaw, pitch;    // 视角
    private int createCount;           // 创建免费次数（默认0）
    private int teleportCount;         // 传送免费次数（默认3）
    private double createPrice;        // 创建费用
    private double teleportPrice;      // 传送费用
    private boolean requiresPermission; // 是否需要创建者许可
    private long createdTime;          // 创建时间

    public Waypoint(String id, String name, UUID creator, Location location) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.world = location.getWorld();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.createCount = 0;           // 默认创建0次
        this.teleportCount = 3;         // 默认传送3次
        this.createPrice = 0;
        this.teleportPrice = 0;
        this.requiresPermission = false;
        this.createdTime = System.currentTimeMillis();
    }

    public Waypoint(String id, String name, UUID creator, String worldName, 
                    double x, double y, double z, float yaw, float pitch,
                    int createCount, int teleportCount, double createPrice, 
                    double teleportPrice, boolean requiresPermission, long createdTime) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.world = org.bukkit.Bukkit.getWorld(worldName);
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createCount = createCount;
        this.teleportCount = teleportCount;
        this.createPrice = createPrice;
        this.teleportPrice = teleportPrice;
        this.requiresPermission = requiresPermission;
        this.createdTime = createdTime;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getCreator() {
        return creator;
    }

    public World getWorld() {
        return world;
    }

    public String getWorldName() {
        return world != null ? world.getName() : "";
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public int getCreateCount() {
        return createCount;
    }

    public void setCreateCount(int createCount) {
        this.createCount = createCount;
    }

    public int getTeleportCount() {
        return teleportCount;
    }

    public void setTeleportCount(int teleportCount) {
        this.teleportCount = teleportCount;
    }

    public double getCreatePrice() {
        return createPrice;
    }

    public void setCreatePrice(double createPrice) {
        this.createPrice = createPrice;
    }

    public double getTeleportPrice() {
        return teleportPrice;
    }

    public void setTeleportPrice(double teleportPrice) {
        this.teleportPrice = teleportPrice;
    }

    public boolean isRequiresPermission() {
        return requiresPermission;
    }

    public void setRequiresPermission(boolean requiresPermission) {
        this.requiresPermission = requiresPermission;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public Location getLocation() {
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean isValid() {
        return world != null;
    }

    @Override
    public String toString() {
        return "Waypoint{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", creator=" + creator +
                ", world=" + (world != null ? world.getName() : "null") +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", createCount=" + createCount +
                ", teleportCount=" + teleportCount +
                '}';
    }
}

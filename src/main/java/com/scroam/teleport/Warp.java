package com.scroam.teleport;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * 地标数据类
 */
public class Warp {
    private final String name;
    private final Location location;
    private final UUID ownerId;
    private final String ownerName;
    private final long createdTime;
    private boolean isPublic;
    private double teleportPrice;
    
    public Warp(String name, Location location, UUID ownerId, String ownerName) {
        this.name = name;
        this.location = location;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.createdTime = System.currentTimeMillis();
        this.isPublic = true;
        this.teleportPrice = 0;
    }
    
    public Warp(String name, Location location, UUID ownerId, String ownerName, 
                long createdTime, boolean isPublic, double teleportPrice) {
        this.name = name;
        this.location = location;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.createdTime = createdTime;
        this.isPublic = isPublic;
        this.teleportPrice = teleportPrice;
    }
    
    public String getName() {
        return name;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public World getWorld() {
        return location.getWorld();
    }
    
    public double getX() {
        return location.getX();
    }
    
    public double getY() {
        return location.getY();
    }
    
    public double getZ() {
        return location.getZ();
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public double getTeleportPrice() {
        return teleportPrice;
    }
    
    public void setTeleportPrice(double teleportPrice) {
        this.teleportPrice = teleportPrice;
    }
    
    public boolean isOwner(UUID playerId) {
        return ownerId.equals(playerId);
    }
}
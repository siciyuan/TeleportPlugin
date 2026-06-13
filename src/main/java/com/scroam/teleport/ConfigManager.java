package com.scroam.teleport;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;

public class ConfigManager {

    private final TeleportPlugin plugin;
    private Location spawnLocation;

    public ConfigManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        ensureConfigDefaults();
        loadSpawnLocation();
    }
    
    /**
     * 确保配置文件包含所有必要的默认值
     */
    private void ensureConfigDefaults() {
        boolean updated = false;
        
        // 确保兑换配置存在
        if (!plugin.getConfig().contains("economy.exchange.enabled")) {
            plugin.getConfig().set("economy.exchange.enabled", true);
            updated = true;
        }
        if (!plugin.getConfig().contains("economy.exchange.withdraw-tax")) {
            plugin.getConfig().set("economy.exchange.withdraw-tax", 0.10);
            updated = true;
        }
        
        // 确保所有兑换比例存在
        if (!plugin.getConfig().contains("economy.exchange.rates")) {
            plugin.getConfig().createSection("economy.exchange.rates");
            updated = true;
        }
        
        // 强制设置所有兑换比例（即使已存在也覆盖为默认值）
        plugin.getConfig().set("economy.exchange.rates.gold_ingot", 100.0);
        plugin.getConfig().set("economy.exchange.rates.gold_nugget", 11.11);
        plugin.getConfig().set("economy.exchange.rates.gold_block", 900.0);
        plugin.getConfig().set("economy.exchange.rates.raw_gold", 50.0);
        plugin.getConfig().set("economy.exchange.rates.copper_ingot", 10.0);
        plugin.getConfig().set("economy.exchange.rates.copper_nugget", 1.11);
        plugin.getConfig().set("economy.exchange.rates.copper_block", 90.0);
        plugin.getConfig().set("economy.exchange.rates.raw_copper", 5.0);
        plugin.getConfig().set("economy.exchange.rates.iron_ingot", 20.0);
        plugin.getConfig().set("economy.exchange.rates.iron_nugget", 2.22);
        plugin.getConfig().set("economy.exchange.rates.iron_block", 180.0);
        plugin.getConfig().set("economy.exchange.rates.raw_iron", 10.0);
        plugin.getConfig().set("economy.exchange.rates.diamond", 500.0);
        plugin.getConfig().set("economy.exchange.rates.diamond_block", 4500.0);
        plugin.getConfig().set("economy.exchange.rates.netherite_ingot", 2000.0);
        plugin.getConfig().set("economy.exchange.rates.netherite_block", 18000.0);
        plugin.getConfig().set("economy.exchange.rates.emerald", 300.0);
        plugin.getConfig().set("economy.exchange.rates.emerald_block", 2700.0);
        plugin.getConfig().set("economy.exchange.rates.coal", 10.0);
        plugin.getConfig().set("economy.exchange.rates.charcoal", 8.0);
        plugin.getConfig().set("economy.exchange.rates.coal_block", 90.0);
        plugin.getConfig().set("economy.exchange.rates.lapis_lazuli", 5.0);
        updated = true;
        
        // 确保世界保护配置存在
        if (!plugin.getConfig().contains("world-protection.enabled")) {
            plugin.getConfig().set("world-protection.enabled", true);
            updated = true;
        }
        if (!plugin.getConfig().contains("world-protection.spawn-protection-radius")) {
            plugin.getConfig().set("world-protection.spawn-protection-radius", 100);
            updated = true;
        }
        if (!plugin.getConfig().contains("world-protection.allowed-players")) {
            plugin.getConfig().set("world-protection.allowed-players", new ArrayList<>());
            updated = true;
        }
        
        // 确保公告配置存在
        if (!plugin.getConfig().contains("announcements.motd")) {
            plugin.getConfig().set("announcements.motd", "欢迎来到服务器");
            updated = true;
        }
        if (!plugin.getConfig().contains("announcements.list")) {
            plugin.getConfig().set("announcements.list", new ArrayList<>());
            updated = true;
        }
        if (!plugin.getConfig().contains("announcements.signs")) {
            plugin.getConfig().set("announcements.signs", new ArrayList<>());
            updated = true;
        }
        
        // 确保地标配置存在
        if (!plugin.getConfig().contains("warp.create.price")) {
            plugin.getConfig().set("warp.create.price", 100.0);
            updated = true;
        }
        if (!plugin.getConfig().contains("warp.create.free_count")) {
            plugin.getConfig().set("warp.create.free_count", 0);
            updated = true;
        }
        if (!plugin.getConfig().contains("warp.teleport.free_count")) {
            plugin.getConfig().set("warp.teleport.free_count", 3);
            updated = true;
        }
        
        if (updated) {
            plugin.saveConfig();
            plugin.getLogger().info("Configuration updated with missing defaults.");
        }
    }

    public void loadSpawnLocation() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("spawn.world")) {
            String worldName = config.getString("spawn.world");
            double x = config.getDouble("spawn.x", 0);
            double y = config.getDouble("spawn.y", 64);
            double z = config.getDouble("spawn.z", 0);
            float yaw = (float) config.getDouble("spawn.yaw", 0);
            float pitch = (float) config.getDouble("spawn.pitch", 0);
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                spawnLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }
    }

    public void saveSpawnLocation(Location location) {
        FileConfiguration config = plugin.getConfig();
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        plugin.saveConfig();
        this.spawnLocation = location;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public int getRtpMinRadius() {
        return plugin.getConfig().getInt("rtp.min-radius", 50);
    }

    public int getRtpMaxRadius() {
        return plugin.getConfig().getInt("rtp.max-radius", 5000);
    }

    public int getRtpDefaultRadius() {
        return plugin.getConfig().getInt("rtp.default-radius", 200);
    }

    public int getTpaTimeout() {
        return plugin.getConfig().getInt("tpa.timeout-seconds", 60);
    }

    public int getMaxHomes() {
        return plugin.getConfig().getInt("homes.max-homes", 3);
    }

    public int getRtpDailyFreeCount() {
        return plugin.getConfig().getInt("rtp.daily-free-count", 3);
    }

    public double getRtpPrice() {
        return plugin.getConfig().getDouble("rtp.price", 100.0);
    }

    public boolean isEconomyEnabled() {
        return plugin.getConfig().getBoolean("economy.enabled", true);
    }

    public double getTransferTax() {
        return plugin.getConfig().getDouble("economy.transfer-tax", 0.05);
    }

    public boolean isExchangeEnabled() {
        return plugin.getConfig().getBoolean("economy.exchange.enabled", true);
    }

    public double getWithdrawTax() {
        return plugin.getConfig().getDouble("economy.exchange.withdraw-tax", 0.10);
    }

    public double getExchangeRate(String itemType) {
        return plugin.getConfig().getDouble("economy.exchange.rates." + itemType, 0);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadSpawnLocation();
    }

    public boolean isDatabaseEnabled() {
        return plugin.getConfig().getBoolean("database.enabled", true);
    }
}
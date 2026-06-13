package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * 数据迁移管理器 - 自动迁移旧版本数据到数据库
 */
public class DataMigrationManager {

    private final TeleportPlugin plugin;
    private final File migrationMarker;

    public DataMigrationManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        this.migrationMarker = new File(plugin.getDataFolder(), ".migrated");
    }

    /**
     * 检查并执行数据迁移
     */
    public void checkAndMigrate() {
        // 如果已经迁移过，跳过
        if (migrationMarker.exists()) {
            plugin.getLogger().info("Data already migrated, skipping migration.");
            return;
        }

        // 如果数据库未启用，跳过迁移
        if (plugin.getDatabaseManager() == null) {
            plugin.getLogger().info("Database not enabled, skipping migration.");
            return;
        }

        plugin.getLogger().info("Starting data migration from old version...");

        // 迁移配置数据
        migrateConfigData();

        // 迁移家数据
        migrateHomesData();

        // 标记迁移完成
        markMigrationComplete();

        plugin.getLogger().info("Data migration completed successfully!");
    }

    /**
     * 迁移配置数据（主城位置等）
     */
    private void migrateConfigData() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // 迁移主城位置
            if (config.contains("spawn.world")) {
                String worldName = config.getString("spawn.world");
                double x = config.getDouble("spawn.x", 0);
                double y = config.getDouble("spawn.y", 64);
                double z = config.getDouble("spawn.z", 0);
                float yaw = (float) config.getDouble("spawn.yaw", 0);
                float pitch = (float) config.getDouble("spawn.pitch", 0);

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location spawnLoc = new Location(world, x, y, z, yaw, pitch);
                    plugin.getDatabaseManager().setWorldSpawn(worldName, spawnLoc);
                    plugin.getLogger().info("Migrated spawn location for world: " + worldName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to migrate config data: " + e.getMessage());
        }
    }

    /**
     * 迁移家数据从 homes.yml 到数据库
     */
    private void migrateHomesData() {
        File homesFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            plugin.getLogger().info("No homes.yml found, skipping homes migration.");
            return;
        }

        try {
            FileConfiguration homesConfig = YamlConfiguration.loadConfiguration(homesFile);
            int migratedCount = 0;

            for (String uuidStr : homesConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    var section = homesConfig.getConfigurationSection(uuidStr);
                    if (section == null) {
                        continue;
                    }

                    for (String homeName : section.getKeys(false)) {
                        try {
                            String path = uuidStr + "." + homeName;
                            String worldName = homesConfig.getString(path + ".world");
                            if (worldName == null) {
                                continue;
                            }

                            World world = Bukkit.getWorld(worldName);
                            if (world == null) {
                                plugin.getLogger().warning("World " + worldName + " not found, skipping home " + homeName);
                                continue;
                            }

                            double x = homesConfig.getDouble(path + ".x", 0);
                            double y = homesConfig.getDouble(path + ".y", 64);
                            double z = homesConfig.getDouble(path + ".z", 0);
                            float yaw = (float) homesConfig.getDouble(path + ".yaw", 0);
                            float pitch = (float) homesConfig.getDouble(path + ".pitch", 0);

                            Location loc = new Location(world, x, y, z, yaw, pitch);
                            plugin.getDatabaseManager().saveHome(uuid, homeName, loc);
                            migratedCount++;
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to migrate home " + homeName + ": " + e.getMessage());
                        }
                    }

                    // 创建用户数据（如果不存在）
                    String username = Bukkit.getOfflinePlayer(uuid).getName();
                    if (username != null) {
                        plugin.getDatabaseManager().createUser(uuid, username);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID during migration: " + uuidStr);
                }
            }

            plugin.getLogger().info("Migrated " + migratedCount + " homes to database.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to migrate homes data: " + e.getMessage());
        }
    }

    /**
     * 标记迁移完成
     */
    private void markMigrationComplete() {
        try {
            migrationMarker.createNewFile();
            FileConfiguration markerConfig = YamlConfiguration.loadConfiguration(migrationMarker);
            markerConfig.set("migrated_at", new Timestamp(System.currentTimeMillis()).toString());
            markerConfig.set("version", plugin.getDescription().getVersion());
            markerConfig.save(migrationMarker);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create migration marker: " + e.getMessage());
        }
    }

    /**
     * 检查是否已迁移
     */
    public boolean isMigrated() {
        return migrationMarker.exists();
    }

    /**
     * 强制重新迁移（管理员命令）
     */
    public void forceMigrate() {
        if (migrationMarker.exists()) {
            migrationMarker.delete();
        }
        checkAndMigrate();
    }
}
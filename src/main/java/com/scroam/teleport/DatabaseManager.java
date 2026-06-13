package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final TeleportPlugin plugin;
    private Connection connection;
    private final Map<UUID, UserData> userCache = new ConcurrentHashMap<>();
    private final Map<String, WorldData> worldCache = new ConcurrentHashMap<>();

    public DatabaseManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/database.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
            loadCache();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // 世界表
            stmt.execute("CREATE TABLE IF NOT EXISTS worlds (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world_name VARCHAR(64) UNIQUE NOT NULL," +
                    "display_name VARCHAR(64) DEFAULT ''," +
                    "spawn_x DOUBLE DEFAULT 0," +
                    "spawn_y DOUBLE DEFAULT 64," +
                    "spawn_z DOUBLE DEFAULT 0," +
                    "spawn_yaw FLOAT DEFAULT 0," +
                    "spawn_pitch FLOAT DEFAULT 0," +
                    "is_main_world BOOLEAN DEFAULT FALSE," +
                    "enabled BOOLEAN DEFAULT TRUE," +
                    "properties TEXT DEFAULT '{}'" +
                    ")");

            // 用户表（为扩展预留空间）
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36) UNIQUE NOT NULL," +
                    "username VARCHAR(16) NOT NULL," +
                    "balance DECIMAL(18,2) DEFAULT 0," +
                    "rtp_free_count INTEGER DEFAULT 3," +
                    "rtp_max_free_count INTEGER DEFAULT 3," +
                    "rtp_price DECIMAL(10,2) DEFAULT 100," +
                    "homes_limit INTEGER DEFAULT 3," +
                    "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "play_time BIGINT DEFAULT 0," +
                    "stats TEXT DEFAULT '{}'," +
                    "settings TEXT DEFAULT '{}'," +
                    "extra_data TEXT DEFAULT '{}'" +
                    ")");

            // 家数据表（独立表，支持多世界）
            stmt.execute("CREATE TABLE IF NOT EXISTS homes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_uuid VARCHAR(36) NOT NULL," +
                    "home_name VARCHAR(32) NOT NULL," +
                    "world_name VARCHAR(64) NOT NULL," +
                    "x DOUBLE NOT NULL," +
                    "y DOUBLE NOT NULL," +
                    "z DOUBLE NOT NULL," +
                    "yaw FLOAT DEFAULT 0," +
                    "pitch FLOAT DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(user_uuid, home_name)" +
                    ")");

            // 用户领地表（预留）
            stmt.execute("CREATE TABLE IF NOT EXISTS territories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_uuid VARCHAR(36) NOT NULL," +
                    "name VARCHAR(64) NOT NULL," +
                    "world_name VARCHAR(64) NOT NULL," +
                    "min_x INTEGER NOT NULL," +
                    "min_y INTEGER NOT NULL," +
                    "min_z INTEGER NOT NULL," +
                    "max_x INTEGER NOT NULL," +
                    "max_y INTEGER NOT NULL," +
                    "max_z INTEGER NOT NULL," +
                    "flags TEXT DEFAULT '{}'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(user_uuid, name)" +
                    ")");

            // 领地数据表（新）
            stmt.execute("CREATE TABLE IF NOT EXISTS lands (" +
                    "id VARCHAR(64) PRIMARY KEY," +
                    "owner_uuid VARCHAR(36) NOT NULL," +
                    "name VARCHAR(64) NOT NULL," +
                    "world_name VARCHAR(64) NOT NULL," +
                    "min_x INTEGER NOT NULL," +
                    "max_x INTEGER NOT NULL," +
                    "min_z INTEGER NOT NULL," +
                    "max_z INTEGER NOT NULL," +
                    "members TEXT DEFAULT '{}'," +
                    "member_perms TEXT DEFAULT '{}'," +
                    "default_perms TEXT DEFAULT 'BUILD,DESTROY,INTERACT,CHEST_ACCESS,ANIMALS,ITEM_DROP,ITEM_PICKUP'," +
                    "buy_price DECIMAL(18,2) DEFAULT 100," +
                    "created_time BIGINT DEFAULT 0" +
                    ")");

            // 用户统计数据表（预留）
            stmt.execute("CREATE TABLE IF NOT EXISTS user_stats (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_uuid VARCHAR(36) NOT NULL," +
                    "stat_type VARCHAR(64) NOT NULL," +
                    "stat_value BIGINT DEFAULT 0," +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(user_uuid, stat_type)" +
                    ")");
        }
    }

    private void loadCache() {
        loadWorldCache();
        loadUserCache();
    }

    private void loadWorldCache() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM worlds")) {
            while (rs.next()) {
                WorldData worldData = new WorldData();
                worldData.id = rs.getInt("id");
                worldData.worldName = rs.getString("world_name");
                worldData.displayName = rs.getString("display_name");
                worldData.spawnX = rs.getDouble("spawn_x");
                worldData.spawnY = rs.getDouble("spawn_y");
                worldData.spawnZ = rs.getDouble("spawn_z");
                worldData.spawnYaw = rs.getFloat("spawn_yaw");
                worldData.spawnPitch = rs.getFloat("spawn_pitch");
                worldData.isMainWorld = rs.getBoolean("is_main_world");
                worldData.enabled = rs.getBoolean("enabled");
                worldData.properties = rs.getString("properties");
                worldCache.put(worldData.worldName, worldData);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load world cache: " + e.getMessage());
        }
    }

    private void loadUserCache() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            while (rs.next()) {
                UserData userData = new UserData();
                userData.id = rs.getInt("id");
                userData.uuid = UUID.fromString(rs.getString("uuid"));
                userData.username = rs.getString("username");
                userData.balance = rs.getDouble("balance");
                userData.rtpFreeCount = rs.getInt("rtp_free_count");
                userData.rtpMaxFreeCount = rs.getInt("rtp_max_free_count");
                userData.rtpPrice = rs.getDouble("rtp_price");
                userData.homesLimit = rs.getInt("homes_limit");
                userData.lastLogin = rs.getTimestamp("last_login");
                userData.firstJoin = rs.getTimestamp("first_join");
                userData.playTime = rs.getLong("play_time");
                userData.stats = rs.getString("stats");
                userData.settings = rs.getString("settings");
                userData.extraData = rs.getString("extra_data");
                userCache.put(userData.uuid, userData);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load user cache: " + e.getMessage());
        }
    }

    // ========== 世界数据操作 ==========

    public void saveWorld(WorldData worldData) {
        try {
            String sql = "INSERT OR REPLACE INTO worlds (" +
                    "world_name, display_name, spawn_x, spawn_y, spawn_z, " +
                    "spawn_yaw, spawn_pitch, is_main_world, enabled, properties) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, worldData.worldName);
                pstmt.setString(2, worldData.displayName);
                pstmt.setDouble(3, worldData.spawnX);
                pstmt.setDouble(4, worldData.spawnY);
                pstmt.setDouble(5, worldData.spawnZ);
                pstmt.setFloat(6, worldData.spawnYaw);
                pstmt.setFloat(7, worldData.spawnPitch);
                pstmt.setBoolean(8, worldData.isMainWorld);
                pstmt.setBoolean(9, worldData.enabled);
                pstmt.setString(10, worldData.properties);
                pstmt.executeUpdate();
            }
            worldCache.put(worldData.worldName, worldData);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save world: " + e.getMessage());
        }
    }

    public WorldData getWorld(String worldName) {
        WorldData data = worldCache.get(worldName);
        if (data == null) {
            try {
                String sql = "SELECT * FROM worlds WHERE world_name = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, worldName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            data = new WorldData();
                            data.id = rs.getInt("id");
                            data.worldName = rs.getString("world_name");
                            data.displayName = rs.getString("display_name");
                            data.spawnX = rs.getDouble("spawn_x");
                            data.spawnY = rs.getDouble("spawn_y");
                            data.spawnZ = rs.getDouble("spawn_z");
                            data.spawnYaw = rs.getFloat("spawn_yaw");
                            data.spawnPitch = rs.getFloat("spawn_pitch");
                            data.isMainWorld = rs.getBoolean("is_main_world");
                            data.enabled = rs.getBoolean("enabled");
                            data.properties = rs.getString("properties");
                            worldCache.put(worldName, data);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get world: " + e.getMessage());
            }
        }
        return data;
    }

    public Location getWorldSpawn(String worldName) {
        WorldData data = getWorld(worldName);
        if (data == null) return null;
        World world = Bukkit.getWorld(data.worldName);
        if (world == null) return null;
        return new Location(world, data.spawnX, data.spawnY, data.spawnZ, data.spawnYaw, data.spawnPitch);
    }

    public void setWorldSpawn(String worldName, Location location) {
        WorldData data = getWorld(worldName);
        if (data == null) {
            data = new WorldData();
            data.worldName = worldName;
        }
        data.spawnX = location.getX();
        data.spawnY = location.getY();
        data.spawnZ = location.getZ();
        data.spawnYaw = location.getYaw();
        data.spawnPitch = location.getPitch();
        saveWorld(data);
    }

    // ========== 用户数据操作 ==========

    public UserData getUser(UUID uuid) {
        UserData data = userCache.get(uuid);
        if (data == null) {
            try {
                String sql = "SELECT * FROM users WHERE uuid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            data = new UserData();
                            data.id = rs.getInt("id");
                            data.uuid = uuid;
                            data.username = rs.getString("username");
                            data.balance = rs.getDouble("balance");
                            data.rtpFreeCount = rs.getInt("rtp_free_count");
                            data.rtpMaxFreeCount = rs.getInt("rtp_max_free_count");
                            data.rtpPrice = rs.getDouble("rtp_price");
                            data.homesLimit = rs.getInt("homes_limit");
                            data.lastLogin = rs.getTimestamp("last_login");
                            data.firstJoin = rs.getTimestamp("first_join");
                            data.playTime = rs.getLong("play_time");
                            data.stats = rs.getString("stats");
                            data.settings = rs.getString("settings");
                            data.extraData = rs.getString("extra_data");
                            userCache.put(uuid, data);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get user: " + e.getMessage());
            }
        }
        return data;
    }

    public void saveUser(UserData userData) {
        try {
            String sql = "INSERT OR REPLACE INTO users (" +
                    "uuid, username, balance, rtp_free_count, rtp_max_free_count, " +
                    "rtp_price, homes_limit, last_login, first_join, play_time, " +
                    "stats, settings, extra_data) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userData.uuid.toString());
                pstmt.setString(2, userData.username);
                pstmt.setDouble(3, userData.balance);
                pstmt.setInt(4, userData.rtpFreeCount);
                pstmt.setInt(5, userData.rtpMaxFreeCount);
                pstmt.setDouble(6, userData.rtpPrice);
                pstmt.setInt(7, userData.homesLimit);
                pstmt.setTimestamp(8, userData.lastLogin);
                pstmt.setTimestamp(9, userData.firstJoin);
                pstmt.setLong(10, userData.playTime);
                pstmt.setString(11, userData.stats);
                pstmt.setString(12, userData.settings);
                pstmt.setString(13, userData.extraData);
                pstmt.executeUpdate();
            }
            userCache.put(userData.uuid, userData);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save user: " + e.getMessage());
        }
    }

    public UserData createUser(UUID uuid, String username) {
        UserData data = getUser(uuid);
        if (data != null) {
            data.username = username;
            data.lastLogin = new Timestamp(System.currentTimeMillis());
            saveUser(data);
            return data;
        }

        data = new UserData();
        data.uuid = uuid;
        data.username = username;
        data.balance = 0;
        data.rtpFreeCount = plugin.getConfigManager().getRtpDailyFreeCount();
        data.rtpMaxFreeCount = plugin.getConfigManager().getRtpDailyFreeCount();
        data.rtpPrice = plugin.getConfigManager().getRtpPrice();
        data.homesLimit = plugin.getConfigManager().getMaxHomes();
        data.lastLogin = new Timestamp(System.currentTimeMillis());
        data.firstJoin = new Timestamp(System.currentTimeMillis());
        data.playTime = 0;
        data.stats = "{}";
        data.settings = "{}";
        data.extraData = "{}";
        saveUser(data);
        return data;
    }

    // 经济系统操作
    public boolean withdrawBalance(UUID uuid, double amount) {
        UserData user = getUser(uuid);
        if (user == null || user.balance < amount) return false;
        user.balance -= amount;
        saveUser(user);
        return true;
    }

    public void depositBalance(UUID uuid, double amount) {
        UserData user = getUser(uuid);
        if (user == null) {
            plugin.getLogger().warning("User not found: " + uuid);
            return;
        }
        user.balance += amount;
        saveUser(user);
    }

    public double getBalance(UUID uuid) {
        UserData user = getUser(uuid);
        return user != null ? user.balance : 0;
    }

    // RTP 免费次数操作
    public boolean hasFreeRtp(UUID uuid) {
        UserData user = getUser(uuid);
        return user != null && user.rtpFreeCount > 0;
    }

    public int getRtpFreeCount(UUID uuid) {
        UserData user = getUser(uuid);
        return user != null ? user.rtpFreeCount : 0;
    }

    public void decrementRtpFreeCount(UUID uuid) {
        UserData user = getUser(uuid);
        if (user != null && user.rtpFreeCount > 0) {
            user.rtpFreeCount--;
            saveUser(user);
        }
    }

    public void resetRtpFreeCount(UUID uuid) {
        UserData user = getUser(uuid);
        if (user != null) {
            user.rtpFreeCount = user.rtpMaxFreeCount;
            saveUser(user);
        }
    }

    public void resetAllRtpFreeCounts() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE users SET rtp_free_count = rtp_max_free_count");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to reset RTP free counts: " + e.getMessage());
        }
        userCache.values().forEach(u -> u.rtpFreeCount = u.rtpMaxFreeCount);
    }

    // ========== 家数据操作 ==========

    public void saveHome(UUID userUuid, String homeName, Location location) {
        try {
            String sql = "INSERT OR REPLACE INTO homes (" +
                    "user_uuid, home_name, world_name, x, y, z, yaw, pitch, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userUuid.toString());
                pstmt.setString(2, homeName);
                pstmt.setString(3, location.getWorld().getName());
                pstmt.setDouble(4, location.getX());
                pstmt.setDouble(5, location.getY());
                pstmt.setDouble(6, location.getZ());
                pstmt.setFloat(7, location.getYaw());
                pstmt.setFloat(8, location.getPitch());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save home: " + e.getMessage());
        }
    }

    public Location getHome(UUID userUuid, String homeName) {
        try {
            String sql = "SELECT * FROM homes WHERE user_uuid = ? AND home_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userUuid.toString());
                pstmt.setString(2, homeName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        World world = Bukkit.getWorld(rs.getString("world_name"));
                        if (world == null) return null;
                        return new Location(
                                world,
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getFloat("yaw"),
                                rs.getFloat("pitch")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get home: " + e.getMessage());
        }
        return null;
    }

    public boolean deleteHome(UUID userUuid, String homeName) {
        try {
            String sql = "DELETE FROM homes WHERE user_uuid = ? AND home_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userUuid.toString());
                pstmt.setString(2, homeName);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete home: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Location> getHomes(UUID userUuid) {
        Map<String, Location> homes = new HashMap<>();
        try {
            String sql = "SELECT * FROM homes WHERE user_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userUuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        World world = Bukkit.getWorld(rs.getString("world_name"));
                        if (world == null) continue;
                        Location loc = new Location(
                                world,
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getFloat("yaw"),
                                rs.getFloat("pitch")
                        );
                        homes.put(rs.getString("home_name"), loc);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get homes: " + e.getMessage());
        }
        return homes;
    }

    public int getHomeCount(UUID userUuid) {
        try {
            String sql = "SELECT COUNT(*) FROM homes WHERE user_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userUuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get home count: " + e.getMessage());
        }
        return 0;
    }

    // ========== 领地操作（预留）==========

    public void saveTerritory(UUID userUuid, String name, String worldName,
                              int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        try {
            String sql = "INSERT OR REPLACE INTO territories (" +
                    "user_uuid, name, world_name, min_x, min_y, min_z, max_x, max_y, max_z) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userUuid.toString());
                pstmt.setString(2, name);
                pstmt.setString(3, worldName);
                pstmt.setInt(4, minX);
                pstmt.setInt(5, minY);
                pstmt.setInt(6, minZ);
                pstmt.setInt(7, maxX);
                pstmt.setInt(8, maxY);
                pstmt.setInt(9, maxZ);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save territory: " + e.getMessage());
        }
    }

    // ========== 用户统计操作（预留）==========

    public void incrementStat(UUID userUuid, String statType, long amount) {
        try {
            String sql = "INSERT OR REPLACE INTO user_stats (user_uuid, stat_type, stat_value, last_updated) " +
                    "VALUES (?, ?, COALESCE((SELECT stat_value FROM user_stats WHERE user_uuid = ? AND stat_type = ?), 0) + ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userUuid.toString());
                pstmt.setString(2, statType);
                pstmt.setString(3, userUuid.toString());
                pstmt.setString(4, statType);
                pstmt.setLong(5, amount);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to increment stat: " + e.getMessage());
        }
    }

    public long getStat(UUID userUuid, String statType) {
        try {
            String sql = "SELECT stat_value FROM user_stats WHERE user_uuid = ? AND stat_type = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userUuid.toString());
                pstmt.setString(2, statType);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("stat_value");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get stat: " + e.getMessage());
        }
        return 0;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
        }
    }

    // ========== 数据类 ==========

    public static class WorldData {
        public int id;
        public String worldName;
        public String displayName;
        public double spawnX;
        public double spawnY;
        public double spawnZ;
        public float spawnYaw;
        public float spawnPitch;
        public boolean isMainWorld;
        public boolean enabled;
        public String properties;
    }

    public static class UserData {
        public int id;
        public UUID uuid;
        public String username;
        public double balance;
        public int rtpFreeCount;
        public int rtpMaxFreeCount;
        public double rtpPrice;
        public int homesLimit;
        public Timestamp lastLogin;
        public Timestamp firstJoin;
        public long playTime;
        public String stats;
        public String settings;
        public String extraData;
    }

    // ========== 领地数据操作 ==========

    /**
     * 保存单个领地
     */
    public void saveLand(Land land) {
        try {
            String sql = "INSERT OR REPLACE INTO lands (" +
                    "id, owner_uuid, name, world_name, min_x, max_x, min_z, max_z, " +
                    "members, member_perms, default_perms, buy_price, created_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, land.getId());
                pstmt.setString(2, land.getOwnerId().toString());
                pstmt.setString(3, land.getName());
                pstmt.setString(4, land.getWorld().getName());
                pstmt.setInt(5, land.getMinX());
                pstmt.setInt(6, land.getMaxX());
                pstmt.setInt(7, land.getMinZ());
                pstmt.setInt(8, land.getMaxZ());
                pstmt.setString(9, serializeMembers(land.getMembers()));
                pstmt.setString(10, serializeMemberPermissions(land.getMemberPermissions()));
                pstmt.setString(11, serializeDefaultPermissions(land.getDefaultPermissions()));
                pstmt.setDouble(12, land.getBuyPrice());
                pstmt.setLong(13, land.getCreatedTime());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save land: " + e.getMessage());
        }
    }

    /**
     * 保存所有领地
     */
    public void saveLands(Iterable<Land> lands) {
        for (Land land : lands) {
            saveLand(land);
        }
    }

    /**
     * 删除领地
     */
    public void deleteLand(String landId) {
        try {
            String sql = "DELETE FROM lands WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, landId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete land: " + e.getMessage());
        }
    }

    /**
     * 加载所有领地
     */
    public void loadLands(LandManager landManager) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM lands")) {
            while (rs.next()) {
                try {
                    Land land = deserializeLand(rs);
                    landManager.addLand(land);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load land: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load lands: " + e.getMessage());
        }
    }

    /**
     * 反序列化领地数据
     */
    private Land deserializeLand(ResultSet rs) throws Exception {
        String id = rs.getString("id");
        UUID ownerId = UUID.fromString(rs.getString("owner_uuid"));
        String name = rs.getString("name");
        String worldName = rs.getString("world_name");
        int minX = rs.getInt("min_x");
        int maxX = rs.getInt("max_x");
        int minZ = rs.getInt("min_z");
        int maxZ = rs.getInt("max_z");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new Exception("World not found: " + worldName);
        }

        Land land = new Land(id, ownerId, name, world, minX, maxX, minZ, maxZ);

        // 加载成员
        String membersStr = rs.getString("members");
        if (membersStr != null && !membersStr.isEmpty()) {
            Map<UUID, Land.MemberType> members = deserializeMembers(membersStr);
            for (Map.Entry<UUID, Land.MemberType> entry : members.entrySet()) {
                land.addMember(entry.getKey(), entry.getValue());
            }
        }

        // 加载成员权限
        String permsStr = rs.getString("member_perms");
        if (permsStr != null && !permsStr.isEmpty()) {
            Map<UUID, Set<Land.Permission>> perms = deserializeMemberPermissions(permsStr);
            for (Map.Entry<UUID, Set<Land.Permission>> entry : perms.entrySet()) {
                for (Land.Permission p : entry.getValue()) {
                    land.setMemberPermission(entry.getKey(), p, true);
                }
            }
        }

        // 加载默认权限
        String defaultPermsStr = rs.getString("default_perms");
        if (defaultPermsStr != null && !defaultPermsStr.isEmpty()) {
            Set<Land.Permission> defaultPerms = deserializeDefaultPermissions(defaultPermsStr);
            for (Land.Permission p : defaultPerms) {
                land.setDefaultPermission(p);
            }
        }

        land.setBuyPrice(rs.getDouble("buy_price"));
        return land;
    }

    /**
     * 序列化成员列表
     */
    private String serializeMembers(Map<UUID, Land.MemberType> members) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<UUID, Land.MemberType> entry : members.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey().toString()).append(":").append(entry.getValue().getKey());
        }
        return sb.toString();
    }

    /**
     * 反序列化成员列表
     */
    private Map<UUID, Land.MemberType> deserializeMembers(String str) {
        Map<UUID, Land.MemberType> result = new HashMap<>();
        if (str == null || str.isEmpty()) return result;
        
        for (String part : str.split(";")) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                UUID uuid = UUID.fromString(kv[0]);
                Land.MemberType type = Land.MemberType.fromKey(kv[1]);
                if (type != null) {
                    result.put(uuid, type);
                }
            }
        }
        return result;
    }

    /**
     * 序列化成员权限
     */
    private String serializeMemberPermissions(Map<UUID, Set<Land.Permission>> perms) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<UUID, Set<Land.Permission>> entry : perms.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey().toString()).append(":");
            StringBuilder permSb = new StringBuilder();
            for (Land.Permission p : entry.getValue()) {
                if (permSb.length() > 0) permSb.append(",");
                permSb.append(p.getKey());
            }
            sb.append(permSb.toString());
        }
        return sb.toString();
    }

    /**
     * 反序列化成员权限
     */
    private Map<UUID, Set<Land.Permission>> deserializeMemberPermissions(String str) {
        Map<UUID, Set<Land.Permission>> result = new HashMap<>();
        if (str == null || str.isEmpty()) return result;
        
        for (String part : str.split(";")) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                UUID uuid = UUID.fromString(kv[0]);
                Set<Land.Permission> permSet = new HashSet<>();
                for (String pKey : kv[1].split(",")) {
                    Land.Permission p = Land.Permission.fromKey(pKey);
                    if (p != null) {
                        permSet.add(p);
                    }
                }
                result.put(uuid, permSet);
            }
        }
        return result;
    }

    /**
     * 序列化默认权限
     */
    private String serializeDefaultPermissions(Set<Land.Permission> perms) {
        StringBuilder sb = new StringBuilder();
        for (Land.Permission p : perms) {
            if (sb.length() > 0) sb.append(",");
            sb.append(p.getKey());
        }
        return sb.toString();
    }

    /**
     * 反序列化默认权限
     */
    private Set<Land.Permission> deserializeDefaultPermissions(String str) {
        Set<Land.Permission> result = new HashSet<>();
        if (str == null || str.isEmpty()) return result;
        
        for (String pKey : str.split(",")) {
            Land.Permission p = Land.Permission.fromKey(pKey);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }
}

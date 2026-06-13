package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

public class TeleportPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private HomeManager homeManager;
    private TpaManager tpaManager;
    private DatabaseManager databaseManager;
    private DataMigrationManager migrationManager;
    private LandManager landManager;
    private WaypointManager waypointManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        
        if (configManager.isDatabaseEnabled()) {
            databaseManager = new DatabaseManager(this);
            getLogger().info("Database integration enabled!");
            
            // 初始化数据迁移管理器并执行迁移
            migrationManager = new DataMigrationManager(this);
            migrationManager.checkAndMigrate();
        }
        
        homeManager = new HomeManager(this);
        tpaManager = new TpaManager(this);

        // 初始化地标系统
        if (configManager.isDatabaseEnabled()) {
            waypointManager = new WaypointManager(this);
            getLogger().info("Waypoint system enabled!");
        }

        // 初始化领地系统
        if (configManager.isDatabaseEnabled()) {
            landManager = new LandManager(this);
            getLogger().info("Land system enabled!");
        }

        registerCommands();
        registerListeners();
        scheduleDailyReset();
        
        getLogger().info("TeleportPlugin v2.1.0 enabled!");
    }

    @Override
    public void onDisable() {
        homeManager.saveHomes();
        if (landManager != null) {
            landManager.saveLands();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("TeleportPlugin v2.1.0 disabled!");
    }

    private void scheduleDailyReset() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now(ZoneId.systemDefault());
                if (now.getHour() == 0 && now.getMinute() == 0) {
                    if (databaseManager != null) {
                        databaseManager.resetAllRtpFreeCounts();
                        getLogger().info("Daily RTP free counts reset!");
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 0, 20 * 60);
    }

    private void registerCommands() {
        new SpawnCommand(this);
        new HomeCommand(this);
        new RtpCommand(this);
        new TpaCommand(this);
        new EconomyCommand(this);
        new AdminCommand(this);
        new ExchangeCommand(this);
        new AnnouncementCommand(this);
        
        // 注册世界保护
        WorldProtectionListener protection = new WorldProtectionListener(this);
        new ProtectionCommand(this, protection);

        // 注册领地系统
        if (landManager != null) {
            new LandCommand(this);
        }

        // 注册地标系统
        if (waypointManager != null) {
            new WaypointCommand(this);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        // 注册领地监听器
        if (landManager != null) {
            getServer().getPluginManager().registerEvents(new LandListener(this), this);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DataMigrationManager getMigrationManager() {
        return migrationManager;
    }
    
    public LandManager getLandManager() {
        return landManager;
    }

    public WaypointManager getWaypointManager() {
        return waypointManager;
    }
}
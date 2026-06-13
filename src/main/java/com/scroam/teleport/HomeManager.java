package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeManager {

    private final TeleportPlugin plugin;
    private final Map<UUID, Map<String, Location>> playerHomes = Collections.synchronizedMap(new HashMap<>());
    private File homesFile;

    public HomeManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        homesFile = new File(plugin.getDataFolder(), "homes.yml");
        loadHomes();
    }

    public void loadHomes() {
        if (!homesFile.exists()) {
            return;
        }
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(homesFile);
            for (String uuidStr : config.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Map<String, Location> homes = new HashMap<>();
                    var section = config.getConfigurationSection(uuidStr);
                    if (section == null) {
                        continue;
                    }
                    for (String homeName : section.getKeys(false)) {
                        try {
                            String path = uuidStr + "." + homeName;
                            String worldName = config.getString(path + ".world");
                            if (worldName == null) {
                                continue;
                            }
                            World world = Bukkit.getWorld(worldName);
                            if (world == null) {
                                plugin.getLogger().warning("World " + worldName + " not found for home " + homeName);
                                continue;
                            }
                            double x = config.getDouble(path + ".x", 0);
                            double y = config.getDouble(path + ".y", 64);
                            double z = config.getDouble(path + ".z", 0);
                            float yaw = (float) config.getDouble(path + ".yaw", 0);
                            float pitch = (float) config.getDouble(path + ".pitch", 0);
                            Location loc = new Location(world, x, y, z, yaw, pitch);
                            homes.put(homeName, loc);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load home " + homeName + ": " + e.getMessage());
                        }
                    }
                    playerHomes.put(uuid, homes);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID: " + uuidStr);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load homes: " + e.getMessage());
        }
    }

    public void saveHomes() {
        try {
            FileConfiguration config = new YamlConfiguration();
            synchronized (playerHomes) {
                for (Map.Entry<UUID, Map<String, Location>> entry : playerHomes.entrySet()) {
                    String uuidStr = entry.getKey().toString();
                    for (Map.Entry<String, Location> homeEntry : entry.getValue().entrySet()) {
                        Location loc = homeEntry.getValue();
                        String path = uuidStr + "." + homeEntry.getKey();
                        config.set(path + ".world", loc.getWorld().getName());
                        config.set(path + ".x", loc.getX());
                        config.set(path + ".y", loc.getY());
                        config.set(path + ".z", loc.getZ());
                        config.set(path + ".yaw", loc.getYaw());
                        config.set(path + ".pitch", loc.getPitch());
                    }
                }
            }
            config.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save homes: " + e.getMessage());
        }
    }

    public Map<String, Location> getHomes(UUID playerId) {
        return playerHomes.computeIfAbsent(playerId, k -> Collections.synchronizedMap(new HashMap<>()));
    }

    public boolean setHome(UUID playerId, String homeName, Location location) {
        if (homeName == null || homeName.isEmpty() || homeName.length() > 32) {
            return false;
        }
        Map<String, Location> homes = getHomes(playerId);
        synchronized (homes) {
            if (homes.size() >= plugin.getConfigManager().getMaxHomes() && !homes.containsKey(homeName)) {
                return false;
            }
            homes.put(homeName, location);
        }
        saveHomes();
        return true;
    }

    public boolean removeHome(UUID playerId, String homeName) {
        if (homeName == null || homeName.isEmpty()) {
            return false;
        }
        Map<String, Location> homes = getHomes(playerId);
        synchronized (homes) {
            boolean removed = homes.remove(homeName) != null;
            if (removed) {
                saveHomes();
            }
            return removed;
        }
    }

    public Location getHome(UUID playerId, String homeName) {
        if (homeName == null || homeName.isEmpty()) {
            return null;
        }
        Map<String, Location> homes = getHomes(playerId);
        synchronized (homes) {
            Location loc = homes.get(homeName);
            if (loc != null && loc.getWorld() == null) {
                homes.remove(homeName);
                return null;
            }
            return loc;
        }
    }
}
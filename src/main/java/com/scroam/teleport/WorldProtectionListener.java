package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 世界主城保护系统
 */
public class WorldProtectionListener implements Listener {

    private final TeleportPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 1000; // 1秒冷却

    public WorldProtectionListener(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 检查玩家是否在保护区域内
     */
    public boolean isInProtectionZone(Player player) {
        if (!isProtectionEnabled()) {
            return false;
        }

        // 检查是否在世界主城所在世界
        Location spawnLoc = plugin.getConfigManager().getSpawnLocation();
        if (spawnLoc == null) {
            return false;
        }

        if (!player.getWorld().equals(spawnLoc.getWorld())) {
            return false;
        }

        // 检查距离
        double radius = getProtectionRadius();
        double distance = player.getLocation().distance(spawnLoc);
        
        return distance <= radius;
    }

    /**
     * 检查玩家是否有保护豁免权限
     */
    public boolean hasProtectionExemption(Player player) {
        // OP 永远有权限
        if (player.isOp()) {
            return true;
        }

        // 检查是否在允许列表中
        List<String> allowedPlayers = plugin.getConfig().getStringList("world-protection.allowed-players");
        return allowedPlayers.contains(player.getUniqueId().toString()) ||
               allowedPlayers.contains(player.getName());
    }

    /**
     * 保护是否启用
     */
    private boolean isProtectionEnabled() {
        return plugin.getConfig().getBoolean("world-protection.enabled", true);
    }

    /**
     * 获取保护半径
     */
    private int getProtectionRadius() {
        return plugin.getConfig().getInt("world-protection.spawn-protection-radius", 100);
    }

    /**
     * 检查是否启用特定保护
     */
    private boolean isProtectionEnabled(String protectionType) {
        return plugin.getConfig().getBoolean("world-protection.protection." + protectionType, true);
    }

    /**
     * 发送冷却消息（防止刷屏）
     */
    private void sendCooldownMessage(Player player, String action) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        if (cooldowns.containsKey(uuid)) {
            long lastMessage = cooldowns.get(uuid);
            if (now - lastMessage < COOLDOWN_TIME) {
                return;
            }
        }
        
        cooldowns.put(uuid, now);
        player.sendMessage(ChatColor.RED + "这里受到世界主城保护，禁止 " + action + "！");
    }

    /**
     * 监听方块破坏事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isProtectionEnabled("block-break")) {
            return;
        }

        Player player = event.getPlayer();
        if (isInProtectionZone(player) && !hasProtectionExemption(player)) {
            event.setCancelled(true);
            sendCooldownMessage(player, "破坏方块");
        }
    }

    /**
     * 监听方块放置事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isProtectionEnabled("block-place")) {
            return;
        }

        Player player = event.getPlayer();
        if (isInProtectionZone(player) && !hasProtectionExemption(player)) {
            event.setCancelled(true);
            sendCooldownMessage(player, "放置方块");
        }
    }

    /**
     * 监听实体伤害事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isProtectionEnabled("entity-damage")) {
            return;
        }

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            Entity victim = event.getEntity();
            Entity attacker = damageEvent.getDamager();

            // 检查受害者是否在保护区内
            if (victim instanceof Player) {
                Player victimPlayer = (Player) victim;
                if (isInProtectionZone(victimPlayer) && !hasProtectionExemption(victimPlayer)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // 检查攻击者是否在保护区内
            if (attacker instanceof Player) {
                Player attackerPlayer = (Player) attacker;
                if (isInProtectionZone(attackerPlayer) && !hasProtectionExemption(attackerPlayer)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } else {
            // 非玩家伤害（如岩浆、仙人掌等）
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if (isInProtectionZone(player)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * 监听PVP伤害
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPVP(EntityDamageByEntityEvent event) {
        if (!isProtectionEnabled("pvp")) {
            return;
        }

        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (isInProtectionZone(victim) && !hasProtectionExemption(victim)) {
            event.setCancelled(true);
            if (!cooldowns.containsKey(victim.getUniqueId()) || 
                System.currentTimeMillis() - cooldowns.get(victim.getUniqueId()) > COOLDOWN_TIME) {
                victim.sendMessage(ChatColor.RED + "这里受到世界主城保护，禁止PVP！");
                cooldowns.put(victim.getUniqueId(), System.currentTimeMillis());
            }
        }

        if (isInProtectionZone(attacker) && !hasProtectionExemption(attacker)) {
            event.setCancelled(true);
            if (!cooldowns.containsKey(attacker.getUniqueId()) || 
                System.currentTimeMillis() - cooldowns.get(attacker.getUniqueId()) > COOLDOWN_TIME) {
                attacker.sendMessage(ChatColor.RED + "这里受到世界主城保护，禁止PVP！");
                cooldowns.put(attacker.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    /**
     * 监听箱子打开事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isProtectionEnabled("chest-access")) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (isInProtectionZone(player) && !hasProtectionExemption(player)) {
            event.setCancelled(true);
            sendCooldownMessage(player, "打开箱子");
        }
    }

    /**
     * 监听玩家交互事件（门、按钮、拉杆等）
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        if (!isInProtectionZone(player) || hasProtectionExemption(player)) {
            return;
        }

        // 检查门、按钮、拉杆等
        if (isProtectionEnabled("door-interact")) {
            switch (event.getClickedBlock().getType()) {
                // 门类
                case OAK_DOOR:
                case SPRUCE_DOOR:
                case BIRCH_DOOR:
                case JUNGLE_DOOR:
                case ACACIA_DOOR:
                case DARK_OAK_DOOR:
                case CRIMSON_DOOR:
                case WARPED_DOOR:
                case OAK_FENCE_GATE:
                case SPRUCE_FENCE_GATE:
                case BIRCH_FENCE_GATE:
                case JUNGLE_FENCE_GATE:
                case ACACIA_FENCE_GATE:
                case DARK_OAK_FENCE_GATE:
                case CRIMSON_FENCE_GATE:
                case WARPED_FENCE_GATE:
                // 按钮
                case OAK_BUTTON:
                case SPRUCE_BUTTON:
                case BIRCH_BUTTON:
                case JUNGLE_BUTTON:
                case ACACIA_BUTTON:
                case DARK_OAK_BUTTON:
                case CRIMSON_BUTTON:
                case WARPED_BUTTON:
                case STONE_BUTTON:
                // 拉杆
                case LEVER:
                // 压力板
                case OAK_PRESSURE_PLATE:
                case SPRUCE_PRESSURE_PLATE:
                case BIRCH_PRESSURE_PLATE:
                case JUNGLE_PRESSURE_PLATE:
                case ACACIA_PRESSURE_PLATE:
                case DARK_OAK_PRESSURE_PLATE:
                case CRIMSON_PRESSURE_PLATE:
                case WARPED_PRESSURE_PLATE:
                case STONE_PRESSURE_PLATE:
                case HEAVY_WEIGHTED_PRESSURE_PLATE:
                case LIGHT_WEIGHTED_PRESSURE_PLATE:
                // 音符盒
                case NOTE_BLOCK:
                // 蛋糕
                case CAKE:
                    event.setCancelled(true);
                    sendCooldownMessage(player, "使用交互方块");
                    return;
            }
        }

        // 检查桶
        if (isProtectionEnabled("bucket-use")) {
            switch (event.getClickedBlock().getType()) {
                case BARREL:
                case DISPENSER:
                case DROPPER:
                case FURNACE:
                case BLAST_FURNACE:
                case SMOKER:
                case HOPPER:
                case BREWING_STAND:
                case ENCHANTING_TABLE:
                case ANVIL:
                case CHIPPED_ANVIL:
                case DAMAGED_ANVIL:
                case GRINDSTONE:
                case STONECUTTER:
                case CARTOGRAPHY_TABLE:
                case FLETCHING_TABLE:
                case SMITHING_TABLE:
                case LOOM:
                case CARVED_PUMPKIN:
                case JACK_O_LANTERN:
                case BEACON:
                case RESPAWN_ANCHOR:
                    event.setCancelled(true);
                    sendCooldownMessage(player, "使用容器");
                    return;
            }
        }
    }

    /**
     * 监听爆炸事件
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityDamageEvent event) {
        if (!isProtectionEnabled("explosion-damage")) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isInProtectionZone(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 监听玩家移动（可选：防止进入保护区）
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 只检查实际移动（不是视角转动）
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (hasProtectionExemption(player)) {
            return;
        }

        // 检查是否从区外进入区内
        boolean fromProtected = false;
        boolean toProtected = false;

        Location spawnLoc = plugin.getConfigManager().getSpawnLocation();
        if (spawnLoc != null && player.getWorld().equals(spawnLoc.getWorld())) {
            double radius = getProtectionRadius();
            fromProtected = event.getFrom().distance(spawnLoc) <= radius;
            toProtected = event.getTo().distance(spawnLoc) <= radius;
        }

        // 如果是进入保护区，给出提示
        if (!fromProtected && toProtected && isProtectionEnabled()) {
            player.sendMessage(ChatColor.YELLOW + "你进入了世界主城保护区，某些操作可能被限制。");
        }
    }

    /**
     * 清理离线玩家的冷却数据
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 添加允许的玩家
     */
    public boolean addAllowedPlayer(String identifier) {
        List<String> allowedPlayers = plugin.getConfig().getStringList("world-protection.allowed-players");
        
        // 尝试解析为UUID
        try {
            UUID uuid = UUID.fromString(identifier);
            if (!allowedPlayers.contains(uuid.toString())) {
                allowedPlayers.add(uuid.toString());
                plugin.getConfig().set("world-protection.allowed-players", allowedPlayers);
                plugin.saveConfig();
                return true;
            }
        } catch (IllegalArgumentException e) {
            // 不是UUID，尝试作为玩家名处理
            if (!allowedPlayers.contains(identifier)) {
                allowedPlayers.add(identifier);
                plugin.getConfig().set("world-protection.allowed-players", allowedPlayers);
                plugin.saveConfig();
                return true;
            }
        }
        return false;
    }

    /**
     * 移除允许的玩家
     */
    public boolean removeAllowedPlayer(String identifier) {
        List<String> allowedPlayers = plugin.getConfig().getStringList("world-protection.allowed-players");
        
        if (allowedPlayers.remove(identifier)) {
            plugin.getConfig().set("world-protection.allowed-players", allowedPlayers);
            plugin.saveConfig();
            return true;
        }
        
        // 尝试用UUID格式移除
        try {
            UUID uuid = UUID.fromString(identifier);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                allowedPlayers.remove(player.getName());
                plugin.getConfig().set("world-protection.allowed-players", allowedPlayers);
                plugin.saveConfig();
                return true;
            }
        } catch (IllegalArgumentException ignored) {}
        
        return false;
    }

    /**
     * 获取允许的玩家列表
     */
    public List<String> getAllowedPlayers() {
        return new ArrayList<>(plugin.getConfig().getStringList("world-protection.allowed-players"));
    }
}
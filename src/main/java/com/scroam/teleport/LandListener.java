package com.scroam.teleport;

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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

/**
 * 领地事件监听器 - 处理领地权限保护
 */
public class LandListener implements Listener {

    private final TeleportPlugin plugin;

    public LandListener(TeleportPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 检查玩家在领地的权限
     */
    private boolean checkLandPermission(Player player, Location loc, Land.Permission permission) {
        Land land = plugin.getLandManager().getLandAt(loc);
        if (land == null) {
            return true; // 不在任何领地内，允许
        }
        
        // 检查是否是管理员（绕过领地限制）
        if (player.hasPermission("teleport.admin")) {
            return true;
        }
        
        // 检查权限
        if (!land.hasPermission(player.getUniqueId(), permission)) {
            player.sendMessage(ChatColor.RED + "你没有 [" + permission.getDescription() + "] 权限！");
            return false;
        }
        
        return true;
    }

    /**
     * 检查PVP权限
     */
    private boolean checkPVPPermission(Player attacker, Player victim) {
        Location attackerLoc = attacker.getLocation();
        Location victimLoc = victim.getLocation();
        
        Land attackerLand = plugin.getLandManager().getLandAt(attackerLoc);
        Land victimLand = plugin.getLandManager().getLandAt(victimLoc);
        
        // 如果攻击者在领地内
        if (attackerLand != null) {
            if (attacker.hasPermission("teleport.admin")) {
                return true;
            }
            if (!attackerLand.hasPermission(attacker.getUniqueId(), Land.Permission.PVP)) {
                attacker.sendMessage(ChatColor.RED + "你没有 [PVP权限] ！");
                return false;
            }
        }
        
        // 如果受害者在领地内
        if (victimLand != null) {
            if (attacker.hasPermission("teleport.admin")) {
                return true;
            }
            if (!victimLand.hasPermission(victim.getUniqueId(), Land.Permission.PVP)) {
                attacker.sendMessage(ChatColor.RED + "目标在受保护的领地内，无法攻击！");
                return false;
            }
        }
        
        return true;
    }

    /**
     * 方块破坏事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        
        if (!checkLandPermission(player, loc, Land.Permission.DESTROY)) {
            event.setCancelled(true);
        }
    }

    /**
     * 方块放置事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        
        if (!checkLandPermission(player, loc, Land.Permission.BUILD)) {
            event.setCancelled(true);
        }
    }

    /**
     * 玩家交互事件（门、按钮、箱子等）
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) {
            return;
        }
        
        Location loc = event.getClickedBlock().getLocation();
        Land land = plugin.getLandManager().getLandAt(loc);
        
        if (land == null) {
            return;
        }
        
        // 检查是否是管理员
        if (player.hasPermission("teleport.admin")) {
            return;
        }
        
        // 检查交互权限
        if (!land.hasPermission(player.getUniqueId(), Land.Permission.INTERACT)) {
            player.sendMessage(ChatColor.RED + "你没有 [交互权限] ！");
            event.setCancelled(true);
            return;
        }
        
        // 检查是否是箱子等需要特殊权限的方块
        org.bukkit.Material mat = event.getClickedBlock().getType();
        if (isContainer(mat)) {
            if (!land.hasPermission(player.getUniqueId(), Land.Permission.CHEST_ACCESS)) {
                player.sendMessage(ChatColor.RED + "你没有 [箱子权限] ！");
                event.setCancelled(true);
            }
        }
    }

    /**
     * 实体伤害事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Location loc = victim.getLocation();
        Land land = plugin.getLandManager().getLandAt(loc);
        
        if (land == null) {
            return;
        }
        
        // 检查是否是管理员
        if (victim.hasPermission("teleport.admin")) {
            return;
        }
        
        // 检查实体伤害权限（不包括PVP）
        if (!land.hasPermission(victim.getUniqueId(), Land.Permission.INTERACT)) {
            event.setCancelled(true);
        }
    }

    /**
     * 玩家攻击事件（PVP）
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity attackerEntity = event.getDamager();
        Entity victimEntity = event.getEntity();
        
        // 检查是否是玩家攻击玩家
        if (!(attackerEntity instanceof Player) || !(victimEntity instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) attackerEntity;
        Player victim = (Player) victimEntity;
        
        if (!checkPVPPermission(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    /**
     * 打开箱子事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // 只检查箱子类型的容器
        if (event.getInventory().getType() != InventoryType.CHEST && 
            event.getInventory().getType() != InventoryType.ENDER_CHEST &&
            event.getInventory().getType() != InventoryType.BARREL) {
            return;
        }
        
        Location loc = event.getInventory().getLocation();
        if (loc == null) {
            return;
        }
        
        Land land = plugin.getLandManager().getLandAt(loc);
        if (land == null) {
            return;
        }
        
        // 检查是否是管理员
        if (player.hasPermission("teleport.admin")) {
            return;
        }
        
        // 检查箱子权限
        if (!land.hasPermission(player.getUniqueId(), Land.Permission.CHEST_ACCESS)) {
            player.sendMessage(ChatColor.RED + "你没有 [箱子权限] ！");
            event.setCancelled(true);
        }
    }

    /**
     * 物品掉落事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getItemDrop().getLocation();
        
        Land land = plugin.getLandManager().getLandAt(loc);
        if (land == null) {
            return;
        }
        
        // 检查是否是管理员
        if (player.hasPermission("teleport.admin")) {
            return;
        }
        
        // 检查物品掉落权限
        if (!land.hasPermission(player.getUniqueId(), Land.Permission.ITEM_DROP)) {
            player.sendMessage(ChatColor.RED + "你没有 [物品掉落权限] ！");
            event.setCancelled(true);
        }
    }

    /**
     * 物品拾取事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getItem().getLocation();
        
        Land land = plugin.getLandManager().getLandAt(loc);
        if (land == null) {
            return;
        }
        
        // 检查是否是管理员
        if (player.hasPermission("teleport.admin")) {
            return;
        }
        
        // 检查物品拾取权限
        if (!land.hasPermission(player.getUniqueId(), Land.Permission.ITEM_PICKUP)) {
            event.setCancelled(true);
        }
    }

    /**
     * 判断是否是容器方块
     */
    private boolean isContainer(org.bukkit.Material mat) {
        return mat == org.bukkit.Material.CHEST ||
               mat == org.bukkit.Material.BARREL ||
               mat == org.bukkit.Material.FURNACE ||
               mat == org.bukkit.Material.HOPPER ||
               mat == org.bukkit.Material.DROPPER ||
               mat == org.bukkit.Material.DISPENSER ||
               mat == org.bukkit.Material.BLAST_FURNACE ||
               mat == org.bukkit.Material.SMOKER ||
               mat == org.bukkit.Material.BREWING_STAND ||
               mat == org.bukkit.Material.BEACON ||
               mat == org.bukkit.Material.ANVIL ||
               mat == org.bukkit.Material.CHIPPED_ANVIL ||
               mat == org.bukkit.Material.DAMAGED_ANVIL ||
               mat == org.bukkit.Material.ENDER_CHEST ||
               mat == org.bukkit.Material.SHULKER_BOX;
    }
}

package com.scroam.teleport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公告系统 - 全局公告和告示牌管理
 */
public class AnnouncementCommand implements CommandExecutor, Listener {

    private final TeleportPlugin plugin;
    private final Map<Location, String> registeredSigns = new HashMap<>();
    private final List<String> announcements = new ArrayList<>();

    public AnnouncementCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("announce").setExecutor(this);
        plugin.getCommand("broadcast").setExecutor(this);
        plugin.getCommand("setmotd").setExecutor(this);
        plugin.getCommand("setsign").setExecutor(this);
        plugin.getCommand("registersign").setExecutor(this);
        plugin.getCommand("unregistersign").setExecutor(this);
        plugin.getCommand("signlist").setExecutor(this);
        plugin.getCommand("addannouncement").setExecutor(this);
        plugin.getCommand("removeannouncement").setExecutor(this);
        plugin.getCommand("announcementlist").setExecutor(this);
        
        // 注册监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // 加载已注册的告示牌
        loadSigns();
        loadAnnouncements();
        
        // 更新MOTD给所有在线玩家
        updateMotdForAll();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String commandName = cmd.getName().toLowerCase();

        switch (commandName) {
            case "announce":
            case "broadcast":
                return handleAnnounce(sender, args);
            case "setmotd":
                return handleSetMotd(sender, args);
            case "setsign":
                return handleSetSign(sender, args);
            case "registersign":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
                    return true;
                }
                return handleRegisterSign((Player) sender);
            case "unregistersign":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
                    return true;
                }
                return handleUnregisterSign((Player) sender);
            case "signlist":
                return handleSignList(sender);
            case "addannouncement":
                return handleAddAnnouncement(sender, args);
            case "removeannouncement":
                return handleRemoveAnnouncement(sender, args);
            case "announcementlist":
                return handleAnnouncementList(sender);
            default:
                return false;
        }
    }

    /**
     * 发送全局公告
     */
    private boolean handleAnnounce(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /announce <消息内容>");
            return true;
        }

        String message = ChatColor.translateAlternateColorCodes('&', 
            ChatColor.GOLD + "[公告] " + ChatColor.WHITE + String.join(" ", args));

        // 发送给所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            player.sendTitle(ChatColor.GOLD + "服务器公告", 
                ChatColor.WHITE + String.join(" ", args), 10, 70, 20);
        }

        sender.sendMessage(ChatColor.GREEN + "公告已发送!");
        return true;
    }

    /**
     * 设置MOTD
     */
    private boolean handleSetMotd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /setmotd <MOTD内容>");
            return true;
        }

        String motd = String.join(" ", args);
        plugin.getConfig().set("announcements.motd", motd);
        plugin.saveConfig();
        
        // 更新所有在线玩家的标签
        updateMotdForAll();
        
        sender.sendMessage(ChatColor.GREEN + "MOTD已设置: " + ChatColor.WHITE + motd);
        return true;
    }

    /**
     * 更新所有玩家的MOTD
     */
    private void updateMotdForAll() {
        String motd = plugin.getConfig().getString("announcements.motd", "欢迎来到服务器");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter(ChatColor.GOLD + motd + "\n", "");
        }
    }

    /**
     * 设置告示牌内容
     */
    private boolean handleSetSign(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("teleport.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null || !isSign(block.getType())) {
            player.sendMessage(ChatColor.RED + "请看向一个告示牌!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /setsign <行1> [行2] [行3] [行4]");
            player.sendMessage(ChatColor.YELLOW + "使用 & 符号表示颜色代码，如 &a 表示绿色");
            return true;
        }

        Sign sign = (Sign) block.getState();
        
        // 设置4行内容
        for (int i = 0; i < 4; i++) {
            if (i < args.length) {
                String line = ChatColor.translateAlternateColorCodes('&', args[i]);
                if (line.length() > 15) {
                    line = line.substring(0, 15);
                }
                sign.setLine(i, line);
            } else {
                sign.setLine(i, "");
            }
        }
        
        sign.update();
        player.sendMessage(ChatColor.GREEN + "告示牌内容已更新!");
        return true;
    }

    /**
     * 注册告示牌
     */
    private boolean handleRegisterSign(Player player) {
        if (!player.hasPermission("teleport.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null || !isSign(block.getType())) {
            player.sendMessage(ChatColor.RED + "请看向一个告示牌!");
            return true;
        }

        Location loc = block.getLocation();
        if (registeredSigns.containsKey(loc)) {
            player.sendMessage(ChatColor.RED + "此告示牌已注册!");
            return true;
        }

        registeredSigns.put(loc, loc.getWorld().getName());
        saveSigns();

        // 更新告示牌内容
        updateSignContent(block);

        player.sendMessage(ChatColor.GREEN + "告示牌已注册为公告板!");
        return true;
    }

    /**
     * 取消注册告示牌
     */
    private boolean handleUnregisterSign(Player player) {
        if (!player.hasPermission("teleport.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null || !isSign(block.getType())) {
            player.sendMessage(ChatColor.RED + "请看向一个告示牌!");
            return true;
        }

        Location loc = block.getLocation();
        if (!registeredSigns.containsKey(loc)) {
            player.sendMessage(ChatColor.RED + "此告示牌未注册!");
            return true;
        }

        registeredSigns.remove(loc);
        saveSigns();

        player.sendMessage(ChatColor.GREEN + "告示牌已取消注册!");
        return true;
    }

    /**
     * 显示已注册告示牌列表
     */
    private boolean handleSignList(CommandSender sender) {
        if (!sender.hasPermission("teleport.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (registeredSigns.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "暂无已注册的告示牌");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "=== 已注册告示牌列表 ===");
        int index = 1;
        for (Map.Entry<Location, String> entry : registeredSigns.entrySet()) {
            Location loc = entry.getKey();
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(index) + ". 世界: " + entry.getValue() + 
                " | 位置: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            index++;
        }
        return true;
    }

    /**
     * 添加公告内容
     */
    private boolean handleAddAnnouncement(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /addannouncement <公告内容>");
            return true;
        }

        String announcement = String.join(" ", args);
        announcements.add(announcement);
        saveAnnouncements();

        // 更新所有告示牌
        updateAllSigns();

        sender.sendMessage(ChatColor.GREEN + "公告已添加! 当前共有 " + announcements.size() + " 条公告");
        return true;
    }

    /**
     * 删除公告内容
     */
    private boolean handleRemoveAnnouncement(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /removeannouncement <序号>");
            return true;
        }

        try {
            int index = Integer.parseInt(args[0]) - 1;
            if (index < 0 || index >= announcements.size()) {
                sender.sendMessage(ChatColor.RED + "无效的序号! 当前共有 " + announcements.size() + " 条公告");
                return true;
            }

            String removed = announcements.remove(index);
            saveAnnouncements();

            // 更新所有告示牌
            updateAllSigns();

            sender.sendMessage(ChatColor.GREEN + "已删除公告: " + ChatColor.WHITE + removed);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的序号!");
            return true;
        }
    }

    /**
     * 显示公告列表
     */
    private boolean handleAnnouncementList(CommandSender sender) {
        if (!sender.hasPermission("teleport.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (announcements.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "暂无公告");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "=== 公告列表 ===");
        int index = 1;
        for (String announcement : announcements) {
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(index) + ". " + ChatColor.WHITE + announcement);
            index++;
        }
        sender.sendMessage(ChatColor.YELLOW + "使用 /removeannouncement <序号> 删除公告");
        return true;
    }

    /**
     * 监听告示牌创建事件
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Location loc = event.getBlock().getLocation();
        if (registeredSigns.containsKey(loc)) {
            // 如果是已注册的告示牌，阻止普通玩家修改
            if (!event.getPlayer().hasPermission("teleport.admin")) {
                event.getPlayer().sendMessage(ChatColor.RED + "这是公告板，你无法修改!");
                event.setCancelled(true);
            }
        }
    }

    /**
     * 监听方块破坏事件
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        
        if (registeredSigns.containsKey(loc)) {
            if (!event.getPlayer().hasPermission("teleport.admin")) {
                event.getPlayer().sendMessage(ChatColor.RED + "这是公告板，你无法破坏!");
                event.setCancelled(true);
            } else {
                registeredSigns.remove(loc);
                saveSigns();
                event.getPlayer().sendMessage(ChatColor.YELLOW + "公告板已被移除!");
            }
        }
    }

    /**
     * 更新告示牌内容
     */
    private void updateSignContent(Block block) {
        if (!isSign(block.getType())) return;
        
        Sign sign = (Sign) block.getState();
        
        if (announcements.isEmpty()) {
            sign.setLine(0, ChatColor.GOLD + "[公告板]");
            sign.setLine(1, ChatColor.YELLOW + "暂无公告");
            sign.setLine(2, "");
            sign.setLine(3, "");
        } else {
            // 显示最新的公告（最多4行）
            sign.setLine(0, ChatColor.GOLD + "[公告板]");
            int lineIndex = 1;
            for (int i = 0; i < Math.min(3, announcements.size()); i++) {
                String announcement = announcements.get(i);
                if (announcement.length() > 15) {
                    announcement = announcement.substring(0, 15);
                }
                sign.setLine(lineIndex, ChatColor.WHITE + announcement);
                lineIndex++;
            }
            while (lineIndex < 4) {
                sign.setLine(lineIndex, "");
                lineIndex++;
            }
        }
        
        sign.update();
    }

    /**
     * 更新所有已注册告示牌
     */
    private void updateAllSigns() {
        for (Location loc : registeredSigns.keySet()) {
            Block block = loc.getBlock();
            if (block != null && isSign(block.getType())) {
                updateSignContent(block);
            }
        }
    }

    /**
     * 检查是否是告示牌
     */
    private boolean isSign(Material material) {
        return material == Material.OAK_SIGN || 
               material == Material.SPRUCE_SIGN ||
               material == Material.BIRCH_SIGN ||
               material == Material.JUNGLE_SIGN ||
               material == Material.ACACIA_SIGN ||
               material == Material.DARK_OAK_SIGN ||
               material == Material.CRIMSON_SIGN ||
               material == Material.WARPED_SIGN ||
               material == Material.OAK_WALL_SIGN ||
               material == Material.SPRUCE_WALL_SIGN ||
               material == Material.BIRCH_WALL_SIGN ||
               material == Material.JUNGLE_WALL_SIGN ||
               material == Material.ACACIA_WALL_SIGN ||
               material == Material.DARK_OAK_WALL_SIGN ||
               material == Material.CRIMSON_WALL_SIGN ||
               material == Material.WARPED_WALL_SIGN;
    }

    /**
     * 加载已注册的告示牌
     */
    private void loadSigns() {
        List<String> signList = plugin.getConfig().getStringList("announcements.signs");
        for (String signData : signList) {
            try {
                String[] parts = signData.split(",");
                if (parts.length >= 4) {
                    String world = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    
                    org.bukkit.World bukkitWorld = plugin.getServer().getWorld(world);
                    if (bukkitWorld != null) {
                        Location loc = new Location(bukkitWorld, x, y, z);
                        registeredSigns.put(loc, world);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load sign: " + signData);
            }
        }
    }

    /**
     * 保存已注册的告示牌
     */
    private void saveSigns() {
        List<String> signList = new ArrayList<>();
        for (Map.Entry<Location, String> entry : registeredSigns.entrySet()) {
            Location loc = entry.getKey();
            signList.add(entry.getValue() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
        plugin.getConfig().set("announcements.signs", signList);
        plugin.saveConfig();
    }

    /**
     * 加载公告列表
     */
    private void loadAnnouncements() {
        // 确保配置有默认值
        if (!plugin.getConfig().contains("announcements.list")) {
            plugin.getConfig().set("announcements.list", new ArrayList<String>());
            plugin.saveConfig();
        }
        
        List<String> list = plugin.getConfig().getStringList("announcements.list");
        announcements.clear();
        announcements.addAll(list);
        
        plugin.getLogger().info("Loaded " + announcements.size() + " announcements");
    }

    /**
     * 保存公告列表
     */
    private void saveAnnouncements() {
        plugin.getConfig().set("announcements.list", announcements);
        plugin.saveConfig();
        plugin.getLogger().info("Saved " + announcements.size() + " announcements");
    }
}
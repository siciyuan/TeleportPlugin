package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * 领地命令处理
 */
public class LandCommand implements CommandExecutor {

    private final TeleportPlugin plugin;

    public LandCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("land").setExecutor(this);
        plugin.getCommand("landlord").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        LandManager landManager = plugin.getLandManager();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "buy":
                return handleBuy(player, args, landManager);
            case "sell":
                return handleSell(player, args, landManager);
            case "add":
                return handleAdd(player, args, landManager);
            case "remove":
                return handleRemove(player, args, landManager);
            case "list":
                return handleList(player, landManager);
            case "info":
                return handleInfo(player, landManager);
            case "perm":
            case "permission":
                return handlePermission(player, args, landManager);
            case "set":
                return handleSet(player, args, landManager);
            case "tp":
            case "goto":
                return handleTeleport(player, args, landManager);
            case "help":
                sendHelp(player);
                return true;
            default:
                player.sendMessage(ChatColor.RED + "未知命令！使用 /land help 查看帮助。");
                return true;
        }
    }

    /**
     * 购买领地
     * /land buy [r=半径]
     */
    private boolean handleBuy(Player player, String[] args, LandManager landManager) {
        int radius = 5; // 默认半径5格
        
        if (args.length > 1) {
            String arg = args[1];
            if (arg.startsWith("r=")) {
                try {
                    radius = Integer.parseInt(arg.substring(2));
                    if (radius < 1 || radius > 50) {
                        player.sendMessage(ChatColor.RED + "半径必须在 1-50 之间！");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "无效的半径格式！使用 r=数字 格式，例如 r=10");
                    return true;
                }
            } else {
                try {
                    radius = Integer.parseInt(arg);
                    if (radius < 1 || radius > 50) {
                        player.sendMessage(ChatColor.RED + "半径必须在 1-50 之间！");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "无效的半径！使用 /land buy r=10");
                    return true;
                }
            }
        }

        // 检查是否已在领地内
        if (landManager.isInAnyLand(player.getWorld(), player.getLocation().getBlockX(), 
                player.getLocation().getBlockZ())) {
            player.sendMessage(ChatColor.RED + "你已经在其他领地内，无法购买！");
            return true;
        }

        String error = landManager.claimLand(player, radius);
        if (error != null) {
            player.sendMessage(ChatColor.RED + error);
        } else {
            player.sendMessage(ChatColor.GREEN + "恭喜！领地购买成功！");
            player.sendMessage(ChatColor.YELLOW + "使用 /land info 查看领地信息");
            player.sendMessage(ChatColor.YELLOW + "使用 /land add <玩家> [类型] 添加成员");
        }
        
        return true;
    }

    /**
     * 出售领地
     * /land sell
     */
    private boolean handleSell(Player player, String[] args, LandManager landManager) {
        Land land = landManager.getLandAt(player.getLocation());
        
        if (land == null) {
            player.sendMessage(ChatColor.RED + "当前位置不在任何领地内！");
            return true;
        }
        
        if (!land.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你不是这个领地的主人！");
            return true;
        }
        
        String result = landManager.sellLand(land.getId(), player);
        if (result.startsWith("领地已出售")) {
            player.sendMessage(ChatColor.GREEN + result);
        } else {
            player.sendMessage(ChatColor.RED + result);
        }
        
        return true;
    }

    /**
     * 添加成员
     * /land add <玩家> [类型]
     */
    private boolean handleAdd(Player player, String[] args, LandManager landManager) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /land add <玩家> [类型]");
            player.sendMessage(ChatColor.YELLOW + "类型: owner, member, guest (默认为 member)");
            return true;
        }
        
        String targetName = args[1];
        String memberType = args.length > 2 ? args[2] : "member";
        
        Land land = landManager.getLandAt(player.getLocation());
        if (land == null) {
            player.sendMessage(ChatColor.RED + "当前位置不在任何领地内！");
            return true;
        }
        
        String result = landManager.addMember(land.getId(), player, targetName, memberType);
        if (result.startsWith("已添加")) {
            player.sendMessage(ChatColor.GREEN + result);
        } else {
            player.sendMessage(ChatColor.RED + result);
        }
        
        return true;
    }

    /**
     * 移除成员
     * /land remove <玩家>
     */
    private boolean handleRemove(Player player, String[] args, LandManager landManager) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /land remove <玩家>");
            return true;
        }
        
        String targetName = args[1];
        
        Land land = landManager.getLandAt(player.getLocation());
        if (land == null) {
            player.sendMessage(ChatColor.RED + "当前位置不在任何领地内！");
            return true;
        }
        
        String result = landManager.removeMember(land.getId(), player, targetName);
        if (result.startsWith("已移除")) {
            player.sendMessage(ChatColor.GREEN + result);
        } else {
            player.sendMessage(ChatColor.RED + result);
        }
        
        return true;
    }

    /**
     * 查看领地列表
     * /land list
     */
    private boolean handleList(Player player, LandManager landManager) {
        player.sendMessage(ChatColor.GOLD + "========== 我的领地 ==========");
        
        // 拥有的领地
        Set<Land> ownedLands = landManager.getPlayerLands(player.getUniqueId());
        if (ownedLands.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "你还没有购买任何领地。");
        } else {
            player.sendMessage(ChatColor.GREEN + "【拥有的领地】 (" + ownedLands.size() + "个)");
            for (Land land : ownedLands) {
                player.sendMessage(ChatColor.YELLOW + "- " + land.getName() + 
                        ChatColor.GRAY + " | 面积: " + land.getArea() + 
                        ChatColor.GRAY + " | 成员: " + land.getMembers().size());
            }
        }
        
        // 作为成员的领地
        Set<Land> memberLands = landManager.getPlayerMemberLands(player.getUniqueId());
        if (!memberLands.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "【作为成员的领地】 (" + memberLands.size() + "个)");
            for (Land land : memberLands) {
                player.sendMessage(ChatColor.YELLOW + "- " + land.getName() + 
                        ChatColor.GRAY + " | 领主: " + plugin.getServer().getOfflinePlayer(land.getOwnerId()).getName());
            }
        }
        
        return true;
    }

    /**
     * 查看领地信息
     * /land info
     */
    private boolean handleInfo(Player player, LandManager landManager) {
        Land land = landManager.getLandAt(player.getLocation());
        
        if (land == null) {
            player.sendMessage(ChatColor.RED + "当前位置不在任何领地内！");
            return true;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        player.sendMessage(ChatColor.GOLD + "========== 领地信息 ==========");
        player.sendMessage(ChatColor.YELLOW + "名称: " + ChatColor.WHITE + land.getName());
        player.sendMessage(ChatColor.YELLOW + "地主: " + ChatColor.WHITE + 
                plugin.getServer().getOfflinePlayer(land.getOwnerId()).getName());
        player.sendMessage(ChatColor.YELLOW + "面积: " + ChatColor.WHITE + land.getArea() + " 格");
        player.sendMessage(ChatColor.YELLOW + "坐标: " + ChatColor.WHITE + 
                "(" + land.getMinX() + "," + land.getMinZ() + ") 到 (" + 
                land.getMaxX() + "," + land.getMaxZ() + ")");
        player.sendMessage(ChatColor.YELLOW + "购买价格: " + ChatColor.GOLD + String.format("%.2f", land.getBuyPrice()) + " 金币");
        player.sendMessage(ChatColor.YELLOW + "创建时间: " + ChatColor.GRAY + sdf.format(new Date(land.getCreatedTime())));
        
        // 成员信息
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "--- 成员列表 ---");
        for (java.util.Map.Entry<java.util.UUID, Land.MemberType> entry : land.getMembers().entrySet()) {
            String memberName = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
            player.sendMessage(ChatColor.WHITE + "- " + memberName + 
                    ChatColor.GRAY + " [" + entry.getValue().getName() + "]");
        }
        
        // 默认权限
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "--- 默认权限 ---");
        if (land.getDefaultPermissions().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "无");
        } else {
            StringBuilder perms = new StringBuilder();
            for (Land.Permission p : land.getDefaultPermissions()) {
                if (perms.length() > 0) perms.append(", ");
                perms.append(p.getDescription());
            }
            player.sendMessage(ChatColor.WHITE + perms.toString());
        }
        
        return true;
    }

    /**
     * 设置权限
     * /land perm <玩家> <权限> <true|false>
     */
    private boolean handlePermission(Player player, String[] args, LandManager landManager) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "用法: /land perm <玩家> <权限> <true|false>");
            player.sendMessage(ChatColor.YELLOW + "权限: build, destroy, interact, chest, pvp, animals, itemdrop, itempickup");
            return true;
        }
        
        String targetName = args[1];
        String permStr = args[2];
        String valueStr = args[3];
        
        boolean enable;
        if (valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("1") || 
            valueStr.equalsIgnoreCase("enable") || valueStr.equalsIgnoreCase("on")) {
            enable = true;
        } else if (valueStr.equalsIgnoreCase("false") || valueStr.equalsIgnoreCase("0") || 
                   valueStr.equalsIgnoreCase("disable") || valueStr.equalsIgnoreCase("off")) {
            enable = false;
        } else {
            player.sendMessage(ChatColor.RED + "无效的值！请使用 true 或 false");
            return true;
        }
        
        Land land = landManager.getLandAt(player.getLocation());
        if (land == null) {
            player.sendMessage(ChatColor.RED + "当前位置不在任何领地内！");
            return true;
        }
        
        String result = landManager.setMemberPermission(land.getId(), player, targetName, permStr, enable);
        if (result.startsWith("已启用") || result.startsWith("已禁用")) {
            player.sendMessage(ChatColor.GREEN + result);
        } else {
            player.sendMessage(ChatColor.RED + result);
        }
        
        return true;
    }

    /**
     * 设置领地名称
     * /land set <名称>
     */
    private boolean handleSet(Player player, String[] args, LandManager landManager) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /land set <名称>");
            return true;
        }
        
        Land land = landManager.getLandAt(player.getLocation());
        if (land == null) {
            player.sendMessage(ChatColor.RED + "当前位置不在任何领地内！");
            return true;
        }
        
        if (!land.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你不是这个领地的主人！");
            return true;
        }
        
        String newName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        land.setName(newName);
        plugin.getDatabaseManager().saveLand(land);
        
        player.sendMessage(ChatColor.GREEN + "领地名称已设置为: " + newName);
        
        return true;
    }

    /**
     * 传送到领地中心
     * /land tp <领地名称>
     */
    private boolean handleTeleport(Player player, String[] args, LandManager landManager) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /land tp <领地名称>");
            return true;
        }
        
        String landName = args[1];
        
        // 先查找玩家拥有的领地
        for (Land land : landManager.getPlayerLands(player.getUniqueId())) {
            if (land.getName().equalsIgnoreCase(landName)) {
                teleportToLand(player, land);
                return true;
            }
        }
        
        // 再查找玩家作为成员的领地
        for (Land land : landManager.getPlayerMemberLands(player.getUniqueId())) {
            if (land.getName().equalsIgnoreCase(landName)) {
                teleportToLand(player, land);
                return true;
            }
        }
        
        player.sendMessage(ChatColor.RED + "未找到领地: " + landName);
        return true;
    }
    
    /**
     * 传送到指定领地
     */
    private void teleportToLand(Player player, Land land) {
        int centerX = (land.getMinX() + land.getMaxX()) / 2;
        int centerZ = (land.getMinZ() + land.getMaxZ()) / 2;
        player.teleport(new org.bukkit.Location(land.getWorld(), centerX + 0.5, 
                land.getWorld().getHighestBlockYAt(centerX, centerZ) + 1, centerZ + 0.5));
        player.sendMessage(ChatColor.GREEN + "已传送到领地: " + land.getName());
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== 领地系统帮助 ==========");
        player.sendMessage(ChatColor.YELLOW + "/land buy [r=半径] " + ChatColor.GRAY + "- 购买领地（默认半径5格）");
        player.sendMessage(ChatColor.YELLOW + "/land sell " + ChatColor.GRAY + "- 出售当前所在领地");
        player.sendMessage(ChatColor.YELLOW + "/land list " + ChatColor.GRAY + "- 查看拥有的领地");
        player.sendMessage(ChatColor.YELLOW + "/land info " + ChatColor.GRAY + "- 查看当前领地信息");
        player.sendMessage(ChatColor.YELLOW + "/land set <名称> " + ChatColor.GRAY + "- 设置领地名称");
        player.sendMessage(ChatColor.YELLOW + "/land add <玩家> [类型] " + ChatColor.GRAY + "- 添加成员");
        player.sendMessage(ChatColor.YELLOW + "/land remove <玩家> " + ChatColor.GRAY + "- 移除成员");
        player.sendMessage(ChatColor.YELLOW + "/land perm <玩家> <权限> <true/false> " + ChatColor.GRAY + "- 设置成员权限");
        player.sendMessage(ChatColor.YELLOW + "/land tp <名称> " + ChatColor.GRAY + "- 传送到领地");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "--- 成员类型 ---");
        player.sendMessage(ChatColor.GRAY + "owner: 地主 - 拥有所有权限");
        player.sendMessage(ChatColor.GRAY + "member: 成员 - 默认有基础权限");
        player.sendMessage(ChatColor.GRAY + "guest: 访客 - 默认无特殊权限");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "--- 权限列表 ---");
        player.sendMessage(ChatColor.GRAY + "build: 建设 | destroy: 破坏 | interact: 交互");
        player.sendMessage(ChatColor.GRAY + "chest: 箱子 | pvp: PVP | animals: 动物交互");
        player.sendMessage(ChatColor.GRAY + "itemdrop: 物品掉落 | itempickup: 物品拾取");
    }
}

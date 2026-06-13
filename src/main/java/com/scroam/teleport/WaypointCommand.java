package com.scroam.teleport;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 地标命令处理
 */
public class WaypointCommand implements CommandExecutor, TabCompleter {
    private final TeleportPlugin plugin;
    private final WaypointManager waypointManager;

    public WaypointCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        this.waypointManager = new WaypointManager(plugin);
        plugin.getCommand("warp").setExecutor(this);
        plugin.getCommand("warp").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, 
                            String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
            case "add":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以使用此命令！");
                    return true;
                }
                handleCreate((Player) sender, args);
                break;
            case "delete":
            case "del":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以使用此命令！");
                    return true;
                }
                handleDelete((Player) sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "setcreatecount":
                handleSetCreateCount(sender, args);
                break;
            case "settpcount":
                handleSetTeleportCount(sender, args);
                break;
            case "setcreateprice":
                handleSetCreatePrice(sender, args);
                break;
            case "settpprice":
                handleSetTeleportPrice(sender, args);
                break;
            case "setpermission":
                handleSetPermission(sender, args);
                break;
            case "permit":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以使用此命令！");
                    return true;
                }
                handleRequestPermission((Player) sender, args);
                break;
            case "approve":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以使用此命令！");
                    return true;
                }
                handleApprove((Player) sender, args);
                break;
            case "deny":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以使用此命令！");
                    return true;
                }
                handleDeny((Player) sender, args);
                break;
            case "help":
                sendHelp(sender);
                break;
            default:
                // 默认尝试传送
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String waypointId = generateWaypointId(args[0], player.getWorld().getName());
                    if (!waypointManager.teleportToWaypoint(player, waypointId)) {
                        sender.sendMessage("§c未知指令，使用 §e/warp help §c查看帮助");
                    }
                } else {
                    sender.sendMessage("§c未知指令，使用 /warp help 查看帮助");
                }
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("teleport.warp.create")) {
            player.sendMessage("§c你没有权限创建地标！");
            return;
        }

        String name;
        if (args.length > 1) {
            name = args[1];
        } else {
            player.sendMessage("§c请输入地标名称：§e/warp create <名称>");
            return;
        }

        waypointManager.createWaypoint(player, name, player.getLocation());
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("teleport.warp.delete")) {
            player.sendMessage("§c你没有权限删除地标！");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c请输入地标名称：§e/warp delete <名称>");
            return;
        }

        String waypointId = generateWaypointId(args[1], player.getWorld().getName());
        waypointManager.deleteWaypoint(player, waypointId);
    }

    private void handleList(CommandSender sender) {
        List<Waypoint> waypoints;
        
        if (sender instanceof Player) {
            waypoints = waypointManager.getAvailableWaypoints(((Player) sender).getUniqueId());
        } else {
            waypoints = waypointManager.getAllWaypoints();
        }

        if (waypoints.isEmpty()) {
            sender.sendMessage("§7目前没有任何地标！");
            return;
        }

        sender.sendMessage("§6=== 地标列表 ===");
        for (Waypoint wp : waypoints) {
            String worldFlag = "";
            if (sender instanceof Player) {
                String currentWorld = ((Player) sender).getWorld().getName();
                if (!wp.getWorldName().equals(currentWorld)) {
                    worldFlag = " §7[" + wp.getWorldName() + "]";
                }
            }

            String priceInfo;
            if (wp.getTeleportCount() > 0) {
                priceInfo = " §a免费" + wp.getTeleportCount() + "次";
            } else if (wp.getTeleportPrice() > 0) {
                priceInfo = " §e" + wp.getTeleportPrice() + "金币";
            } else {
                priceInfo = " §a免费";
            }

            String permFlag = wp.isRequiresPermission() ? " §6🔒" : "";

            sender.sendMessage(" §e" + wp.getName() + worldFlag + permFlag + priceInfo + 
                             " §7- 传送到: " + (int)wp.getX() + ", " + (int)wp.getY() + ", " + (int)wp.getZ());
        }
        sender.sendMessage("§7使用 §e/warp <名称> §7传送");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.warp.admin")) {
            sender.sendMessage("§c你没有权限查看地标信息！");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c请输入地标名称：§e/warp info <名称>");
            return;
        }

        Waypoint wp;
        if (sender instanceof Player) {
            String waypointId = generateWaypointId(args[1], ((Player) sender).getWorld().getName());
            wp = waypointManager.getWaypointById(waypointId);
            if (wp == null) {
                // 尝试在所有世界中查找
                wp = waypointManager.getWaypointByName(args[1], "");
            }
        } else {
            wp = waypointManager.getWaypointByName(args[1], "");
        }

        if (wp == null) {
            sender.sendMessage("§c地标不存在！");
            return;
        }

        sender.sendMessage("§6=== 地标信息 ===");
        sender.sendMessage(" §e名称: §f" + wp.getName());
        sender.sendMessage(" §eID: §f" + wp.getId());
        sender.sendMessage(" §e世界: §f" + wp.getWorldName());
        sender.sendMessage(" §e坐标: §f" + (int)wp.getX() + ", " + (int)wp.getY() + ", " + (int)wp.getZ());
        sender.sendMessage(" §e创建者: §f" + wp.getCreator());
        sender.sendMessage(" §e创建免费次数: §f" + wp.getCreateCount());
        sender.sendMessage(" §e传送免费次数: §f" + wp.getTeleportCount());
        sender.sendMessage(" §e创建费用: §f" + wp.getCreatePrice() + " 金币");
        sender.sendMessage(" §e传送费用: §f" + wp.getTeleportPrice() + " 金币");
        sender.sendMessage(" §e需要权限: §f" + (wp.isRequiresPermission() ? "§a是" : "§c否"));
    }

    private void handleSetCreateCount(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.warp.admin")) {
            sender.sendMessage("§c你没有权限设置地标！");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§c用法：§e/warp setcreatecount <名称> <次数>");
            return;
        }

        int count;
        try {
            count = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c次数必须是数字！");
            return;
        }

        String waypointId = generateWaypointId(args[1], sender instanceof Player ? 
                                               ((Player)sender).getWorld().getName() : "");
        waypointManager.setWaypointCreateCount(sender, waypointId, count);
    }

    private void handleSetTeleportCount(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.warp.admin")) {
            sender.sendMessage("§c你没有权限设置地标！");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§c用法：§e/warp settpcount <名称> <次数>");
            return;
        }

        int count;
        try {
            count = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c次数必须是数字！");
            return;
        }

        String waypointId = generateWaypointId(args[1], sender instanceof Player ? 
                                               ((Player)sender).getWorld().getName() : "");
        waypointManager.setWaypointTeleportCount(sender, waypointId, count);
    }

    private void handleSetCreatePrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.warp.admin")) {
            sender.sendMessage("§c你没有权限设置地标！");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§c用法：§e/warp setcreateprice <名称> <价格>");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c价格必须是数字！");
            return;
        }

        String waypointId = generateWaypointId(args[1], sender instanceof Player ? 
                                               ((Player)sender).getWorld().getName() : "");
        waypointManager.setWaypointCreatePrice(sender, waypointId, price);
    }

    private void handleSetTeleportPrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.warp.admin")) {
            sender.sendMessage("§c你没有权限设置地标！");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§c用法：§e/warp settpprice <名称> <价格>");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c价格必须是数字！");
            return;
        }

        String waypointId = generateWaypointId(args[1], sender instanceof Player ? 
                                               ((Player)sender).getWorld().getName() : "");
        waypointManager.setWaypointTeleportPrice(sender, waypointId, price);
    }

    private void handleSetPermission(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleport.warp.admin")) {
            sender.sendMessage("§c你没有权限设置地标！");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§c用法：§e/warp setpermission <名称> <true|false>");
            return;
        }

        boolean required = args[2].equalsIgnoreCase("true");
        String waypointId = generateWaypointId(args[1], sender instanceof Player ? 
                                               ((Player)sender).getWorld().getName() : "");
        waypointManager.setWaypointPermission(sender, waypointId, required);
    }

    private void handleRequestPermission(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c请输入地标名称：§e/warp permit <名称>");
            return;
        }

        String waypointId = generateWaypointId(args[1], player.getWorld().getName());
        waypointManager.requestPermission(player, waypointId);
    }

    private void handleApprove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c请输入地标名称：§e/warp approve <名称>");
            return;
        }

        String waypointId = generateWaypointId(args[1], player.getWorld().getName());
        waypointManager.approvePermission(player, waypointId);
    }

    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c请输入地标名称：§e/warp deny <名称>");
            return;
        }

        String waypointId = generateWaypointId(args[1], player.getWorld().getName());
        waypointManager.denyPermission(player, waypointId);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6========== 地标系统帮助 ==========");
        sender.sendMessage(" §e/warp <名称> §7- 传送到地标");
        sender.sendMessage(" §e/warp create <名称> §7- 创建地标");
        sender.sendMessage(" §e/warp delete <名称> §7- 删除地标");
        sender.sendMessage(" §e/warp list §7- 查看所有地标");
        sender.sendMessage(" §e/warp info <名称> §7- 查看地标详情");
        
        if (sender.hasPermission("teleport.warp.admin")) {
            sender.sendMessage("§6========== 管理命令 ==========");
            sender.sendMessage(" §e/warp setcreatecount <名称> <次数> §7- 设置创建免费次数");
            sender.sendMessage(" §e/warp settpcount <名称> <次数> §7- 设置传送免费次数");
            sender.sendMessage(" §e/warp setcreateprice <名称> <价格> §7- 设置创建费用");
            sender.sendMessage(" §e/warp settpprice <名称> <价格> §7- 设置传送费用");
            sender.sendMessage(" §e/warp setpermission <名称> <true|false> §7- 设置权限要求");
        }
        
        if (sender.hasPermission("teleport.warp.perm")) {
            sender.sendMessage("§6========== 权限命令 ==========");
            sender.sendMessage(" §e/warp permit <名称> §7- 申请使用地标");
            sender.sendMessage(" §e/warp approve <名称> §7- 批准申请");
            sender.sendMessage(" §e/warp deny <名称> §7- 拒绝申请");
        }
    }

    private String generateWaypointId(String name, String worldName) {
        return (worldName.isEmpty() ? "" : worldName.toLowerCase() + "_") + name.toLowerCase();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
                                       String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList(
                    "create", "delete", "list", "info", "help"
            ));
            if (sender.hasPermission("teleport.warp.admin")) {
                subCommands.addAll(Arrays.asList(
                        "setcreatecount", "settpcount", "setcreateprice", "settpprice", "setpermission"
                ));
            }
            if (sender.hasPermission("teleport.warp.perm")) {
                subCommands.addAll(Arrays.asList("permit", "approve", "deny"));
            }
            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            List<Waypoint> waypoints;
            if (subCommand.equals("info") && sender.hasPermission("teleport.warp.admin")) {
                waypoints = waypointManager.getAllWaypoints();
            } else if (subCommand.equals("approve") || subCommand.equals("deny")) {
                waypoints = waypointManager.getPlayerWaypoints(((Player) sender).getUniqueId());
            } else {
                waypoints = waypointManager.getAvailableWaypoints(((Player) sender).getUniqueId());
            }
            
            completions = waypoints.stream()
                    .filter(wp -> wp.getName().toLowerCase().startsWith(input))
                    .map(Waypoint::getName)
                    .collect(Collectors.toList());
        } else if (args.length == 3 && sender.hasPermission("teleport.warp.admin")) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("setcreatecount") || subCommand.equals("settpcount")) {
                completions = Arrays.asList("0", "1", "2", "3", "5", "10", "999");
            } else if (subCommand.equals("setcreateprice") || subCommand.equals("settpprice")) {
                completions = Arrays.asList("0", "10", "50", "100", "500", "1000");
            } else if (subCommand.equals("setpermission")) {
                completions = Arrays.asList("true", "false");
            }
        }

        return completions;
    }
}

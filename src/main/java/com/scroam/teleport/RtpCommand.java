package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class RtpCommand implements CommandExecutor {

    private final TeleportPlugin plugin;
    private final Random random = new Random();

    public RtpCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("rtp").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("teleport.rtp")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        DatabaseManager db = plugin.getDatabaseManager();
        boolean useEconomy = plugin.getConfigManager().isEconomyEnabled() && db != null;

        if (useEconomy) {
            db.createUser(player.getUniqueId(), player.getName());
            
            int freeCount = db.getRtpFreeCount(player.getUniqueId());
            double balance = db.getBalance(player.getUniqueId());
            double price = db.getUser(player.getUniqueId()).rtpPrice;

            if (freeCount > 0) {
                db.decrementRtpFreeCount(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "使用免费RTP次数! 剩余次数: " + (freeCount - 1));
            } else {
                if (balance < price) {
                    player.sendMessage(ChatColor.RED + "余额不足! 需要 " + String.format("%.2f", price) + " 金币");
                    player.sendMessage(ChatColor.YELLOW + "你的余额: " + String.format("%.2f", balance));
                    return true;
                }
                
                db.withdrawBalance(player.getUniqueId(), price);
                player.sendMessage(ChatColor.GREEN + "已扣除 " + String.format("%.2f", price) + " 金币");
            }
        }

        World world = player.getWorld();
        if (world == null) {
            player.sendMessage(ChatColor.RED + "无效的世界!");
            return true;
        }

        int radius = plugin.getConfigManager().getRtpDefaultRadius();
        if (args.length > 0) {
            try {
                radius = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "无效的半径!");
                return true;
            }
        }

        int minRadius = plugin.getConfigManager().getRtpMinRadius();
        int maxRadius = plugin.getConfigManager().getRtpMaxRadius();

        if (radius < minRadius) {
            player.sendMessage(ChatColor.RED + "半径不能小于 " + minRadius + "!");
            return true;
        }

        if (radius > maxRadius) {
            player.sendMessage(ChatColor.RED + "半径不能大于 " + maxRadius + "!");
            return true;
        }

        if (radius < 10) {
            player.sendMessage(ChatColor.RED + "半径太小!");
            return true;
        }

        Location location = findRandomLocation(world, radius);
        if (location != null) {
            player.teleport(location);
            player.sendMessage(ChatColor.GREEN + "已随机传送!");
        } else {
            player.sendMessage(ChatColor.RED + "无法找到安全的传送位置!");
        }

        return true;
    }

    private Location findRandomLocation(World world, int radius) {
        if (world == null) {
            return null;
        }

        int attempts = 0;
        int maxAttempts = 100;

        while (attempts < maxAttempts) {
            double x = random.nextDouble() * radius * 2 - radius;
            double z = random.nextDouble() * radius * 2 - radius;
            int y = findSafeY(world, (int) x, (int) z);

            if (y > 0 && y < world.getMaxHeight() - 1) {
                Location loc = new Location(world, x, y, z);
                if (isSafeLocation(loc)) {
                    loc.getChunk().load(true);
                    return loc;
                }
            }
            attempts++;
        }

        return null;
    }

    private int findSafeY(World world, int x, int z) {
        if (world == null) {
            return -1;
        }

        try {
            int highestY = world.getHighestBlockYAt(x, z);
            
            if (highestY <= 0) {
                return -1;
            }

            for (int y = highestY; y > 0; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType().isSolid()) {
                    return y + 1;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error finding safe Y: " + e.getMessage());
        }

        return -1;
    }

    private boolean isSafeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }

        try {
            Block ground = loc.getBlock().getRelative(0, -1, 0);
            Block feet = loc.getBlock();
            Block head = loc.getBlock().getRelative(0, 1, 0);
            Block aboveHead = loc.getBlock().getRelative(0, 2, 0);

            if (ground == null || feet == null || head == null) {
                return false;
            }

            Material groundType = ground.getType();
            Material feetType = feet.getType();
            Material headType = head.getType();

            if (!groundType.isSolid()) {
                return false;
            }

            if (groundType.name().contains("LAVA") || groundType.name().contains("WATER")) {
                return false;
            }

            if (feetType != Material.AIR) {
                return false;
            }

            if (headType != Material.AIR) {
                return false;
            }

            if (aboveHead != null && aboveHead.getType() != Material.AIR) {
                return false;
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking safe location: " + e.getMessage());
            return false;
        }
    }
}

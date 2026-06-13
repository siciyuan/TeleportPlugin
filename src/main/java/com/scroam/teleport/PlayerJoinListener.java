package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoinListener implements Listener {

    private final TeleportPlugin plugin;

    public PlayerJoinListener(TeleportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 新玩家设置默认家
        if (!event.getPlayer().hasPlayedBefore()) {
            plugin.getHomeManager().setHome(event.getPlayer().getUniqueId(), "home", event.getPlayer().getLocation());
            event.getPlayer().sendMessage(ChatColor.GREEN + "欢迎来到服务器！你的默认家已设置在当前位置。");
        }
        
        // 发送公告给玩家
        sendJoinAnnouncement(event.getPlayer());
    }
    
    /**
     * 发送加入服务器时的公告
     */
    private void sendJoinAnnouncement(org.bukkit.entity.Player player) {
        List<String> announcements = plugin.getConfig().getStringList("announcements.list");
        
        // 设置MOTD
        String motd = plugin.getConfig().getString("announcements.motd", "欢迎来到服务器");
        player.setPlayerListHeaderFooter(ChatColor.GOLD + motd + "\n", "");
        
        if (announcements.isEmpty()) {
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "========== 服务器公告 ==========");
        for (int i = 0; i < Math.min(5, announcements.size()); i++) {
            player.sendMessage(ChatColor.WHITE + String.valueOf(i + 1) + ". " + announcements.get(i));
        }
        
        if (announcements.size() > 5) {
            player.sendMessage(ChatColor.YELLOW + "还有 " + ChatColor.WHITE + (announcements.size() - 5) + ChatColor.YELLOW + " 条公告...");
        }
        player.sendMessage(ChatColor.GOLD + "================================");
    }
}
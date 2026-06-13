package com.scroam.teleport;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ExchangeCommand implements CommandExecutor {

    private final TeleportPlugin plugin;
    
    // 物品名称映射
    private static final Map<String, Material> MATERIAL_MAP = new HashMap<>();
    static {
        // 金相关
        MATERIAL_MAP.put("gold_ingot", Material.GOLD_INGOT);
        MATERIAL_MAP.put("gold_nugget", Material.GOLD_NUGGET);
        MATERIAL_MAP.put("gold_block", Material.GOLD_BLOCK);
        MATERIAL_MAP.put("raw_gold", Material.RAW_GOLD);
        
        // 铜相关
        MATERIAL_MAP.put("copper_ingot", Material.COPPER_INGOT);
        MATERIAL_MAP.put("copper_nugget", Material.COPPER_NUGGET);
        MATERIAL_MAP.put("copper_block", Material.COPPER_BLOCK);
        MATERIAL_MAP.put("raw_copper", Material.RAW_COPPER);
        
        // 铁相关
        MATERIAL_MAP.put("iron_ingot", Material.IRON_INGOT);
        MATERIAL_MAP.put("iron_nugget", Material.IRON_NUGGET);
        MATERIAL_MAP.put("iron_block", Material.IRON_BLOCK);
        MATERIAL_MAP.put("raw_iron", Material.RAW_IRON);
        
        // 钻石相关
        MATERIAL_MAP.put("diamond", Material.DIAMOND);
        MATERIAL_MAP.put("diamond_block", Material.DIAMOND_BLOCK);
        
        // 下界合金
        MATERIAL_MAP.put("netherite_ingot", Material.NETHERITE_INGOT);
        MATERIAL_MAP.put("netherite_block", Material.NETHERITE_BLOCK);
        
        // 绿宝石
        MATERIAL_MAP.put("emerald", Material.EMERALD);
        MATERIAL_MAP.put("emerald_block", Material.EMERALD_BLOCK);
        
        // 煤炭相关
        MATERIAL_MAP.put("coal", Material.COAL);
        MATERIAL_MAP.put("charcoal", Material.CHARCOAL);
        MATERIAL_MAP.put("coal_block", Material.COAL_BLOCK);
        
        // 青金石
        MATERIAL_MAP.put("lapis_lazuli", Material.LAPIS_LAZULI);
    }

    public ExchangeCommand(TeleportPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("deposit").setExecutor(this);
        plugin.getCommand("withdraw").setExecutor(this);
        plugin.getCommand("exchange").setExecutor(this);
        plugin.getCommand("exchangeinfo").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;
        DatabaseManager db = plugin.getDatabaseManager();

        if (db == null) {
            player.sendMessage(ChatColor.RED + "数据库未启用!");
            return true;
        }

        if (!plugin.getConfigManager().isExchangeEnabled()) {
            player.sendMessage(ChatColor.RED + "兑换系统未启用!");
            return true;
        }

        String commandName = cmd.getName().toLowerCase();

        switch (commandName) {
            case "deposit":
                return handleDeposit(player, args, db);
            case "withdraw":
                return handleWithdraw(player, args, db);
            case "exchange":
                return handleExchangeInfo(player);
            case "exchangeinfo":
                return handleExchangeInfo(player);
            default:
                return false;
        }
    }

    /**
     * 存入矿物兑换货币
     */
    private boolean handleDeposit(Player player, String[] args, DatabaseManager db) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /deposit <物品类型> <数量>");
            player.sendMessage(ChatColor.YELLOW + "物品类型: gold_ingot, gold_nugget, iron_ingot, diamond 等");
            player.sendMessage(ChatColor.YELLOW + "使用 /exchangeinfo 查看所有可兑换物品");
            return true;
        }

        String itemType = args[0].toLowerCase();
        Material material = MATERIAL_MAP.get(itemType);
        
        if (material == null) {
            player.sendMessage(ChatColor.RED + "未知的物品类型: " + itemType);
            player.sendMessage(ChatColor.YELLOW + "使用 /exchangeinfo 查看所有可兑换物品");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的数量!");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "数量必须大于0!");
            return true;
        }

        // 检查玩家是否有足够的物品
        int playerAmount = countItems(player, material);
        if (playerAmount < amount) {
            player.sendMessage(ChatColor.RED + "你没有足够的 " + getItemDisplayName(itemType) + "!");
            player.sendMessage(ChatColor.YELLOW + "当前持有: " + playerAmount + ", 需要: " + amount);
            return true;
        }

        // 计算兑换金额
        double rate = getExchangeRate(itemType);
        double totalMoney = amount * rate;

        // 移除物品
        removeItems(player, material, amount);

        // 增加余额
        db.depositBalance(player.getUniqueId(), totalMoney);

        player.sendMessage(ChatColor.GREEN + "成功存入 " + amount + " 个 " + getItemDisplayName(itemType));
        player.sendMessage(ChatColor.GREEN + "获得 " + ChatColor.GOLD + String.format("%.2f", totalMoney) + " 金币");
        player.sendMessage(ChatColor.YELLOW + "当前余额: " + ChatColor.GOLD + String.format("%.2f", db.getBalance(player.getUniqueId())));
        
        return true;
    }

    /**
     * 提取货币兑换矿物（带税）
     */
    private boolean handleWithdraw(Player player, String[] args, DatabaseManager db) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /withdraw <物品类型> <数量>");
            player.sendMessage(ChatColor.YELLOW + "物品类型: gold_ingot, gold_nugget, iron_ingot, diamond 等");
            player.sendMessage(ChatColor.YELLOW + "使用 /exchangeinfo 查看所有可兑换物品");
            return true;
        }

        String itemType = args[0].toLowerCase();
        Material material = MATERIAL_MAP.get(itemType);
        
        if (material == null) {
            player.sendMessage(ChatColor.RED + "未知的物品类型: " + itemType);
            player.sendMessage(ChatColor.YELLOW + "使用 /exchangeinfo 查看所有可兑换物品");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的数量!");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "数量必须大于0!");
            return true;
        }

        // 计算需要的金额（含税）
        double rate = getExchangeRate(itemType);
        double baseCost = amount * rate;
        double taxRate = plugin.getConfigManager().getWithdrawTax();
        double tax = baseCost * taxRate;
        double totalCost = baseCost + tax;

        // 检查余额
        double balance = db.getBalance(player.getUniqueId());
        if (balance < totalCost) {
            player.sendMessage(ChatColor.RED + "余额不足!");
            player.sendMessage(ChatColor.YELLOW + "需要: " + String.format("%.2f", totalCost) + " 金币 (含税 " + (taxRate * 100) + "%)");
            player.sendMessage(ChatColor.YELLOW + "当前余额: " + String.format("%.2f", balance));
            return true;
        }

        // 检查背包空间
        if (player.getInventory().firstEmpty() == -1 && !hasSpaceFor(player, material, amount)) {
            player.sendMessage(ChatColor.RED + "背包空间不足!");
            return true;
        }

        // 扣除余额
        db.withdrawBalance(player.getUniqueId(), totalCost);

        // 给予物品
        giveItems(player, material, amount);

        player.sendMessage(ChatColor.GREEN + "成功提取 " + amount + " 个 " + getItemDisplayName(itemType));
        player.sendMessage(ChatColor.YELLOW + "花费: " + ChatColor.RED + String.format("%.2f", totalCost) + " 金币");
        if (tax > 0) {
            player.sendMessage(ChatColor.YELLOW + "手续费: " + ChatColor.RED + String.format("%.2f", tax) + " (税率 " + (taxRate * 100) + "%)");
        }
        player.sendMessage(ChatColor.YELLOW + "当前余额: " + ChatColor.GOLD + String.format("%.2f", db.getBalance(player.getUniqueId())));
        
        return true;
    }

    /**
     * 显示兑换信息
     */
    private boolean handleExchangeInfo(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== 矿物兑换系统 ===");
        player.sendMessage(ChatColor.YELLOW + "存入矿物: /deposit <物品> <数量>");
        player.sendMessage(ChatColor.YELLOW + "提取矿物: /withdraw <物品> <数量>");
        player.sendMessage(ChatColor.YELLOW + "提取税率: " + ChatColor.RED + (plugin.getConfigManager().getWithdrawTax() * 100) + "%");
        player.sendMessage(ChatColor.GREEN + "--- 兑换比例 ---");
        
        // 金相关
        player.sendMessage(ChatColor.GOLD + "金锭: " + getExchangeRate("gold_ingot") + " | 金粒: " + getExchangeRate("gold_nugget") + " | 金块: " + getExchangeRate("gold_block"));
        
        // 铁相关
        player.sendMessage(ChatColor.WHITE + "铁锭: " + getExchangeRate("iron_ingot") + " | 铁粒: " + getExchangeRate("iron_nugget") + " | 铁块: " + getExchangeRate("iron_block"));
        
        // 铜相关
        player.sendMessage(ChatColor.YELLOW + "铜锭: " + getExchangeRate("copper_ingot") + " | 铜粒: " + getExchangeRate("copper_nugget") + " | 铜块: " + getExchangeRate("copper_block"));
        
        // 钻石
        player.sendMessage(ChatColor.AQUA + "钻石: " + getExchangeRate("diamond") + " | 钻石块: " + getExchangeRate("diamond_block"));
        
        // 绿宝石
        player.sendMessage(ChatColor.GREEN + "绿宝石: " + getExchangeRate("emerald") + " | 绿宝石块: " + getExchangeRate("emerald_block"));
        
        // 煤炭
        player.sendMessage(ChatColor.DARK_GRAY + "煤炭: " + getExchangeRate("coal") + " | 木炭: " + getExchangeRate("charcoal") + " | 煤炭块: " + getExchangeRate("coal_block"));
        
        // 下界合金
        player.sendMessage(ChatColor.DARK_GRAY + "下界合金锭: " + getExchangeRate("netherite_ingot") + " | 下界合金块: " + getExchangeRate("netherite_block"));
        
        // 青金石
        player.sendMessage(ChatColor.BLUE + "青金石: " + getExchangeRate("lapis_lazuli"));
        
        player.sendMessage(ChatColor.YELLOW + "当前余额: " + ChatColor.GOLD + String.format("%.2f", plugin.getDatabaseManager().getBalance(player.getUniqueId())));
        
        return true;
    }

    /**
     * 获取兑换比例
     */
    private double getExchangeRate(String itemType) {
        return plugin.getConfig().getDouble("economy.exchange.rates." + itemType, 0);
    }

    /**
     * 获取物品显示名称
     */
    private String getItemDisplayName(String itemType) {
        switch (itemType) {
            case "gold_ingot": return ChatColor.GOLD + "金锭";
            case "gold_nugget": return ChatColor.GOLD + "金粒";
            case "gold_block": return ChatColor.GOLD + "金块";
            case "raw_gold": return ChatColor.GOLD + "原金";
            case "iron_ingot": return ChatColor.WHITE + "铁锭";
            case "iron_nugget": return ChatColor.WHITE + "铁粒";
            case "iron_block": return ChatColor.WHITE + "铁块";
            case "raw_iron": return ChatColor.WHITE + "原铁";
            case "copper_ingot": return ChatColor.YELLOW + "铜锭";
            case "copper_nugget": return ChatColor.YELLOW + "铜粒";
            case "copper_block": return ChatColor.YELLOW + "铜块";
            case "raw_copper": return ChatColor.YELLOW + "原铜";
            case "diamond": return ChatColor.AQUA + "钻石";
            case "diamond_block": return ChatColor.AQUA + "钻石块";
            case "emerald": return ChatColor.GREEN + "绿宝石";
            case "emerald_block": return ChatColor.GREEN + "绿宝石块";
            case "coal": return ChatColor.GRAY + "煤炭";
            case "charcoal": return ChatColor.GRAY + "木炭";
            case "coal_block": return ChatColor.GRAY + "煤炭块";
            case "netherite_ingot": return ChatColor.DARK_GRAY + "下界合金锭";
            case "netherite_block": return ChatColor.DARK_GRAY + "下界合金块";
            case "lapis_lazuli": return ChatColor.BLUE + "青金石";
            default: return itemType;
        }
    }

    /**
     * 计算玩家背包中某物品的数量
     */
    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 从玩家背包移除指定数量的物品
     */
    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    item.setAmount(0);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
    }

    /**
     * 给予玩家物品
     */
    private void giveItems(Player player, Material material, int amount) {
        int maxStackSize = material.getMaxStackSize();
        int fullStacks = amount / maxStackSize;
        int remainder = amount % maxStackSize;

        for (int i = 0; i < fullStacks; i++) {
            player.getInventory().addItem(new ItemStack(material, maxStackSize));
        }
        if (remainder > 0) {
            player.getInventory().addItem(new ItemStack(material, remainder));
        }
    }

    /**
     * 检查玩家背包是否有足够空间
     */
    private boolean hasSpaceFor(Player player, Material material, int amount) {
        int maxStackSize = material.getMaxStackSize();
        int neededSlots = (int) Math.ceil(amount / (double) maxStackSize);
        int availableSlots = 0;

        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null) {
                availableSlots++;
            } else if (item.getType() == material && item.getAmount() < maxStackSize) {
                // 可以堆叠的空间
                availableSlots += (maxStackSize - item.getAmount()) / maxStackSize;
            }
        }

        return availableSlots >= neededSlots;
    }
}
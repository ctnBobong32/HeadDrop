package me.rrs.headdrop.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品工具类
 * 用于处理物品的Lore添加和格式化
 * 作者: RRS
 */
public class ItemUtils {

    /**
     * 为头颅物品添加Lore信息
     * 
     * @param head 头颅物品
     * @param rawLore 原始Lore列表
     * @param killer 击杀者玩家（可为null）
     */
    public void addLore(ItemStack head, List<String> rawLore, Player killer) {
        // 检查Lore列表是否为空
        if (rawLore == null || rawLore.isEmpty()) return;

        ItemMeta itemMeta = head.getItemMeta();
        // 如果物品没有元数据，则无法添加Lore
        if (itemMeta == null) return;
        
        List<String> finalLore = new ArrayList<>();

        // 处理每行Lore
        rawLore.forEach(lore -> {
            if (!lore.trim().isEmpty()) {
                // 替换占位符
                lore = lore
                        .replace("{KILLER}", killer != null ? killer.getName() : "未知")
                        .replace("{DATE}", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
                        .replace("{WEAPON}", killer != null ? getWeaponDisplayName(killer.getInventory().getItemInMainHand().getType().toString()) : "未知");
                
                // 支持PlaceholderAPI
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    lore = killer != null ? PlaceholderAPI.setPlaceholders(killer, lore) : PlaceholderAPI.setPlaceholders(null, lore);
                }
                
                // 转换颜色代码并添加到最终Lore列表
                finalLore.add(ChatColor.translateAlternateColorCodes('&', lore));
            }
        });

        // 设置Lore并更新物品
        itemMeta.setLore(finalLore);
        head.setItemMeta(itemMeta);
    }

    /**
     * 获取武器显示名称（将英文转换为中文）
     * 
     * @param weaponType 武器类型英文名称
     * @return 武器中文显示名称
     */
    private String getWeaponDisplayName(String weaponType) {
        // 常见武器类型的中文映射
        switch (weaponType.toUpperCase()) {
            case "DIAMOND_SWORD":
                return "钻石剑";
            case "IRON_SWORD":
                return "铁剑";
            case "GOLDEN_SWORD":
                return "金剑";
            case "STONE_SWORD":
                return "石剑";
            case "WOODEN_SWORD":
                return "木剑";
            case "DIAMOND_AXE":
                return "钻石斧";
            case "IRON_AXE":
                return "铁斧";
            case "GOLDEN_AXE":
                return "金斧";
            case "STONE_AXE":
                return "石斧";
            case "WOODEN_AXE":
                return "木斧";
            case "BOW":
                return "弓";
            case "CROSSBOW":
                return "弩";
            case "TRIDENT":
                return "三叉戟";
            case "SHEARS":
                return "剪刀";
            case "FISHING_ROD":
                return "钓鱼竿";
            case "AIR":
                return "空手";
            default:
                // 对于未知武器，返回格式化的英文名称
                return formatWeaponName(weaponType);
        }
    }

    /**
     * 格式化武器名称（将下划线分隔的英文转换为空格分隔的友好名称）
     * 
     * @param weaponName 武器名称
     * @return 格式化的武器名称
     */
    private String formatWeaponName(String weaponName) {
        if (weaponName == null || weaponName.isEmpty()) {
            return "未知";
        }
        
        // 将下划线替换为空格并转为小写（首字母大写）
        String[] words = weaponName.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                         .append(word.substring(1))
                         .append(" ");
            }
        }
        
        return formatted.toString().trim();
    }

    /**
     * 检查物品是否已有Lore
     * 
     * @param item 要检查的物品
     * @return 是否已有Lore
     */
    public boolean hasLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasLore() && !meta.getLore().isEmpty();
    }

    /**
     * 清空物品的Lore
     * 
     * @param item 要清空Lore的物品
     */
    public void clearLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(new ArrayList<>());
            item.setItemMeta(meta);
        }
    }
}
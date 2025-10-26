package me.rrs.headdrop.util;

import me.clip.placeholderapi.PlaceholderAPI;
import me.rrs.headdrop.HeadDrop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 语言工具类
 * 用于处理多语言消息的发送和格式化
 * 作者: RRS
 */
public class Lang {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * 发送消息给玩家（无占位符）
     *
     * @param prefix 消息前缀
     * @param path 语言文件路径
     * @param player 目标玩家
     */
    public void msg(String prefix, String path, Player player) {
        String message = HeadDrop.getInstance().getLang().getString(path);
        if (message == null) {
            message = "§c未找到消息路径: " + path;
        }
        
        // 支持PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        // 使用MiniMessage解析颜色代码
        Component component = miniMessage.deserialize(prefix + " " + message);
        player.sendMessage(component);
    }

    /**
     * 发送消息给玩家（带单个占位符）
     *
     * @param prefix 消息前缀
     * @param path 语言文件路径
     * @param placeholder 占位符
     * @param obj 替换对象
     * @param player 目标玩家
     */
    public void msg(String prefix, String path, String placeholder, String obj, Player player) {
        String message = HeadDrop.getInstance().getLang().getString(path);
        if (message == null) {
            message = "§c未找到消息路径: " + path;
        }
        
        message = message.replace(placeholder, obj);
        
        // 支持PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        // 使用MiniMessage解析颜色代码
        Component component = miniMessage.deserialize(prefix + " " + message);
        player.sendMessage(component);
    }

    /**
     * 发送消息给玩家（带多个占位符）
     *
     * @param prefix 消息前缀
     * @param path 语言文件路径
     * @param placeholders 占位符数组
     * @param replacements 替换值数组
     * @param player 目标玩家
     */
    public void msg(String prefix, String path, String[] placeholders, String[] replacements, Player player) {
        if (placeholders.length != replacements.length) {
            player.sendMessage("§c消息占位符数量不匹配！");
            return;
        }
        
        String message = HeadDrop.getInstance().getLang().getString(path);
        if (message == null) {
            message = "§c未找到消息路径: " + path;
        }
        
        // 替换所有占位符
        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace(placeholders[i], replacements[i]);
        }
        
        // 支持PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        // 使用MiniMessage解析颜色代码
        Component component = miniMessage.deserialize(prefix + " " + message);
        player.sendMessage(component);
    }

    /**
     * 发送无权限错误消息
     *
     * @param player 目标玩家
     */
    public void noPerm(Player player) {
        msg("&c&l[头颅掉落]&r", "Permission-Error", player);
    }

    /**
     * 发送控制台错误消息（玩家命令控制台执行）
     */
    public void pcmd() {
        String message = HeadDrop.getInstance().getLang().getString("Player-Command");
        if (message == null) {
            message = "§c这个命令只能由玩家执行！";
        }
        
        Component component = miniMessage.deserialize(message);
        Bukkit.getLogger().severe(component.toString());
    }

    /**
     * 发送成功消息
     *
     * @param player 目标玩家
     * @param path 语言文件路径
     */
    public void success(Player player, String path) {
        msg("&a&l[头颅掉落]&r", path, player);
    }

    /**
     * 发送警告消息
     *
     * @param player 目标玩家
     * @param path 语言文件路径
     */
    public void warning(Player player, String path) {
        msg("&6&l[头颅掉落]&r", path, player);
    }

    /**
     * 发送错误消息
     *
     * @param player 目标玩家
     * @param path 语言文件路径
     */
    public void error(Player player, String path) {
        msg("&c&l[头颅掉落]&r", path, player);
    }

    /**
     * 发送信息消息
     *
     * @param player 目标玩家
     * @param path 语言文件路径
     */
    public void info(Player player, String path) {
        msg("&b&l[头颅掉落]&r", path, player);
    }

    /**
     * 直接从语言文件获取消息（不发送）
     *
     * @param path 语言文件路径
     * @return 消息字符串
     */
    public String getMessage(String path) {
        String message = HeadDrop.getInstance().getLang().getString(path);
        if (message == null) {
            return "§c未找到消息路径: " + path;
        }
        return message;
    }

    /**
     * 直接从语言文件获取消息并替换占位符（不发送）
     *
     * @param path 语言文件路径
     * @param placeholders 占位符数组
     * @param replacements 替换值数组
     * @return 处理后的消息字符串
     */
    public String getMessage(String path, String[] placeholders, String[] replacements) {
        if (placeholders.length != replacements.length) {
            return "§c消息占位符数量不匹配！";
        }
        
        String message = HeadDrop.getInstance().getLang().getString(path);
        if (message == null) {
            return "§c未找到消息路径: " + path;
        }
        
        // 替换所有占位符
        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace(placeholders[i], replacements[i]);
        }
        
        return message;
    }
}
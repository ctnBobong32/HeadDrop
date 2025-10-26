package me.rrs.headdrop.commands;

import me.rrs.headdrop.HeadDrop;
import me.rrs.headdrop.listener.HeadGUI;
import me.rrs.headdrop.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final Lang lang = new Lang();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(miniMessage.deserialize("<gold>头颅掉落插件 by RRS</gold>"));
        } else {
            switch (args[0].toLowerCase()) {
                case "help":
                case "帮助":
                    sendHelpMessage(sender);
                    break;
                case "reload":
                case "重载":
                    reloadConfigAndLang(sender);
                    break;
                case "leaderboard":
                case "排行榜":
                    showLeaderboard(sender);
                    break;
                case "debug":
                case "调试":
                    generateDebugFile(sender);
                    break;
                case "gui":
                case "界面":
                    openGUI(sender);
                    break;
            }
        }
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        if (sender instanceof Player player) {
            Component message = miniMessage.deserialize("""
            <dark_green>头颅掉落插件</dark_green> <reset>作者: RRS
            
            <aqua>> </aqua><light_purple>/headdrop help</light_purple> <reset>-> 显示此帮助信息
            
            <aqua>> </aqua><light_purple>/headdrop reload</light_purple> <reset>-> 重载插件配置
            
            <aqua>> </aqua><light_purple>/headdrop leaderboard</light_purple> <reset>-> 显示头颅收集排行榜
            
            <aqua>> </aqua><light_purple>/headdrop gui</light_purple> <reset>-> 打开头颅界面
            
            <aqua>> </aqua><light_purple>/myhead</light_purple> <reset>-> 获取自己的头颅
            
            <aqua>> </aqua><light_purple>/head <玩家名></light_purple> <reset>-> 获取其他玩家的头颅
            """);

            player.sendMessage(message);
        }
    }

    private void reloadConfigAndLang(CommandSender sender) {
        if (sender instanceof Player player) {
            if (player.hasPermission("headdrop.reload")) {
                try {
                    HeadDrop.getInstance().getLang().reload();
                    HeadDrop.getInstance().getConfiguration().reload();
                    Component message = miniMessage.deserialize("<green>[头颅掉落]</green> <reset>配置重载成功！");
                    sender.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                lang.noPerm(player);
            }
        } else {
            try {
                HeadDrop.getInstance().getConfiguration().reload();
                HeadDrop.getInstance().getLang().reload();
                Bukkit.getLogger().info(HeadDrop.getInstance().getLang().getString("Reload"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showLeaderboard(CommandSender sender) {
        if (!sender.hasPermission("headdrop.view.leaderboard")){
            return;
        }
        if (!HeadDrop.getInstance().getConfiguration().getBoolean("Database.Enable")){
            Bukkit.getLogger().severe("[头颅掉落] 请在配置中启用数据库！");
            if (sender instanceof Player) sender.sendMessage("[头颅掉落] 这是一个错误，请向管理员报告！");
            return;
        }
        Map<String, Integer> playerData = HeadDrop.getInstance().getDatabase().getPlayerData();
        List<Map.Entry<String, Integer>> sortedData = new ArrayList<>(playerData.entrySet());
        sortedData.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        sender.sendMessage(miniMessage.deserialize("<gold><bold>=-=-= 头颅收集排行榜 =-=-=</bold></gold>"));
        sender.sendMessage(miniMessage.deserialize("<gray>----------------------------</gray>"));

        for (int i = 0; i < Math.min(sortedData.size(), 10); i++) {
            Map.Entry<String, Integer> entry = sortedData.get(i);
            Component message = miniMessage.deserialize("""
             <aqua>#%d</aqua> <yellow>%s</yellow> - <green>%d</green> <gold>个头颅</gold>
            """.formatted(i + 1, entry.getKey(), entry.getValue()));
            sender.sendMessage(message);
        }

        sender.sendMessage(miniMessage.deserialize("<gray>----------------------------</gray>"));

    }

    private void generateDebugFile(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            try {
                File debugFile = new File(HeadDrop.getInstance().getDataFolder().getAbsolutePath() + File.separator + "debug.txt");
                if (debugFile.exists()) {
                    debugFile.delete();
                }
                debugFile.createNewFile();

                try (FileWriter writer = new FileWriter(debugFile)) {
                    writer.write("服务器名称: " + Bukkit.getServer().getName() + "\n");
                    writer.write("服务器版本: " + Bukkit.getServer().getVersion() + "\n");
                    writer.write("插件版本: " + HeadDrop.getInstance().getDescription().getVersion() + "\n");
                    writer.write("Java 版本: " + System.getProperty("java.version") + "\n");
                    writer.write("操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n");
                    writer.write("\n");
                    writer.write("需要击杀者: " + HeadDrop.getInstance().getConfiguration().getBoolean("Config.Require-Killer-Player") + "\n");
                    writer.write("需要击杀者权限: " + HeadDrop.getInstance().getConfiguration().getBoolean("Config.Killer-Require-Permission") + "\n");
                    writer.write("启用抢夺附魔: " + HeadDrop.getInstance().getConfiguration().getBoolean("Config.Enable-Looting") + "\n");
                    writer.write("启用权限几率: " + HeadDrop.getInstance().getConfiguration().getBoolean("Config.Enable-Perm-Chance") + "\n");
                    writer.write("数据库: " + HeadDrop.getInstance().getConfiguration().getBoolean("Database.Online") + "\n");
                    writer.write("高级版: " + "True" + "\n");
                }
                Bukkit.getLogger().info("[头颅掉落-调试] debug.txt 文件已创建！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openGUI(CommandSender sender) {
        if (sender instanceof Player player) {
            if (player.hasPermission("headdrop.gui.view")) {
                HeadGUI gui = new HeadGUI();
                player.openInventory(gui.getInventory());
            } else {
                lang.pcmd();
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equals("headdrop") && args.length == 1) {
            return Arrays.asList("help", "帮助", "reload", "重载", "leaderboard", "排行榜", "gui", "界面");
        }
        return Collections.emptyList();
    }
}
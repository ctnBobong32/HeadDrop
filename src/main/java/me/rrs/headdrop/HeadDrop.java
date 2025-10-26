package me.rrs.headdrop;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.Pattern;
import dev.dejvokep.boostedyaml.dvs.segment.Segment;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import me.rrs.headdrop.commands.Head;
import me.rrs.headdrop.commands.MainCommand;
import me.rrs.headdrop.database.Database;
import me.rrs.headdrop.hook.GeyserMC;
import me.rrs.headdrop.hook.HeadDropExpansion;
import me.rrs.headdrop.hook.WorldGuardSupport;
import me.rrs.headdrop.listener.EntityDeath;
import me.rrs.headdrop.listener.HeadGUI;
import me.rrs.headdrop.util.UpdateAPI;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;

import java.io.File;
import java.io.IOException;

/**
 * 头颅掉落插件主类
 * 处理生物死亡时的头颅掉落功能
 * 作者: RRS
 */
public class HeadDrop extends JavaPlugin {

    // 实例管理
    private static HeadDrop instance;
    public static HeadDrop getInstance() { return instance; }

    // 配置文件
    private YamlDocument lang;
    private YamlDocument config;
    public YamlDocument getConfiguration() { return config; }
    public YamlDocument getLang() { return lang; }

    // 核心组件
    private Database database;
    public Database getDatabase() { return database; }

    /**
     * 插件加载时执行
     */
    @Override
    public void onLoad() {
        instance = this;
        loadConfigurations();
        setupDatabase();
        initializeWorldGuardSupport();
    }

    /**
     * 插件启用时执行
     */
    @Override
    public void onEnable() {
        displayStartupMessage();
        setupMetrics();
        registerComponents();
        startUpdateChecker();
        startWebServer();
        logInfo("插件启用成功！");
    }

    /**
     * 插件禁用时执行
     */
    @Override
    public void onDisable() {
        stopWebServer();
        logInfo("插件已禁用");
    }

    // region 配置和设置
    /**
     * 加载配置文件
     */
    private void loadConfigurations() {
        try {
            lang = createYamlDocument("lang.yml", "Version");
            config = createYamlDocument("config.yml", "Config.Version");
        } catch (IOException e) {
            logSevere("加载配置文件失败！");
            e.printStackTrace();
        }
    }

    /**
     * 创建YAML文档
     * @param fileName 文件名
     * @param versionKey 版本键
     * @return YAML文档
     * @throws IOException 文件操作异常
     */
    private YamlDocument createYamlDocument(String fileName, String versionKey) throws IOException {
        return YamlDocument.create(
                new File(getDataFolder(), fileName),
                getResource(fileName),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder()
                        .setAutoSave(true)
                        .setVersioning(new Pattern(
                                        Segment.range(1, Integer.MAX_VALUE),
                                        Segment.literal("."),
                                        Segment.range(0, 100)),
                                versionKey
                        ).build()
        );
    }

    /**
     * 设置数据库
     */
    private void setupDatabase() {
        database = new Database();
        database.setupDataSource();
        database.createTable();
        database.cleanupOldData(config.getInt("Database.Cleanup", 30));
    }

    /**
     * 初始化WorldGuard支持
     */
    private void initializeWorldGuardSupport() {
        try {
            new WorldGuardSupport();
        } catch (NoClassDefFoundError ignored) {
            // WorldGuard未安装，忽略错误
        }
    }

    // region 组件注册
    /**
     * 注册组件
     */
    private void registerComponents() {
        registerEvents();
        registerCommands();
        registerPlaceholderAPI();
        registerGeyserHook();
    }

    /**
     * 注册事件监听器
     */
    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new EntityDeath(), this);
        pm.registerEvents(new HeadGUI.GUIListener(), this);
    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        getCommand("head").setExecutor(new Head());
        getCommand("headdrop").setExecutor(new MainCommand());
    }

    /**
     * 注册PlaceholderAPI扩展
     */
    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new HeadDropExpansion().register();
            logInfo("已挂钩到 PlaceholderAPI！");
        }
    }

    /**
     * 注册Geyser挂钩
     */
    private void registerGeyserHook() {
        if (Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot")) {
            GeyserApi.api().eventBus().subscribe(
                    new GeyserMC(),
                    GeyserDefineCustomSkullsEvent.class,
                    GeyserMC::onDefineCustomSkulls
            );
            logInfo("已挂钩到 Geyser！");
        }
    }

    // region Web服务器
    /**
     * 启动Web服务器
     */
    private void startWebServer() {
        if (!config.getBoolean("Web.Enable")) return;

        if (!config.getBoolean("Database.Enable")) {
            logSevere("必须启用数据库才能托管排行榜网站！");
            return;
        }

        try {
            WebsiteController handler = new WebsiteController();
            handler.start(config.getInt("Web.Port"));
            logInfo("网站已在端口 " + config.getInt("Web.Port") + " 上线");
        } catch (IOException e) {
            logSevere("启动Web服务器失败！");
            throw new RuntimeException(e);
        }
    }

    /**
     * 停止Web服务器
     */
    private void stopWebServer() {
        if (config.getBoolean("Web.Enable")) {
            new WebsiteController().stop();
        }
    }

    // region 更新检查
    /**
     * 启动更新检查器
     */
    private void startUpdateChecker() {
        if (isFolia()) {
            checkForUpdates();
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() { checkForUpdates(); }
        }.runTaskTimerAsynchronously(this, 0L, 20L * 60L * 30L);
    }

    /**
     * 检查更新
     */
    private void checkForUpdates() {
        UpdateAPI updateAPI = new UpdateAPI();
        if (updateAPI.hasGithubUpdate("RRS-9747", "HeadDrop")) {
            String newVersion = updateAPI.getGithubVersion("RRS-9747", "HeadDrop");
            notifyPlayers(newVersion);
            logUpdateInfo(newVersion);
        }
    }

    /**
     * 通知玩家有更新
     * @param newVersion 新版本号
     */
    private void notifyPlayers(String newVersion) {
        String[] message = createUpdateMessage(newVersion);
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("headdrop.notify"))
                .forEach(p -> p.sendMessage(message));
    }

    /**
     * 记录更新信息
     * @param newVersion 新版本号
     */
    private void logUpdateInfo(String newVersion) {
        String currentVersion = getPluginMeta().getVersion();
        logInfo("发现可用更新！");
        logInfo("当前版本: v" + currentVersion + " | 新版本: v" + newVersion);
        logInfo("下载地址: https://modrinth.com/plugin/headdrop");
    }

    /**
     * 创建更新消息
     * @param newVersion 新版本号
     * @return 更新消息数组
     */
    private String[] createUpdateMessage(String newVersion) {
        String current = getPluginMeta().getVersion();
        return new String[] {
                "§e§l--------------------------------",
                "§b§l当前版本: §6§l" + current,
                "§b§l可用更新: §6§l" + newVersion,
                "§b§l下载地址: §6§lhttps://modrinth.com/plugin/headdrop",
                "§e§l--------------------------------"
        };
    }

    // region 工具方法
    /**
     * 显示启动消息
     */
    private void displayStartupMessage() {
        String version = getPluginMeta().getVersion();
        logInfo("\n==============================");
        logInfo("    头颅掉落插件 v" + version);
        logInfo("==============================\n");
    }

    /**
     * 检查是否为Folia服务器
     * @return 是否为Folia
     */
    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 设置统计指标
     */
    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 13554);
        metrics.addCustomChart(new SimplePie("discord_bot", () ->
                String.valueOf(config.getBoolean("Bot.Enable"))));
        metrics.addCustomChart(new SimplePie("web", () ->
                String.valueOf(config.getBoolean("Web.Enable"))));
    }

    /**
     * 记录信息日志
     * @param message 消息内容
     */
    private void logInfo(String message) {
        getLogger().info("[头颅掉落] " + message);
    }

    /**
     * 记录错误日志
     * @param message 消息内容
     */
    private void logSevere(String message) {
        getLogger().severe("[头颅掉落] " + message);
    }

    /**
     * 记录警告日志
     * @param message 消息内容
     */
    private void logWarning(String message) {
        getLogger().warning("[头颅掉落] " + message);
    }

    /**
     * 获取插件数据文件夹路径
     * @return 数据文件夹路径
     */
    public String getDataFolderPath() {
        return getDataFolder().getAbsolutePath();
    }

    /**
     * 检查插件是否已完全加载
     * @return 是否已加载
     */
    public boolean isPluginLoaded() {
        return instance != null && database != null;
    }
}
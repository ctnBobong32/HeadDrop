package me.rrs.headdrop.util;

import me.rrs.discordutils.DiscordUtils;
import me.rrs.headdrop.HeadDrop;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Discord嵌入消息工具类
 * 用于向Discord频道发送生物头颅掉落通知
 * 作者: RRS
 */
public class Embed {
    
    /**
     * 发送Discord嵌入消息
     * 
     * @param title 消息标题
     * @param description 消息描述
     * @param footer 消息页脚
     */
    public void msg(String title, String description, String footer){
        try {
            // 创建嵌入消息构建器
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setFooter(footer, null);
            
            // 获取配置中的频道ID并发送消息
            String channelId = HeadDrop.getInstance().getConfiguration().getString("Bot.Channel-ID");
            DiscordUtils.getInstance().getJda()
                    .getTextChannelById(channelId)
                    .sendMessageEmbeds(builder.build())
                    .queue();
                    
        } catch (NoClassDefFoundError ignore) {
            // 处理DiscordUtils未安装的情况
            HeadDrop.getInstance().getLogger().severe("你需要安装 DiscordUtils 插件才能使用 Discord 通知功能！");
        } catch (Exception e) {
            // 处理其他可能的异常
            HeadDrop.getInstance().getLogger().warning("发送 Discord 消息时出现错误: " + e.getMessage());
        }
    }

    /**
     * 发送带颜色的Discord嵌入消息
     * 
     * @param title 消息标题
     * @param description 消息描述  
     * @param footer 消息页脚
     * @param color 消息颜色（十六进制）
     */
    public void msgWithColor(String title, String description, String footer, int color){
        try {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setFooter(footer, null)
                    .setColor(color);
            
            String channelId = HeadDrop.getInstance().getConfiguration().getString("Bot.Channel-ID");
            DiscordUtils.getInstance().getJda()
                    .getTextChannelById(channelId)
                    .sendMessageEmbeds(builder.build())
                    .queue();
                    
        } catch (NoClassDefFoundError ignore) {
            HeadDrop.getInstance().getLogger().severe("你需要安装 DiscordUtils 插件才能使用 Discord 通知功能！");
        } catch (Exception e) {
            HeadDrop.getInstance().getLogger().warning("发送 Discord 消息时出现错误: " + e.getMessage());
        }
    }

    /**
     * 发送带缩略图的Discord嵌入消息
     * 
     * @param title 消息标题
     * @param description 消息描述
     * @param footer 消息页脚
     * @param thumbnailUrl 缩略图URL
     */
    public void msgWithThumbnail(String title, String description, String footer, String thumbnailUrl){
        try {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setFooter(footer, null)
                    .setThumbnail(thumbnailUrl);
            
            String channelId = HeadDrop.getInstance().getConfiguration().getString("Bot.Channel-ID");
            DiscordUtils.getInstance().getJda()
                    .getTextChannelById(channelId)
                    .sendMessageEmbeds(builder.build())
                    .queue();
                    
        } catch (NoClassDefFoundError ignore) {
            HeadDrop.getInstance().getLogger().severe("你需要安装 DiscordUtils 插件才能使用 Discord 通知功能！");
        } catch (Exception e) {
            HeadDrop.getInstance().getLogger().warning("发送 Discord 消息时出现错误: " + e.getMessage());
        }
    }
}
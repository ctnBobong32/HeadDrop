package me.rrs.headdrop.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Base64;
import java.util.UUID;

/**
 * 头颅创建工具类
 * 提供创建玩家头颅的便捷方法，支持通过玩家名或Base64纹理创建
 * 作者: RRS
 */
public class SkullCreator {

    // 私有构造方法，防止实例化
    private SkullCreator() {}

    /**
     * 创建一个空的玩家头颅
     * 
     * @return 空的玩家头颅物品
     */
    public static ItemStack createSkull() {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    /**
     * 通过玩家名创建玩家头颅
     * 
     * @param name 玩家名称
     * @return 带有指定玩家皮肤的头部物品
     */
    public static ItemStack createSkullWithName(String name) {
        ItemStack skull = createSkull();
        return itemWithName(skull, name);
    }

    /**
     * 通过Base64纹理数据创建玩家头颅
     * 
     * @param base64 Base64编码的纹理数据
     * @param uuid 玩家UUID（用于标识）
     * @return 带有指定纹理的头部物品
     */
    public static ItemStack createSkullWithBase64(String base64, UUID uuid) {
        ItemStack skull = createSkull();
        return itemWithBase64(skull, base64, uuid);
    }

    /**
     * 为已有的头颅物品设置玩家皮肤
     * 
     * @param item 头颅物品
     * @param name 玩家名称
     * @return 设置好皮肤的头部物品
     * @throws IllegalArgumentException 如果参数为空
     */
    public static ItemStack itemWithName(ItemStack item, String name) {
        notNull(item, "物品");
        notNull(name, "玩家名称");

        // 检查物品是否为玩家头颅
        if (item.getType() != Material.PLAYER_HEAD) {
            throw new IllegalArgumentException("物品必须是玩家头颅类型");
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        // 设置头颅所有者
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 为已有的头颅物品设置Base64纹理
     * 
     * @param item 头颅物品
     * @param base64 Base64编码的纹理数据
     * @param uuid 玩家UUID
     * @return 设置好纹理的头部物品，如果失败返回null
     * @throws IllegalArgumentException 如果参数为空
     */
    private static ItemStack itemWithBase64(ItemStack item, String base64, UUID uuid) {
        notNull(item, "物品");
        notNull(base64, "Base64数据");

        // 检查物品是否为玩家头颅
        if (item.getType() != Material.PLAYER_HEAD) {
            throw new IllegalArgumentException("物品必须是玩家头颅类型");
        }

        if (!(item.getItemMeta() instanceof SkullMeta meta)) {
            return null;
        }

        try {
            // 解码Base64数据
            String json = new String(Base64.getDecoder().decode(base64));

            // 解析JSON获取纹理URL
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            String textureUrl = jsonObject.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url").getAsString();

            // 创建玩家档案并设置纹理
            PlayerProfile profile = Bukkit.createProfile(uuid);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);

            // 应用玩家档案到头颅
            meta.setOwnerProfile(profile);
            item.setItemMeta(meta);

            return item;

        } catch (Exception e) {
            // 记录错误但不抛出异常，避免影响主线程
            Bukkit.getLogger().warning("设置头颅Base64纹理时出现错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 通过URL创建自定义纹理头颅
     * 
     * @param textureUrl 皮肤纹理URL
     * @param uuid 玩家UUID
     * @return 带有指定纹理的头部物品
     */
    public static ItemStack createSkullWithTextureUrl(String textureUrl, UUID uuid) {
        notNull(textureUrl, "纹理URL");
        notNull(uuid, "UUID");

        ItemStack skull = createSkull();
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        try {
            PlayerProfile profile = Bukkit.createProfile(uuid);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);

            meta.setOwnerProfile(profile);
            skull.setItemMeta(meta);

            return skull;

        } catch (Exception e) {
            Bukkit.getLogger().warning("通过URL创建头颅时出现错误: " + e.getMessage());
            return createSkull(); // 返回空头颅作为降级处理
        }
    }

    /**
     * 创建服务器默认头颅（Steve头像）
     * 
     * @return 默认的玩家头颅
     */
    public static ItemStack createDefaultSkull() {
        return createSkullWithName("Steve");
    }

    /**
     * 检查物品是否为有效的玩家头颅
     * 
     * @param item 要检查的物品
     * @return 是否为有效的玩家头颅
     */
    public static boolean isValidSkull(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        return meta != null && (meta.hasOwner() || meta.getOwnerProfile() != null);
    }

    /**
     * 参数非空检查
     * 
     * @param obj 要检查的对象
     * @param name 参数名称（用于错误信息）
     * @throws IllegalArgumentException 如果参数为空
     */
    private static void notNull(Object obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + "不能为空！");
        }
    }

    /**
     * 获取头颅的玩家名称（如果有）
     * 
     * @param item 头颅物品
     * @return 玩家名称，如果没有则返回null
     */
    public static String getSkullOwnerName(ItemStack item) {
        if (!isValidSkull(item)) {
            return null;
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta.hasOwner()) {
            return meta.getOwner();
        }

        PlayerProfile profile = meta.getOwnerProfile();
        if (profile != null && profile.getName() != null) {
            return profile.getName();
        }

        return null;
    }
}
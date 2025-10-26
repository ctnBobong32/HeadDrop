package me.rrs.headdrop.listener;

import com.google.gson.JsonParseException;
import me.rrs.headdrop.HeadDrop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LoreListeners implements Listener {

    private final NamespacedKey loreKey;

    public LoreListeners() {
        this.loreKey = new NamespacedKey(HeadDrop.getInstance(), "headdrop_lore");
    }

    /**
     * 处理方块放置事件 - 保存头颅的Lore信息到方块实体数据中
     * @param event 方块放置事件
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        // 只处理玩家头颅
        if (item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) return;

        Block block = event.getBlockPlaced();
        // 确保是TileEntity方块
        if (!(block.getState() instanceof TileState tileState)) return;

        PersistentDataContainer container = tileState.getPersistentDataContainer();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) return;

        try {
            // 将Lore组件序列化为JSON字符串并用"§"分隔符连接
            String lore = meta.lore().stream()
                    .map(component -> GsonComponentSerializer.gson().serialize(component))
                    .collect(Collectors.joining("§"));
            container.set(loreKey, PersistentDataType.STRING, lore);
            tileState.update();
        } catch (Exception ignored) {
            // 忽略序列化异常
        }
    }

    /**
     * 处理方块破坏事件 - 从方块实体数据中恢复头颅的Lore信息
     * @param event 方块破坏事件
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        // 只处理玩家头颅
        if (block.getType() != Material.PLAYER_HEAD) return;

        if (!(block.getState() instanceof Skull skull)) return;

        PersistentDataContainer container = skull.getPersistentDataContainer();
        // 检查是否包含自定义Lore数据
        if (!container.has(loreKey, PersistentDataType.STRING)) return;

        // 阻止默认掉落
        event.setDropItems(false);
        Collection<ItemStack> drops = block.getDrops();

        for (ItemStack drop : drops) {
            if (drop.getType() != Material.PLAYER_HEAD || !drop.hasItemMeta()) continue;

            ItemMeta meta = drop.getItemMeta();
            String loreString = container.get(loreKey, PersistentDataType.STRING);
            if (loreString != null) {
                try {
                    // 使用"§"分隔符分割Lore字符串，并将每个JSON反序列化为Component
                    List<Component> loreComponents = Arrays.stream(loreString.split("§"))
                            .map(json -> {
                                try {
                                    return GsonComponentSerializer.gson().deserialize(json);
                                } catch (JsonParseException e) {
                                    // 如果JSON解析失败，返回空组件
                                    return Component.empty();
                                }
                            })
                            .collect(Collectors.toList());
                    meta.lore(loreComponents);
                } catch (Exception ignored) {
                    // 忽略反序列化异常
                }
            }
            drop.setItemMeta(meta);
            // 自然掉落带有Lore的头颅
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
    }
}
package com.boydti.fawe.bukkit.block;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.bukkit.util.ItemUtil;
import com.boydti.fawe.object.collection.SoftHashMap;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.BrushHolder;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public class BrushBoundBaseBlock extends BaseBlock implements BrushHolder {
    public static SoftHashMap<Object, BrushTool> brushCache = new SoftHashMap<>();
    private final LocalSession session;
    private final Player player;

    private ItemStack item;
    private BrushTool tool;

    private static CompoundTag getNBT(ItemStack item) {
        ItemUtil util = Fawe.<FaweBukkit>imp().getItemUtil();
        return util != null && item.hasItemMeta() ? util.getNBT(item) : null;
    }

    private static Object getKey(ItemStack item) {
        ItemUtil util = Fawe.<FaweBukkit>imp().getItemUtil();
        return util != null ? util.getNMSItem(item) : item;
    }

    public BrushBoundBaseBlock(Player player, LocalSession session, ItemStack item) {
        super(item.getTypeId(), item.getType().getMaxDurability() != 0 ? 0 : Math.max(0, item.getDurability()), getNBT(item));
        this.item = item;
        this.tool = brushCache.get(getKey(item));
        this.player = player;
        this.session = session;
    }

    @Override
    public BrushTool getTool() {
        if (tool == null && hasNbtData()) {
            String json = getNbtData().getString("weBrushJson");
            if (json != null) {
                try {
                    this.tool = BrushTool.fromString(player, session, json);
                    this.tool.setHolder(this);
                } catch (Throwable ignore) {
                    ignore.printStackTrace();
                }
            }
        }
        return this.tool;
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    public BrushTool setTool(BrushTool tool) {
        this.tool = tool;
        CompoundTag nbt = getNbtData();
        Map<String, Tag> map;
        if (nbt == null) {
            if (tool == null) {
                return tool;
            }
            setNbtData(nbt = new CompoundTag(map = new HashMap<>()));
        } else {
            map = ReflectionUtils.getMap(nbt.getValue());
        }
        brushCache.remove(getKey(item));
        if (tool != null) {
            String json = tool.toString();
            map.put("weBrushJson", new StringTag(json));
        } else if (map.containsKey("weBrushJson")) {
            map.remove("weBrushJson");
        } else {
            return tool;
        }
        item = Fawe.<FaweBukkit>imp().getItemUtil().setNBT(item, nbt);
        if (tool != null) {
            brushCache.put(getKey(item), tool);
        }
        return tool;
    }
}
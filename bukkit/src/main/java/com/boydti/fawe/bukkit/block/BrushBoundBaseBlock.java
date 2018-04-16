package com.boydti.fawe.bukkit.block;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.bukkit.util.ItemUtil;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.util.ReflectionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.WeakHashMap;
import org.bukkit.inventory.ItemStack;

public class BrushBoundBaseBlock extends BaseBlock implements BrushHolder {
    private static WeakHashMap<Object, BrushTool> brushCache = new WeakHashMap<>();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
        super(item.getTypeId(), item.getType().getMaxDurability() != 0 || item.getDurability() > 15 ? 0 : Math.max(0, item.getDurability()), getNBT(item));
        this.item = item;
        this.tool = brushCache.get(getKey(item));
        this.player = player;
        this.session = session;
    }

    private static final ThreadLocal<Boolean> RECURSION = new ThreadLocal<>();

    @Override
    public BrushTool getTool() {
        if (tool == null && hasNbtData()) {
            StringTag json = (StringTag) getNbtData().getValue().get("weBrushJson");
            if (json != null) {
                try {
                    if (RECURSION.get() != null) return null;
                    RECURSION.set(true);

                    this.tool = BrushTool.fromString(player, session, json.getValue());
                    this.tool.setHolder(this);
                    brushCache.put(getKey(item), tool);
                } catch (Throwable ignore) {
                    ignore.printStackTrace();
                    Fawe.debug("Invalid brush for " + player + " holding " + item.getType() + ": " + json.getValue());
                    if (item != null) {
                        item = Fawe.<FaweBukkit>imp().getItemUtil().setNBT(item, null);
                        brushCache.remove(getKey(item));
                    }
                } finally {
                    RECURSION.remove();
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
        CompoundTag display = (CompoundTag) map.get("display");
        Map<String, Tag> displayMap;
        if (tool != null) {
            String json = tool.toString(gson);
            map.put("weBrushJson", new StringTag(json));
            if (display == null) {
                map.put("display", new CompoundTag(displayMap = new HashMap()));
            } else {
                displayMap = ReflectionUtils.getMap(display.getValue());
            }
            displayMap.put("Lore", FaweCache.asTag(json.split("\\r?\\n")));
            String primary = (String) tool.getPrimary().getSettings().get(BrushSettings.SettingType.BRUSH);
            String secondary = (String) tool.getSecondary().getSettings().get(BrushSettings.SettingType.BRUSH);
            if (primary == null) primary = secondary;
            if (secondary == null) secondary = primary;
            if (primary != null) {
                String name = primary == secondary ? primary.split(" ")[0] : primary.split(" ")[0] + " / " + secondary.split(" ")[0];
                displayMap.put("Name", new StringTag(name));
            }
        } else if (map.containsKey("weBrushJson")) {
            map.remove("weBrushJson");
            if (display != null) {
                displayMap = ReflectionUtils.getMap(display.getValue());
                displayMap.remove("Lore");
                displayMap.remove("Name");
                if (displayMap.isEmpty()) {
                    map.remove("display");
                }
            }

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
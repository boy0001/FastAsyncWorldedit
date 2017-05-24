package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.bukkit.v0.BukkitQueue_All;
import com.boydti.fawe.bukkit.v1_10.BukkitQueue_1_10;
import com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11;
import com.boydti.fawe.bukkit.v1_12.BukkitQueue_1_12;
import com.boydti.fawe.bukkit.v1_12.NMSRegistryDumper;
import com.boydti.fawe.bukkit.v1_7.BukkitQueue17;
import com.boydti.fawe.bukkit.v1_8.BukkitQueue18R3;
import com.boydti.fawe.bukkit.v1_9.BukkitQueue_1_9_R1;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.world.World;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitMain extends JavaPlugin {

    static {
        {   // Disable AWE as otherwise both fail to load
            PluginManager manager = Bukkit.getPluginManager();
            try {
                Field pluginsField = manager.getClass().getDeclaredField("plugins");
                Field lookupNamesField = manager.getClass().getDeclaredField("lookupNames");
                pluginsField.setAccessible(true);
                lookupNamesField.setAccessible(true);
                List<Plugin> plugins = (List<Plugin>) pluginsField.get(manager);
                Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(manager);
                pluginsField.set(manager, new ArrayList<Plugin>(plugins) {
                    @Override
                    public boolean add(Plugin plugin) {
                        if (!plugin.getName().startsWith("AsyncWorldEdit")) {
                            return super.add(plugin);
                        } else {
                            Fawe.debug("Disabling `" + plugin.getName() + "` as it is incompatible");
                        }
                        return false;
                    }
                });
                lookupNamesField.set(manager, new ConcurrentHashMap<String, Plugin>(lookupNames) {
                    @Override
                    public Plugin put(String key, Plugin plugin) {
                        if (!plugin.getName().startsWith("AsyncWorldEdit")) {
                            return super.put(key, plugin);
                        }
                        return null;
                    }
                });
            } catch (Throwable ignore) {}
        }
    }

    private Version version = Version.NONE;

    @Override
    public void onEnable() {
        FaweBukkit imp = new FaweBukkit(this);
        for (Version v : Version.values()) {
            try {
                BukkitQueue_0.checkVersion(v.name());
                this.version = v;
                if (version == Version.v1_12_R1) {
                    try {
                        Fawe.debug("Running 1.12 registry dumper!");
                        NMSRegistryDumper dumper = new NMSRegistryDumper(MainUtil.getFile(getDataFolder(), "extrablocks.json"));
                        dumper.run();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                break;
            } catch (IllegalStateException e) {}
        }
    }

    private enum Version {
        v1_7_R4,
        v1_8_R3,
        v1_9_R2,
        v1_10_R1,
        v1_11_R1,
        v1_12_R1,
        NONE,
    }

    public FaweQueue getQueue(World world) {
        switch (version) {
            case v1_7_R4:
                return new BukkitQueue17(world);
            case v1_8_R3:
                return new BukkitQueue18R3(world);
            case v1_9_R2:
                return new BukkitQueue_1_9_R1(world);
            case v1_10_R1:
                return new BukkitQueue_1_10(world);
            case v1_11_R1:
                return new BukkitQueue_1_11(world);
            case v1_12_R1:
                return new BukkitQueue_1_12(world);
            default:
            case NONE:
                return new BukkitQueue_All(world);
        }
    }

    public FaweQueue getQueue(String world) {
        switch (version) {
            case v1_7_R4:
                return new BukkitQueue17(world);
            case v1_8_R3:
                return new BukkitQueue18R3(world);
            case v1_9_R2:
                return new BukkitQueue_1_9_R1(world);
            case v1_10_R1:
                return new BukkitQueue_1_10(world);
            case v1_11_R1:
                return new BukkitQueue_1_11(world);
            case v1_12_R1:
                return new BukkitQueue_1_12(world);
            default:
            case NONE:
                return new BukkitQueue_All(world);
        }
    }
}
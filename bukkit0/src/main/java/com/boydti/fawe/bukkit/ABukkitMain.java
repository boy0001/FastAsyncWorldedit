package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweQueue;
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

public abstract class ABukkitMain extends JavaPlugin {

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

    @Override
    public void onEnable() {
        FaweBukkit imp = new FaweBukkit(this);
    }

    public abstract FaweQueue getQueue(World world);

    public abstract FaweQueue getQueue(String world);
}
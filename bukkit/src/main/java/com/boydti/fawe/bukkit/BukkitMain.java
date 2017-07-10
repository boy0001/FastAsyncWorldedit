package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            try {
                File output = new File(this.getDataFolder().getParentFile(), "WorldEdit.jar");
                URL worldEditUrl = new URL("https://addons.cursecdn.com/files/2431/372/worldedit-bukkit-6.1.7.2.jar");
                try (ReadableByteChannel rbc = Channels.newChannel(worldEditUrl.openStream())) {
                    try (FileOutputStream fos = new FileOutputStream(output)) {
                        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    }
                }
                Bukkit.getPluginManager().loadPlugin(output);
            } catch (Throwable e) {
                e.printStackTrace();
                Fawe.debug("====== INSTALL WORLDEDIT ======");
                Fawe.debug("FAWE requires WorldEdit to function correctly");
                Fawe.debug("Info: https://github.com/boy0001/FastAsyncWorldedit/releases/");
                Fawe.debug("===============================");
                return;
            }
        }
        FaweBukkit imp = new FaweBukkit(this);
    }
}
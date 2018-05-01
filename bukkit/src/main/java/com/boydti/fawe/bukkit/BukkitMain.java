package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.Jars;
import java.io.File;
import java.io.FileOutputStream;
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
                        if (plugin.getName().startsWith("AsyncWorldEdit")) {
                            Fawe.debug("Disabling `" + plugin.getName() + "` as it is incompatible");
                        } else if (plugin.getName().startsWith("BetterShutdown")) {
                            Fawe.debug("Disabling `" + plugin.getName() + "` as it is incompatible (Improperly shaded classes from com.sk89q.minecraft.util.commands)");
                        } else {
                            return super.add(plugin);
                        }
                        return false;
                    }
                });
                lookupNamesField.set(manager, new ConcurrentHashMap<String, Plugin>(lookupNames) {
                    @Override
                    public Plugin put(String key, Plugin plugin) {
                        if (plugin.getName().startsWith("AsyncWorldEdit") || plugin.getName().startsWith("BetterShutdown")) {
                            return null;
                        }
                        return super.put(key, plugin);

                    }
                });
            } catch (Throwable ignore) {}
        }
    }

    @Override
    public void onEnable() {
        Plugin toLoad = null;
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            try {
                File output = new File(this.getDataFolder().getParentFile(), "WorldEdit.jar");
                byte[] weJar = Jars.WE_B_6_1_7_2.download();
                try (FileOutputStream fos = new FileOutputStream(output)) {
                    fos.write(weJar);
                }
                toLoad = Bukkit.getPluginManager().loadPlugin(output);
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
        if (toLoad != null) {
            Bukkit.getPluginManager().enablePlugin(toLoad);
        }
    }

    @Override
    public void onDisable() {
        Fawe.get().onDisable();
    }
}
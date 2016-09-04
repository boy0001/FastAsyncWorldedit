package com.boydti.fawe.nukkit.core;

import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.util.YAMLConfiguration;
import java.io.File;

public class NukkitConfiguration extends YAMLConfiguration {

    public boolean noOpPermissions = false;
    private final NukkitWorldEdit plugin;

    public NukkitConfiguration(YAMLProcessor config, NukkitWorldEdit plugin) {
        super(config, plugin.getWELogger());
        this.plugin = plugin;
    }

    @Override
    public void load() {
        super.load();
        noOpPermissions = config.getBoolean("no-op-permissions", false);
        migrateLegacyFolders();
    }

    private void migrateLegacyFolders() {
        migrate(scriptsDir, "craftscripts");
        migrate(saveDir, "schematics");
        migrate("drawings", "draw.js images");
    }

    private void migrate(String file, String name) {
        File fromDir = new File(".", file);
        File toDir = new File(getWorkingDirectory(), file);
        if (fromDir.exists() & !toDir.exists()) {
            if (fromDir.renameTo(toDir)) {
                plugin.getLogger().info("Migrated " + name + " folder '" + file +
                        "' from server root to plugin data folder.");
            } else {
                plugin.getLogger().warning("Error while migrating " + name + " folder!");
            }
        }
    }

    @Override
    public File getWorkingDirectory() {
        return plugin.getDataFolder();
    }
}
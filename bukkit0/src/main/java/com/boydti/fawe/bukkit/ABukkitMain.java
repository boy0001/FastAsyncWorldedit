package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweQueue;
import com.sk89q.worldedit.EditSession;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ABukkitMain extends JavaPlugin {

    @Override
    public void onEnable() {
        new FaweBukkit(this);

        try {
            SnipeData.inject();
            Sniper.inject();
            Fawe.debug("Injected VoxelSniper classes");
        } catch (Throwable ignore) {}
    }

    public abstract FaweQueue getQueue(String world);

    public abstract EditSessionWrapper getEditSessionWrapper(EditSession session);
}

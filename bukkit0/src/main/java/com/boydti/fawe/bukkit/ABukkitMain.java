package com.boydti.fawe.bukkit;

import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.util.FaweQueue;
import com.sk89q.worldedit.EditSession;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ABukkitMain extends JavaPlugin {

    @Override
    public void onEnable() {
        new FaweBukkit(this);
    }

    public abstract FaweQueue getQueue(String world);

    public abstract EditSessionWrapper getEditSessionWrapper(EditSession session);
}

package com.boydti.fawe.bukkit;

import com.boydti.fawe.object.FaweQueue;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ABukkitMain extends JavaPlugin {

    @Override
    public void onEnable() {
        FaweBukkit imp = new FaweBukkit(this);
    }

    public abstract FaweQueue getQueue(String world);
}
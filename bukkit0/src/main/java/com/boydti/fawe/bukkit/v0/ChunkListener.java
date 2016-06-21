package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ChunkListener implements Listener {
    public ChunkListener() {
        Bukkit.getPluginManager().registerEvents(ChunkListener.this, Fawe.<FaweBukkit>imp().getPlugin());
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                physicsFreeze = false;
                physicsLimit = Settings.TICK_LIMITER.PHYSICS;
                itemLimit = Settings.TICK_LIMITER.PHYSICS;
            }
        }, 1);
    }

    private int physicsLimit = Integer.MAX_VALUE;
    private int itemLimit = Integer.MAX_VALUE;
    public static boolean physicsFreeze = false;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPhysics(BlockPhysicsEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
        } else if (physicsLimit-- < 0) {
            physicsFreeze = true;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
        } else if (itemLimit-- < 0) {
            physicsFreeze = true;
        }
    }
}

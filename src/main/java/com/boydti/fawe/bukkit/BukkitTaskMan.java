package com.boydti.fawe.bukkit;

import java.util.HashMap;

import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.boydti.fawe.util.TaskManager;

public class BukkitTaskMan extends TaskManager {
    
    private final Plugin plugin;
    
    public BukkitTaskMan(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(final Runnable r, final int interval) {
        return plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, r, interval, interval);
    }
    
    @Override
    public int repeatAsync(final Runnable r, final int interval) {
        return plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, r, interval, interval);
    }
    
    public MutableInt index = new MutableInt(0);
    public HashMap<Integer, Integer> tasks = new HashMap<>();
    
    @Override
    public void async(final Runnable r) {
        if (r == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r).getTaskId();
    }
    
    @Override
    public void task(final Runnable r) {
        if (r == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, r).getTaskId();
    }
    
    @Override
    public void later(final Runnable r, final int delay) {
        if (r == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, r, delay).getTaskId();
    }
    
    @Override
    public void laterAsync(final Runnable r, final int delay) {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, r, delay);
    }
    
    @Override
    public void cancel(final int task) {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
        }
    }
}

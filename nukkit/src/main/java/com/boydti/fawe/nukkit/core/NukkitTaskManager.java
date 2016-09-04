package com.boydti.fawe.nukkit.core;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.scheduler.TaskHandler;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NukkitTaskManager {

    private final Plugin plugin;

    public NukkitTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    public int repeat(final Runnable r, final int interval) {
        TaskHandler task = this.plugin.getServer().getScheduler().scheduleRepeatingTask(r, interval, false);
        return task.getTaskId();
    }

    public int repeatAsync(final Runnable r, final int interval) {
        TaskHandler task = this.plugin.getServer().getScheduler().scheduleRepeatingTask(r, interval, true);
        return task.getTaskId();
    }

    public AtomicInteger index = new AtomicInteger(0);
    public HashMap<Integer, Integer> tasks = new HashMap<>();

    public void async(final Runnable r) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().scheduleTask(r, true);
    }

    public void task(final Runnable r) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().scheduleTask(r, false);
    }

    public void later(final Runnable r, final int delay) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().scheduleDelayedTask(r, delay);
    }

    public void laterAsync(final Runnable r, final int delay) {
        this.plugin.getServer().getScheduler().scheduleDelayedTask(r, delay, true);
    }

    public void cancel(final int task) {
        if (task != -1) {
            this.plugin.getServer().getScheduler().cancelTask(task);
        }
    }
}

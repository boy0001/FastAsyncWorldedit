package com.boydti.fawe.nukkit.core;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.scheduler.TaskHandler;
import com.boydti.fawe.util.TaskManager;
import java.util.HashMap;
import org.apache.commons.lang.mutable.MutableInt;

public class NukkitTaskManager extends TaskManager {

    private final Plugin plugin;

    public NukkitTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(final Runnable r, final int interval) {
        TaskHandler task = this.plugin.getServer().getScheduler().scheduleRepeatingTask(r, interval, false);
        return task.getTaskId();
    }

    @Override
    public int repeatAsync(final Runnable r, final int interval) {
        TaskHandler task = this.plugin.getServer().getScheduler().scheduleRepeatingTask(r, interval, true);
        return task.getTaskId();
    }

    public MutableInt index = new MutableInt(0);
    public HashMap<Integer, Integer> tasks = new HashMap<>();

    @Override
    public void async(final Runnable r) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().scheduleTask(r, true);
    }

    @Override
    public void task(final Runnable r) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().scheduleTask(r, false);
    }

    @Override
    public void later(final Runnable r, final int delay) {
        if (r == null) {
            return;
        }
        this.plugin.getServer().getScheduler().scheduleDelayedTask(r, delay);
    }

    @Override
    public void laterAsync(final Runnable r, final int delay) {
        this.plugin.getServer().getScheduler().scheduleDelayedTask(r, delay, true);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            this.plugin.getServer().getScheduler().cancelTask(task);
        }
    }
}

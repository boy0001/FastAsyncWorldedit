package com.boydti.fawe.forge;

import com.boydti.fawe.util.TaskManager;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

public class SpongeTaskMan extends TaskManager {

    private final SpongeMain plugin;

    public SpongeTaskMan(SpongeMain plugin) {
        this.plugin = plugin;
    }

    private final AtomicInteger i = new AtomicInteger();

    private final HashMap<Integer, Task> tasks = new HashMap<>();

    @Override
    public int repeat(Runnable runnable, int interval) {
        int val = this.i.incrementAndGet();
        Task.Builder builder = Sponge.getGame().getScheduler().createTaskBuilder();
        Task.Builder built = builder.delayTicks(interval).intervalTicks(interval).execute(runnable);
        Task task = built.submit(plugin);
        this.tasks.put(val, task);
        return val;
    }

    @Override
    public int repeatAsync(Runnable runnable, int interval) {
        int val = this.i.incrementAndGet();
        Task.Builder builder = Sponge.getGame().getScheduler().createTaskBuilder();
        Task.Builder built = builder.delayTicks(interval).async().intervalTicks(interval).execute(runnable);
        Task task = built.submit(plugin);
        this.tasks.put(val, task);
        return val;
    }

    @Override
    public void async(Runnable runnable) {
        Task.Builder builder = Sponge.getGame().getScheduler().createTaskBuilder();
        builder.async().execute(runnable).submit(plugin);
    }

    @Override
    public void task(Runnable runnable) {
        Task.Builder builder = Sponge.getGame().getScheduler().createTaskBuilder();
        builder.execute(runnable).submit(plugin);
    }

    @Override
    public void later(Runnable runnable, int delay) {
        Task.Builder builder = Sponge.getGame().getScheduler().createTaskBuilder();
        builder.delayTicks(delay).execute(runnable).submit(plugin);
    }

    @Override
    public void laterAsync(Runnable runnable, int delay) {
        Task.Builder builder = Sponge.getGame().getScheduler().createTaskBuilder();
        builder.async().delayTicks(delay).execute(runnable).submit(plugin);
    }

    @Override
    public void cancel(int i) {
        Task task = this.tasks.remove(i);
        if (task != null) {
            task.cancel();
        }
    }
}
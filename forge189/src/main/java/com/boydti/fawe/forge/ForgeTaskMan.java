package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ForgeTaskMan extends TaskManager {

    private final ConcurrentLinkedDeque<Runnable> syncTasks = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Runnable> asyncTasks = new ConcurrentLinkedDeque<>();

    private final ConcurrentHashMap<Integer, Runnable> taskIdMap = new ConcurrentHashMap<>();


    private final AtomicInteger taskId = new AtomicInteger();
    private final ExecutorService executor;

    public ForgeTaskMan(int size) {
        this.executor = Executors.newFixedThreadPool(size);
        FMLCommonHandler.instance().bus().register(this);
    }


    @Override
    public int repeat(final Runnable r, final int interval) {
        if (r == null) {
            return -1;
        }
        int id = taskId.incrementAndGet();
        taskIdMap.put(id, r);
        task(new Runnable() {
            @Override
            public void run() {
                if (!taskIdMap.containsKey(id)) {
                    return;
                }
                try {
                    r.run();
                } catch (Throwable e) {
                    MainUtil.handleError(e);
                }
                later(this, interval);
            }
        });
        return id;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        Fawe.get().setMainThread();
        int asyncSize = asyncTasks.size();
        for (int i = 0; i < asyncSize; i++) {
            Runnable item = asyncTasks.poll();
            if (item != null) {
                async(item);
            }
        }
        int syncSize = syncTasks.size();
        for (int i = 0; i < syncSize; i++) {
            Runnable item = syncTasks.poll();
            if (item != null) {
                try {
                    item.run();
                } catch (Throwable e) {
                    MainUtil.handleError(e);
                }
            }
        }
    }

    @Override
    public int repeatAsync(Runnable r, int interval) {
        if (r == null) {
            return -1;
        }
        int id = taskId.incrementAndGet();
        taskIdMap.put(id, r);
        async(new Runnable() {
            @Override
            public void run() {
                if (!taskIdMap.containsKey(id)) {
                    return;
                }
                try {
                    r.run();
                } catch (Throwable e) {
                    MainUtil.handleError(e);
                }
                laterAsync(this, interval);
            }
        });
        return id;
    }

    @Override
    public void async(Runnable r) {
        if (r == null) {
            return;
        }
        executor.execute(r);
    }

    @Override
    public void task(Runnable r) {
        if (r == null) {
            return;
        }
        syncTasks.add(r);
    }

    @Override
    public void later(Runnable r, int delay) {
        if (r == null) {
            return;
        }
        AtomicInteger remaining = new AtomicInteger(delay);
        task(new Runnable() {
            @Override
            public void run() {
                if (remaining.decrementAndGet() <= 0) {
                    try {
                        r.run();
                    } catch (Throwable e) {
                        MainUtil.handleError(e);
                    }
                    return;
                }
                task(this);
            }
        });
    }

    @Override
    public void laterAsync(Runnable r, int delay) {
        if (r == null) {
            return;
        }
        AtomicInteger remaining = new AtomicInteger(delay);
        task(new Runnable() {
            @Override
            public void run() {
                if (remaining.decrementAndGet() <= 0) {
                    try {
                        async(r);
                    } catch (Throwable e) {
                        MainUtil.handleError(e);
                    }
                    return;
                }
                task(this);
            }
        });
    }

    @Override
    public void cancel(int task) {
        taskIdMap.remove(task);
    }
}

package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RunnableVal2;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class SetQueue {

    /**
     * The implementation specific queue
     */
    public static final SetQueue IMP = new SetQueue();

    public final LinkedBlockingDeque<FaweQueue> activeQueues;
    public final LinkedBlockingDeque<FaweQueue> inactiveQueues;

    /**
     * Used to calculate elapsed time in milliseconds and ensure block placement doesn't lag the server
     */
    private long last;
    private long secondLast;
    private long lastSuccess;
    
    /**
     * A queue of tasks that will run when the queue is empty
     */
    private final LinkedBlockingDeque<Runnable> runnables = new LinkedBlockingDeque<>();

    private final RunnableVal2<Long, FaweQueue> SET_TASK = new RunnableVal2<Long, FaweQueue>() {
        @Override
        public void run(Long free, FaweQueue queue) {
            do {
                final FaweChunk<?> current = queue.next();
                if (current == null) {
                    lastSuccess = last;
                    if (inactiveQueues.size() == 0 && activeQueues.size() == 0) {
                        tasks();
                    }
                    return;
                }
            } while (((SetQueue.this.secondLast = System.currentTimeMillis()) - SetQueue.this.last) < free);
        }
    };

    public SetQueue() {
        activeQueues = new LinkedBlockingDeque();
        inactiveQueues = new LinkedBlockingDeque<>();
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                if (inactiveQueues.size() == 0 && activeQueues.size() == 0) {
                    tasks();
                    return;
                }
                if (!MemUtil.isMemoryFree()) {
                    final int mem = MemUtil.calculateMemory();
                    if (mem != Integer.MAX_VALUE) {
                        if ((mem <= 1) && Settings.ENABLE_HARD_LIMIT) {
                            for (FaweQueue queue : getAllQueues()) {
                                queue.saveMemory();
                            }
                            return;
                        }
                        if (SetQueue.this.forceChunkSet()) {
                            System.gc();
                        } else {
                            SetQueue.this.tasks();
                        }
                        return;
                    }
                }
                SET_TASK.value1 = Settings.ALLOCATE + 50 + Math.min((50 + SetQueue.this.last) - (SetQueue.this.last = System.currentTimeMillis()), SetQueue.this.secondLast - System.currentTimeMillis());
                SET_TASK.value2 = getNextQueue();
                if (SET_TASK.value2 == null) {
                    return;
                }
                if (Settings.UNSAFE_PARALLEL_THREADS <= 1) {
                    SET_TASK.run();
                } else {
                    boolean timingsEnabled = true;
                    try {
                        Field fieldEnabled = Class.forName("co.aikar.timings.Timings").getDeclaredField("timingsEnabled");
                        fieldEnabled.setAccessible(true);
                        timingsEnabled = (boolean) fieldEnabled.get(null);
                        if (timingsEnabled) {
                            fieldEnabled.set(null, false);
                            Method methodCheck = Class.forName("co.aikar.timings.TimingsManager").getDeclaredMethod("recheckEnabled");
                            methodCheck.setAccessible(true);
                            methodCheck.invoke(null);
                        }
                    } catch (Throwable ignore) {ignore.printStackTrace();}
                    try { Class.forName("org.spigotmc.AsyncCatcher").getField("enabled").set(null, false); } catch (Throwable ignore) {}
                    ArrayList<Thread> threads = new ArrayList<Thread>();
                    for (int i = 0; i < Settings.UNSAFE_PARALLEL_THREADS; i++) {
                        threads.add(new Thread(SET_TASK));
                    }
                    for (Thread thread : threads) {
                        thread.start();
                    }
                    for (Thread thread : threads) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {Field fieldEnabled = Class.forName("co.aikar.timings.Timings").getDeclaredField("timingsEnabled");fieldEnabled.setAccessible(true);fieldEnabled.set(null, timingsEnabled);
                    } catch (Throwable ignore) {ignore.printStackTrace();}
                    try { Class.forName("org.spigotmc.AsyncCatcher").getField("enabled").set(null, true); } catch (Throwable ignore) {}
                }
            }
        }, 1);
    }

    public void enqueue(FaweQueue queue) {
        inactiveQueues.remove(queue);
        activeQueues.add(queue);
    }

    public List<FaweQueue> getAllQueues() {
        ArrayList<FaweQueue> list = new ArrayList<FaweQueue>(activeQueues.size() + inactiveQueues.size());
        list.addAll(inactiveQueues);
        list.addAll(activeQueues);
        return list;
    }

    public List<FaweQueue> getActiveQueues() {
        return new ArrayList<>(activeQueues);
    }

    public List<FaweQueue> getInactiveQueues() {
        return new ArrayList<>(inactiveQueues);
    }

    public FaweQueue getNewQueue(String world, boolean autoqueue) {
        FaweQueue queue = Fawe.imp().getNewQueue(world);
        if (autoqueue) {
            inactiveQueues.add(queue);
        }
        return queue;
    }

    public FaweQueue getNextQueue() {
        while (activeQueues.size() > 0) {
            FaweQueue queue = activeQueues.peek();
            if (queue != null && queue.size() > 0) {
                return queue;
            } else {
                activeQueues.poll();
            }
        }
        if (inactiveQueues.size() > 0) {
            ArrayList<FaweQueue> tmp = new ArrayList<>(inactiveQueues);
            if (Settings.QUEUE_MAX_WAIT != -1) {
                long now = System.currentTimeMillis();
                if (lastSuccess == 0) {
                    lastSuccess = now;
                }
                long diff = now - lastSuccess;
                if (diff > Settings.QUEUE_MAX_WAIT) {
                    for (FaweQueue queue : tmp) {
                        if (queue != null && queue.size() > 0) {
                            return queue;
                        } else {
                            activeQueues.poll();
                        }
                    }
                    if (diff > Settings.QUEUE_DISCARD_AFTER) {
                        // These edits never finished
                        inactiveQueues.clear();
                    }
                    return null;
                }
            }
            if (Settings.QUEUE_SIZE != -1) {
                int total = 0;
                for (FaweQueue queue : tmp) {
                    total += queue.size();
                }
                if (total > Settings.QUEUE_SIZE) {
                    for (FaweQueue queue : tmp) {
                        if (queue != null && queue.size() > 0) {
                            return queue;
                        } else {
                            activeQueues.poll();
                        }
                    }
                }
            }
        }
        return null;
    }

    public FaweChunk<?> next() {
        while (activeQueues.size() > 0) {
            FaweQueue queue = activeQueues.poll();
            if (queue != null) {
                final FaweChunk<?> set = queue.next();
                if (set != null) {
                    activeQueues.add(queue);
                    return set;
                }
            }
        }
        if (inactiveQueues.size() > 0) {
            ArrayList<FaweQueue> tmp = new ArrayList<>(inactiveQueues);
            if (Settings.QUEUE_MAX_WAIT != -1) {
                long now = System.currentTimeMillis();
                if (lastSuccess == 0) {
                    lastSuccess = now;
                }
                long diff = now - lastSuccess;
                if (diff > Settings.QUEUE_MAX_WAIT) {
                    for (FaweQueue queue : tmp) {
                        FaweChunk result = queue.next();
                        if (result != null) {
                            return result;
                        }
                    }
                    if (diff > Settings.QUEUE_DISCARD_AFTER) {
                        // These edits never finished
                        inactiveQueues.clear();
                    }
                    return null;
                }
            }
            if (Settings.QUEUE_SIZE != -1) {
                int total = 0;
                for (FaweQueue queue : tmp) {
                    total += queue.size();
                }
                if (total > Settings.QUEUE_SIZE) {
                    for (FaweQueue queue : tmp) {
                        FaweChunk result = queue.next();
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean forceChunkSet() {
        return next() != null;
    }

    public boolean isDone() {
        return activeQueues.size() == 0 && inactiveQueues.size() == 0;
    }

    public boolean addTask(final Runnable whenDone) {
        if (this.isDone()) {
            // Run
            this.tasks();
            if (whenDone != null) {
                whenDone.run();
            }
            return true;
        }
        if (whenDone != null) {
            this.runnables.add(whenDone);
        }
        return false;
    }

    public synchronized boolean tasks() {
        if (this.runnables.size() == 0) {
            return false;
        }
        final LinkedBlockingDeque<Runnable> tmp = new LinkedBlockingDeque<>(this.runnables);
        this.runnables.clear();
        for (final Runnable runnable : tmp) {
            runnable.run();
        }
        return true;
    }
}

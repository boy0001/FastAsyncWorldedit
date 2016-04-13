package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SetQueue {

    /**
     * The implementation specific queue
     */
    public static final SetQueue IMP = new SetQueue();

    public final ArrayDeque<FaweQueue> queues;

    /**
     * Track the time in ticks
     */
    private final AtomicInteger time_waiting = new AtomicInteger(2);
    private final AtomicInteger time_current = new AtomicInteger(0);
    
    /**
     * Used to calculate elapsed time in milliseconds and ensure block placement doesn't lag the server 
     */
    private long last;
    private long last2;
    
    /**
     * A queue of tasks that will run when the queue is empty
     */
    private final ArrayDeque<Runnable> runnables = new ArrayDeque<>();


    public SetQueue() {
        queues = new ArrayDeque();
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                if (!MemUtil.isMemoryFree()) {
                    final int mem = MemUtil.calculateMemory();
                    if (mem != Integer.MAX_VALUE) {
                        if ((mem <= 1) && Settings.ENABLE_HARD_LIMIT) {
                            for (FaweQueue queue : getQueues()) {
                                queue.saveMemory();
                            }
                            return;
                        }
                        if (SetQueue.this.forceChunkSet()) {
                            System.gc();
                        } else {
                            SetQueue.this.time_current.incrementAndGet();
                            SetQueue.this.tasks();
                        }
                        return;
                    }
                }
                final long free = Settings.ALLOCATE + 50 + Math.min((50 + SetQueue.this.last) - (SetQueue.this.last = System.currentTimeMillis()), SetQueue.this.last2 - System.currentTimeMillis());
                SetQueue.this.time_current.incrementAndGet();
                do {
                    if (SetQueue.this.isWaiting()) {
                        return;
                    }
                    final FaweChunk<?> current = next();
                    if (current == null) {
                        SetQueue.this.time_waiting.set(Math.max(SetQueue.this.time_waiting.get(), SetQueue.this.time_current.get() - 2));
                        SetQueue.this.tasks();
                        return;
                    }
                } while (((SetQueue.this.last2 = System.currentTimeMillis()) - SetQueue.this.last) < free);
                SetQueue.this.time_waiting.set(SetQueue.this.time_current.get() - 1);
            }
        }, 1);
    }

    public void enqueue(FaweQueue queue) {
        queues.add(queue);
    }

    public List<FaweQueue> getQueues() {
        return new ArrayList<>(queues);
    }

    public FaweQueue getNewQueue(String world) {
        return Fawe.imp().getNewQueue(world);
    }

    public FaweChunk<?> next() {
        while (queues.size() > 0) {
            FaweQueue queue = queues.poll();
            final FaweChunk<?> set = queue.next();
            if (set != null) {
                queues.add(queue);
                return set;
            }
        }
        return null;
    }

    public boolean forceChunkSet() {
        return next() != null;
    }

    public boolean isWaiting() {
        return this.time_waiting.get() >= this.time_current.get();
    }

    public boolean isDone() {
        return (this.time_waiting.get() + 1) < this.time_current.get();
    }

    public void setWaiting() {
        this.time_waiting.set(this.time_current.get() + 1);
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

    public boolean tasks() {
        if (this.runnables.size() == 0) {
            return false;
        }
        final ArrayDeque<Runnable> tmp = this.runnables.clone();
        this.runnables.clear();
        for (final Runnable runnable : tmp) {
            runnable.run();
        }
        return true;
    }
}

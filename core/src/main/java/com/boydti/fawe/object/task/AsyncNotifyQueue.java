package com.boydti.fawe.object.task;

import com.boydti.fawe.util.TaskManager;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AsyncNotifyQueue {
    private Object lock = new Object();
    private final Runnable task;
    private final AtomicBoolean running = new AtomicBoolean();

    public AsyncNotifyQueue() {
        this.task = new Runnable() {
            @Override
            public void run() {
                operate();
                synchronized (lock) {
                    if (hasQueued()) TaskManager.IMP.async(this);
                    else running.set(false);
                }
            }
        };
    }

    public abstract boolean hasQueued();

    public void queue(Runnable queueTask) {
        synchronized (lock) {
            queueTask.run();
            if (!running.get()) {
                running.set(true);
                TaskManager.IMP.async(task);
            }
        }
    }

    public abstract void operate();
}

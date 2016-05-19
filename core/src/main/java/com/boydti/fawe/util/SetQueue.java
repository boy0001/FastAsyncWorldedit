package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RunnableVal2;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class SetQueue {

    /**
     * The implementation specific queue
     */
    public static final SetQueue IMP = new SetQueue();

    public static enum QueueStage {
        INACTIVE, ACTIVE, NONE;
    }

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
                    lastSuccess = System.currentTimeMillis();
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
                if (Thread.currentThread() != Fawe.get().getMainThread()) {
                    throw new IllegalStateException("This shouldn't be possible for placement to occur off the main thread");
                }
                // Disable the async catcher as it can't discern async vs parallel
                SET_TASK.value2.startSet(true);
                try {
                    if (Settings.PARALLEL_THREADS <= 1) {
                        SET_TASK.run();
                    } else {
                        ArrayList<Thread> threads = new ArrayList<Thread>();
                        for (int i = 0; i < Settings.PARALLEL_THREADS; i++) {
                            threads.add(new Thread(SET_TASK));
                        }
                        for (Thread thread : threads) {
                            thread.start();
                        }
                        for (Thread thread : threads) {
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                MainUtil.handleError(e);
                            }
                        }
                    }
                } catch (Throwable e) {
                    MainUtil.handleError(e);
                } finally {
                    // Enable it again (note that we are still on the main thread)
                    SET_TASK.value2.endSet(true);
                }
            }
        }, 1);
    }

    public QueueStage getStage(FaweQueue queue) {
        if (activeQueues.contains(queue)) {
            return QueueStage.ACTIVE;
        } else if (inactiveQueues.contains(queue)) {
            return QueueStage.INACTIVE;
        }
        return QueueStage.NONE;
    }

    public boolean isStage(FaweQueue queue, QueueStage stage) {
        switch (stage) {
            case ACTIVE:
                return activeQueues.contains(queue);
            case INACTIVE:
                return inactiveQueues.contains(queue);
            case NONE:
                return !activeQueues.contains(queue) && !inactiveQueues.contains(queue);
        }
        return false;
    }

    public void enqueue(FaweQueue queue) {
        inactiveQueues.remove(queue);
        if (queue.size() > 0 && !activeQueues.contains(queue)) {
            queue.optimize();
            activeQueues.add(queue);
        }
    }

    public void dequeue(FaweQueue queue) {
        inactiveQueues.remove(queue);
        activeQueues.remove(queue);
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

    public FaweQueue getNewQueue(String world, boolean fast, boolean autoqueue) {
        FaweQueue queue = Fawe.imp().getNewQueue(world, fast);
        if (autoqueue) {
            inactiveQueues.add(queue);
        }
        return queue;
    }

    public FaweQueue getNextQueue() {
        while (activeQueues.size() > 0) {
            FaweQueue queue = activeQueues.peek();
            if (queue != null && queue.size() > 0) {
                queue.modified = System.currentTimeMillis();
                return queue;
            } else {
                activeQueues.poll();
            }
        }
        if (inactiveQueues.size() > 0) {
            ArrayList<FaweQueue> tmp = new ArrayList<>(inactiveQueues);
            if (Settings.QUEUE_MAX_WAIT >= 0) {
                long now = System.currentTimeMillis();
                if (lastSuccess != 0) {
                    for (FaweQueue queue : tmp) {
                        if (queue != null && queue.size() > 0 && now - queue.modified > Settings.QUEUE_MAX_WAIT) {
                            queue.modified = now;
                            return queue;
                        } else if (now - queue.modified > Settings.QUEUE_DISCARD_AFTER) {
                            inactiveQueues.remove(queue);
                        }
                    }
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
                            queue.modified = System.currentTimeMillis();
                            return queue;
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

package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal2;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SetQueue {

    /**
     * The implementation specific queue
     */
    public static final SetQueue IMP = new SetQueue();

    public enum QueueStage {
        INACTIVE, ACTIVE, NONE;
    }

    private final ConcurrentLinkedDeque<FaweQueue> activeQueues;
    private final ConcurrentLinkedDeque<FaweQueue> inactiveQueues;
    private final ConcurrentLinkedDeque<Runnable> tasks;

    /**
     * Used to calculate elapsed time in milliseconds and ensure block placement doesn't lag the server
     */
    private long last;
    private long secondLast;
    private long lastSuccess;

    /**
     * A queue of tasks that will run when the queue is empty
     */
    private final ConcurrentLinkedDeque<Runnable> emptyTasks = new ConcurrentLinkedDeque<>();

    private final RunnableVal2<Long, FaweQueue> SET_TASK = new RunnableVal2<Long, FaweQueue>() {
        @Override
        public void run(Long free, FaweQueue queue) {
            do {
                final boolean current = queue.next();
                if (current == false) {
                    lastSuccess = last;
                    if (inactiveQueues.size() == 0 && activeQueues.size() == 0) {
                        runEmptyTasks();
                    }
                    return;
                }
            } while (((SetQueue.this.secondLast = System.currentTimeMillis()) - SetQueue.this.last) < free);
        }
    };

    public SetQueue() {
        tasks = new ConcurrentLinkedDeque<>();
        activeQueues = new ConcurrentLinkedDeque();
        inactiveQueues = new ConcurrentLinkedDeque<>();
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                while (!tasks.isEmpty() && Fawe.get().getTimer().isAbove(18.5)) {
                    tasks.poll().run();
                }
                if (inactiveQueues.isEmpty() && activeQueues.isEmpty()) {
                    lastSuccess = System.currentTimeMillis();
                    runEmptyTasks();
                    return;
                }
                if (!MemUtil.isMemoryFree()) {
                    final int mem = MemUtil.calculateMemory();
                    if (mem != Integer.MAX_VALUE) {
                        if ((mem <= 1) && Settings.CRASH_MITIGATION) {
                            for (FaweQueue queue : getAllQueues()) {
                                queue.saveMemory();
                            }
                            return;
                        }
                        if (SetQueue.this.forceChunkSet()) {
                            System.gc();
                        } else {
                            SetQueue.this.runEmptyTasks();
                        }
                        return;
                    }
                }
                SET_TASK.value1 = Settings.QUEUE.EXTRA_TIME_MS + 50 + Math.min((50 + SetQueue.this.last) - (SetQueue.this.last = System.currentTimeMillis()), SetQueue.this.secondLast - System.currentTimeMillis());
                SET_TASK.value2 = getNextQueue();
                if (SET_TASK.value2 == null) {
                    return;
                }
                if (Thread.currentThread() != Fawe.get().getMainThread()) {
                    throw new IllegalStateException("This shouldn't be possible for placement to occur off the main thread");
                }
                // Disable the async catcher as it can't discern async vs parallel
                boolean parallel = Settings.QUEUE.PARALLEL_THREADS > 1;
                SET_TASK.value2.startSet(parallel);
                try {
                    if (Settings.QUEUE.PARALLEL_THREADS <= 1) {
                        SET_TASK.run();
                    } else {
                        Thread[] threads = new Thread[Settings.QUEUE.PARALLEL_THREADS];
                        for (int i = 0; i < Settings.QUEUE.PARALLEL_THREADS; i++) {
                            threads[i] = (new Thread(SET_TASK));
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
                    SET_TASK.value2.endSet(parallel);
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

    public Collection<FaweQueue> getAllQueues() {
        ArrayList<FaweQueue> list = new ArrayList<FaweQueue>(activeQueues.size() + inactiveQueues.size());
        list.addAll(inactiveQueues);
        list.addAll(activeQueues);
        return list;
    }

    public Collection<FaweQueue> getActiveQueues() {
        return activeQueues;
    }

    public Collection<FaweQueue> getInactiveQueues() {
        return inactiveQueues;
    }

    public FaweQueue getNewQueue(String world, boolean fast, boolean autoqueue) {
        FaweQueue queue = Fawe.imp().getNewQueue(world, fast);
        if (autoqueue) {
            inactiveQueues.add(queue);
        }
        return queue;
    }

    public void flush(FaweQueue queue) {
        SET_TASK.value1 = Long.MAX_VALUE;
        SET_TASK.value2 = queue;
        if (SET_TASK.value2 == null) {
            return;
        }
        if (Thread.currentThread() != Fawe.get().getMainThread()) {
            throw new IllegalStateException("Must be flushed on the main thread!");
        }
        // Disable the async catcher as it can't discern async vs parallel
        boolean parallel = Settings.QUEUE.PARALLEL_THREADS > 1;
        SET_TASK.value2.startSet(parallel);
        try {
            if (!parallel) {
                SET_TASK.run();
            } else {
                Thread[] threads = new Thread[Settings.QUEUE.PARALLEL_THREADS];
                for (int i = 0; i < Settings.QUEUE.PARALLEL_THREADS; i++) {
                    threads[i] = (new Thread(SET_TASK));
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
            SET_TASK.value2.endSet(parallel);
            dequeue(queue);
        }
    }

    public FaweQueue getNextQueue() {
        long now = System.currentTimeMillis();
        while (activeQueues.size() > 0) {
            FaweQueue queue = activeQueues.peek();
            if (queue != null && queue.size() > 0) {
                queue.setModified(now);
                return queue;
            } else {
                activeQueues.poll();
            }
        }
        int size = inactiveQueues.size();
        if (size > 0) {
            Iterator<FaweQueue> iter = inactiveQueues.iterator();
            try {
                int total = 0;
                FaweQueue firstNonEmpty = null;
                while (iter.hasNext()) {
                    FaweQueue queue = iter.next();
                    long age = now - queue.getModified();
                    total += queue.size();
                    if (queue.size() == 0) {
                        if (age > Settings.QUEUE.DISCARD_AFTER_MS) {
                            iter.remove();
                        }
                        continue;
                    }
                    if (firstNonEmpty == null) {
                        firstNonEmpty = queue;
                    }
                    if (total > Settings.QUEUE.TARGET_SIZE) {
                        firstNonEmpty.setModified(now);
                        return firstNonEmpty;
                    }
                    if (age > Settings.QUEUE.MAX_WAIT_MS) {
                        queue.setModified(now);
                        return queue;
                    }
                }
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean next() {
        while (activeQueues.size() > 0) {
            FaweQueue queue = activeQueues.poll();
            if (queue != null) {
                final boolean set = queue.next();
                if (set) {
                    activeQueues.add(queue);
                    return set;
                }
            }
        }
        if (inactiveQueues.size() > 0) {
            ArrayList<FaweQueue> tmp = new ArrayList<>(inactiveQueues);
            if (Settings.QUEUE.MAX_WAIT_MS != -1) {
                long now = System.currentTimeMillis();
                if (lastSuccess == 0) {
                    lastSuccess = now;
                }
                long diff = now - lastSuccess;
                if (diff > Settings.QUEUE.MAX_WAIT_MS) {
                    for (FaweQueue queue : tmp) {
                        boolean result = queue.next();
                        if (result) {
                            return result;
                        }
                    }
                    if (diff > Settings.QUEUE.DISCARD_AFTER_MS) {
                        // These edits never finished
                        inactiveQueues.clear();
                    }
                    return false;
                }
            }
            if (Settings.QUEUE.TARGET_SIZE != -1) {
                int total = 0;
                for (FaweQueue queue : tmp) {
                    total += queue.size();
                }
                if (total > Settings.QUEUE.TARGET_SIZE) {
                    for (FaweQueue queue : tmp) {
                        boolean result = queue.next();
                        if (result) {
                            return result;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean forceChunkSet() {
        return next();
    }

    /**
     * Is the this empty
     * @return
     */
    public boolean isEmpty() {
        return activeQueues.size() == 0 && inactiveQueues.size() == 0;
    }

    public void addTask(Runnable whenFree) {
        tasks.add(whenFree);
    }

    /**
     * Add a task to run when it is empty
     * @param whenDone
     * @return
     */
    public boolean addEmptyTask(final Runnable whenDone) {
        if (this.isEmpty()) {
            // Run
            this.runEmptyTasks();
            if (whenDone != null) {
                whenDone.run();
            }
            return true;
        }
        if (whenDone != null) {
            this.emptyTasks.add(whenDone);
        }
        return false;
    }

    private synchronized boolean runEmptyTasks() {
        if (this.emptyTasks.isEmpty()) {
            return false;
        }
        final ConcurrentLinkedDeque<Runnable> tmp = new ConcurrentLinkedDeque<>(this.emptyTasks);
        this.emptyTasks.clear();
        for (final Runnable runnable : tmp) {
            runnable.run();
        }
        return true;
    }
}

package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.RunnableVal;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class TaskManager {

    public static TaskManager IMP;

    /**
     * Run a repeating task on the main thread
     * @param r
     * @param interval in ticks
     * @return
     */
    public abstract int repeat(final Runnable r, final int interval);

    /**
     * Run a repeating task asynchronously
     * @param r
     * @param interval in ticks
     * @return
     */
    public abstract int repeatAsync(final Runnable r, final int interval);

    /**
     * Run a task asynchronously
     * @param r
     */
    public abstract void async(final Runnable r);

    /**
     * Run a task on the main thread
     * @param r
     */
    public abstract void task(final Runnable r);

    /**
     * Run a task on the current thread or asynchronously
     *  - If it's already the main thread, it will jst call run()
     * @param r
     * @param async
     */
    public void taskNow(final Runnable r, boolean async) {
        if (async) {
            async(r);
        } else {
            r.run();
        }
    }

    /**
     * Run a task as soon as possible on the main thread, or now async
     * @param r
     * @param async
     */
    public void taskSyncNow(final Runnable r, boolean async) {
        if (async) {
            async(r);
        } else if (r != null && Thread.currentThread() == Fawe.get().getMainThread()){
            r.run();
        } else {
            task(r);
        }
    }

    /**
     * Run a task on the main thread at the next tick or now async
     * @param r
     * @param async
     */
    public void taskSyncSoon(final Runnable r, boolean async) {
        if (async) {
            async(r);
        } else {
            task(r);
        }
    }


    /**
     * Run a task later on the main thread
     * @param r
     * @param delay in ticks
     */
    public abstract void later(final Runnable r, final int delay);

    /**
     * Run a task later asynchronously
     * @param r
     * @param delay in ticks
     */
    public abstract void laterAsync(final Runnable r, final int delay);

    /**
     * Cancel a task
     * @param task
     */
    public abstract void cancel(final int task);

    /**
     * Break up a task and run it in fragments of 5ms.<br>
     *     - Each task will run on the main thread.<br>
     * @param objects - The list of objects to run the task for
     * @param task - The task to run on each object
     * @param whenDone - When the object task completes
     * @param <T>
     */
    public <T> void objectTask(Collection<T> objects, final RunnableVal<T> task, final Runnable whenDone) {
        final Iterator<T> iterator = objects.iterator();
        task(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                boolean hasNext;
                while ((hasNext = iterator.hasNext()) && System.currentTimeMillis() - start < 5) {
                    task.value = iterator.next();
                    task.run();
                }
                if (!hasNext) {
                    later(whenDone, 1);
                } else {
                    later(this, 1);
                }
            }
        });
    }

    /**
     * Quickly run a task on the main thread, and wait for execution to finish:<br>
     *     - Useful if you need to access something from the Bukkit API from another thread<br>
     *     - Usualy wait time is around 25ms<br>
     * @param function
     * @param <T>
     * @return
     */
    public <T> T sync(final RunnableVal<T> function) {
       return sync(function, Integer.MAX_VALUE);
    }

    /**
     * Quickly run a task on the main thread, and wait for execution to finish:<br>
     *     - Useful if you need to access something from the Bukkit API from another thread<br>
     *     - Usualy wait time is around 25ms<br>
     * @param function
     * @param timeout - How long to wait for execution
     * @param <T>
     * @return
     */
    public <T> T sync(final RunnableVal<T> function, int timeout) {
        if (Fawe.get().getMainThread() == Thread.currentThread()) {
            function.run();
            return function.value;
        }
        final AtomicBoolean running = new AtomicBoolean(true);
        RunnableVal<RuntimeException> run = new RunnableVal<RuntimeException>() {
            @Override
            public void run(RuntimeException value) {
                try {
                    function.run();
                } catch (RuntimeException e) {
                    this.value = e;
                } catch (Throwable neverHappens) {
                    neverHappens.printStackTrace();
                } finally {
                    running.set(false);
                }
                synchronized (function) {
                    function.notifyAll();
                }
            }
        };
        TaskManager.IMP.task(run);
        try {
            synchronized (function) {
                while (running.get()) {
                    function.wait(timeout);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (run.value != null) {
            throw run.value;
        }
        return function.value;
    }
}

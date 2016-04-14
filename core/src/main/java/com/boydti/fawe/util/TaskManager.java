package com.boydti.fawe.util;

import com.boydti.fawe.object.RunnableVal;
import java.util.Collection;
import java.util.Iterator;

public abstract class TaskManager {

    public static TaskManager IMP;

    public abstract int repeat(final Runnable r, final int interval);

    public abstract int repeatAsync(final Runnable r, final int interval);

    public abstract void async(final Runnable r);

    public abstract void task(final Runnable r);

    public abstract void later(final Runnable r, final int delay);

    public abstract void laterAsync(final Runnable r, final int delay);

    public abstract void cancel(final int task);

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
}

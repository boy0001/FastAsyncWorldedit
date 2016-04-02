package com.boydti.fawe.util;

public abstract class TaskManager {

    public static TaskManager IMP;

    public abstract int repeat(final Runnable r, final int interval);

    public abstract int repeatAsync(final Runnable r, final int interval);

    public abstract void async(final Runnable r);

    public abstract void task(final Runnable r);

    public abstract void later(final Runnable r, final int delay);

    public abstract void laterAsync(final Runnable r, final int delay);

    public abstract void cancel(final int task);
}

package com.boydti.fawe.util;

import com.boydti.fawe.config.Settings;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemUtil {

    private static AtomicBoolean memory = new AtomicBoolean(false);

    public static boolean isMemoryFree() {
        return !memory.get();
    }

    public static boolean isMemoryLimited() {
        return memory.get();
    }

    public static int calculateMemory() {
        final long heapSize = Runtime.getRuntime().totalMemory();
        final long heapMaxSize = Runtime.getRuntime().maxMemory();
        if (heapSize < heapMaxSize) {
            return Integer.MAX_VALUE;
        }
        final long heapFreeSize = Runtime.getRuntime().freeMemory();
        final int size = (int) ((heapFreeSize * 100) / heapMaxSize);
        if (size > (100 - Settings.MEM_FREE)) {
            memoryPlentifulTask();
            return Integer.MAX_VALUE;
        }
        return size;
    }

    private static BlockingQueue<Runnable> memoryLimitedTasks = new LinkedBlockingQueue<>();
    private static BlockingQueue<Runnable> memoryPlentifulTasks = new LinkedBlockingQueue<>();

    public static void addMemoryLimitedTask(Runnable run) {
        if (run != null)
            memoryLimitedTasks.add(run);
    }

    public static void addMemoryPlentifulTask(Runnable run) {
        if (run != null)
            memoryPlentifulTasks.add(run);
    }

    public static void memoryLimitedTask() {
        for (Runnable task : memoryLimitedTasks) {
            task.run();
        }
        memory.set(true);
    }

    public static void memoryPlentifulTask() {
        for (Runnable task : memoryPlentifulTasks) {
            task.run();
        }
        memory.set(false);
    }
}

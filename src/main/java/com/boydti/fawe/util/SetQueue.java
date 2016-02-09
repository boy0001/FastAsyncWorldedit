package com.boydti.fawe.util;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class SetQueue {
    
    public static final SetQueue IMP = new SetQueue();
    
    public FaweQueue queue;
    
    private final AtomicInteger time_waiting = new AtomicInteger(2);
    private final AtomicInteger time_current = new AtomicInteger(0);
    private final ArrayDeque<Runnable> runnables = new ArrayDeque<>();
    private long last;
    private long last2;

    public SetQueue() {
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                if (!MemUtil.isMemoryFree()) {
                    final int mem = MemUtil.calculateMemory();
                    if (mem != Integer.MAX_VALUE) {
                        if (mem <= 1 && Settings.ENABLE_HARD_LIMIT) {
                            queue.saveMemory();
                            return;
                        }
                        if (forceChunkSet()) {
                            System.gc();
                        } else {
                            time_current.incrementAndGet();
                            tasks();
                        }
                        return;
                    }
                }
                long free = 50 + Math.min(50 + last - (last = System.currentTimeMillis()), last2 - System.currentTimeMillis());
                time_current.incrementAndGet();
                do {
                    if (isWaiting()) {
                        return;
                    }
                    final FaweChunk<?> current = queue.next();
                    if (current == null) {
                        time_waiting.set(Math.max(time_waiting.get(), time_current.get() - 2));
                        tasks();
                        return;
                    }
                } while ((last2 = System.currentTimeMillis()) - last < free);
                time_waiting.set(time_current.get() - 1);
            }
        }, 1);
    }
    
    public boolean forceChunkSet() {
        final FaweChunk<?> set = queue.next();
        return set != null;
    }
    
    public boolean isWaiting() {
        return time_waiting.get() >= time_current.get();
    }
    
    public boolean isDone() {
        return (time_waiting.get() + 1) < time_current.get();
    }
    
    public void setWaiting() {
        time_waiting.set(time_current.get() + 1);
    }
    
    public boolean addTask(final Runnable whenDone) {
        if (isDone()) {
            // Run
            tasks();
            if (whenDone != null) {
                whenDone.run();
            }
            return true;
        }
        if (whenDone != null) {
            runnables.add(whenDone);
        }
        return false;
    }
    
    public boolean tasks() {
        if (runnables.size() == 0) {
            return false;
        }
        final ArrayDeque<Runnable> tmp = runnables.clone();
        runnables.clear();
        for (final Runnable runnable : tmp) {
            runnable.run();
        }
        return true;
    }
    
    /**
     * @param world
     * @param x
     * @param y
     * @param z
     * @param id
     * @param data
     * @return
     */
    public boolean setBlock(final String world, final int x, final int y, final int z, final short id, final byte data) {
        SetQueue.IMP.setWaiting();
        return queue.setBlock(world, x, y, z, id, data);
    }
    
    /**
     * @param world
     * @param x
     * @param y
     * @param z
     * @param id
     * @return
     */
    public boolean setBlock(final String world, final int x, final int y, final int z, final short id) {
        SetQueue.IMP.setWaiting();
        return queue.setBlock(world, x, y, z, id, (byte) 0);
    }
    
    /**
     * @param world
     * @param x
     * @param y
     * @param z
     * @param id
     * @param data
     * @return
     */
    public boolean setBiome(final String world, final int x, final int z, BaseBiome biome) {
        SetQueue.IMP.setWaiting();
        return queue.setBiome(world, x, z, biome);
    }
    
    public boolean isChunkLoaded(String world, int x, int z) {
        return queue.isChunkLoaded(world, x, z);
    }
}

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
                        if ((mem <= 1) && Settings.ENABLE_HARD_LIMIT) {
                            SetQueue.this.queue.saveMemory();
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
                final long free = 50 + Math.min((50 + SetQueue.this.last) - (SetQueue.this.last = System.currentTimeMillis()), SetQueue.this.last2 - System.currentTimeMillis());
                SetQueue.this.time_current.incrementAndGet();
                do {
                    if (SetQueue.this.isWaiting()) {
                        return;
                    }
                    final FaweChunk<?> current = SetQueue.this.queue.next();
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

    public boolean forceChunkSet() {
        final FaweChunk<?> set = this.queue.next();
        return set != null;
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
        return this.queue.setBlock(world, x, y, z, id, data);
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
        return this.queue.setBlock(world, x, y, z, id, (byte) 0);
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
    public boolean setBiome(final String world, final int x, final int z, final BaseBiome biome) {
        SetQueue.IMP.setWaiting();
        return this.queue.setBiome(world, x, z, biome);
    }

    public boolean isChunkLoaded(final String world, final int x, final int z) {
        return this.queue.isChunkLoaded(world, x, z);
    }
}

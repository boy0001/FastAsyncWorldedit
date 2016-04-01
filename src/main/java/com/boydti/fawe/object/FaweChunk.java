package com.boydti.fawe.object;

import java.util.ArrayDeque;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.worldedit.world.biome.BaseBiome;

public abstract class FaweChunk<T> {

    private ChunkLoc chunk;

    private final ArrayDeque<Runnable> tasks = new ArrayDeque<Runnable>();

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     */
    public FaweChunk(final ChunkLoc chunk) {
        this.chunk = chunk;
    }

    public void setChunkLoc(final ChunkLoc loc) {
        this.chunk = loc;
    }

    public ChunkLoc getChunkLoc() {
        return this.chunk;
    }

    public void addToQueue() {
        if (this.chunk == null) {
            throw new IllegalArgumentException("Chunk location cannot be null!");
        }
        SetQueue.IMP.queue.setChunk(this);
    }

    public void fixLighting() {
        SetQueue.IMP.queue.fixLighting(this, Settings.FIX_ALL_LIGHTING);
    }

    public void fill(final int id, final byte data) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    this.setBlock(x, y, z, id, data);
                }
            }
        }
    }

    public void addTask(Runnable run) {
        if (run != null) {
            tasks.add(run);
        }
    }
    
    public void executeTasks() {
        for (Runnable task : tasks) {
            task.run();
        }
        tasks.clear();
    }

    public abstract T getChunk();

    public abstract void setBlock(final int x, final int y, final int z, final int id, final byte data);

    public abstract void setBiome(final int x, final int z, final BaseBiome biome);

    @Override
    public int hashCode() {
        return this.chunk.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if ((obj == null) || !(obj instanceof FaweChunk)) {
            return false;
        }
        return this.chunk.equals(((FaweChunk) obj).chunk);
    }
}

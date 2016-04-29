package com.boydti.fawe.object;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.FaweQueue;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayDeque;

public abstract class FaweChunk<T> {

    private FaweQueue parent;
    private int x,z;

    private final ArrayDeque<Runnable> tasks = new ArrayDeque<Runnable>();

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     */
    public FaweChunk(FaweQueue parent, int x, int z) {
        this.parent = parent;
        this.x = x;
        this.z = z;
    }

    public void setLoc(FaweQueue parent, int x, int z) {
        this.parent = parent;
        this.x = x;
        this.z = z;
    }

    public FaweQueue getParent() {
        return parent;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public long longHash() {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    @Override
    public int hashCode() {
        return x << 16 | z & 0xFFFF;
    }

    public void addToQueue() {
        parent.setChunk(this);
    }

    public void fixLighting() {
        parent.fixLighting(this, Settings.FIX_ALL_LIGHTING);
    }

    /**
     * Fill this chunk with a block
     * @param id
     * @param data
     */
    public void fill(int id, byte data) {
        fillCuboid(0, 15, 0, 255, 0, 15, id, data);
    }

    /**
     * Fill a cuboid in this chunk with a block
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @param z1
     * @param z2
     * @param id
     * @param data
     */
    public void fillCuboid(int x1, int x2, int y1, int y2, int z1, int z2, int id, byte data) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    setBlock(x, y, z, id, data);
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

    public void optimize() {}

    @Override
    public boolean equals(final Object obj) {
        if ((obj == null) || obj.hashCode() != hashCode() || !(obj instanceof FaweChunk)) {
            return false;
        }
        return longHash() != ((FaweChunk) obj).longHash();
    }

    public abstract FaweChunk<T> copy(boolean shallow);
}

package com.boydti.fawe.object;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    /**
     * Change the chunk's location<br>
     *     - E.g. if you are cloning a chunk and want to set multiple
     * @param parent
     * @param x
     * @param z
     */
    public void setLoc(FaweQueue parent, int x, int z) {
        this.parent = parent;
        this.x = x;
        this.z = z;
    }

    /**
     * Get the parent queue this chunk belongs to
     * @return
     */
    public FaweQueue getParent() {
        return parent;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    /**
     * Get a unique hashcode for this chunk
     * @return
     */
    public long longHash() {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    /**
     * Get a hashcode; unique below abs(x/z) < Short.MAX_VALUE
     * @return
     */
    @Override
    public int hashCode() {
        return x << 16 | z & 0xFFFF;
    }

    /**
     * Add the chunk to the queue
     */
    public void addToQueue() {
        parent.setChunk(this);
    }

    /**
     * Fix the lighting in this chunk
     */
    public void fixLighting() {
        parent.fixLighting(this, Settings.LIGHTING.FIX_ALL ? FaweQueue.RelightMode.OPTIMAL : FaweQueue.RelightMode.MINIMAL);
    }

    /**
     * This may return the raw value or constructed depending on the implementation<br>
     *     - The first index (i) is the layer (layer = y >> 4) (16 layers)<br>
     *     - The second array is length 4096 and contains the combined ids (cast to an int if you want)
     *
     * @see com.boydti.fawe.FaweCache#CACHE_I
     * @see com.boydti.fawe.FaweCache#CACHE_J
     * @see com.boydti.fawe.FaweCache#CACHE_X
     * @see com.boydti.fawe.FaweCache#CACHE_Y
     * @see com.boydti.fawe.FaweCache#CACHE_Z
     *
     * @return Combined id arrays
     */
    public abstract char[][] getCombinedIdArrays();

    /**
     * Get the combined block id at a location<br>
     * combined = (id <<<< 4) + data
     * @param x
     * @param y
     * @param z
     * @return The combined id
     */
    public int getBlockCombinedId(int x, int y, int z) {
        char[][] arrays = getCombinedIdArrays();
        char[] array = arrays[y >> 4];
        return array != null ? (array[FaweCache.CACHE_J[y][x][z]]) : 0;
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

    /**
     * Add a task to run when this chunk is dispatched
     * @param run
     */
    public void addNotifyTask(Runnable run) {
        if (run != null) {
            tasks.add(run);
        }
    }

    public boolean hasNotifyTasks() {
        return tasks.size() > 0;
    }

    public void executeNotifyTasks() {
        for (Runnable task : tasks) {
            task.run();
        }
        tasks.clear();
    }

    /**
     * Get the underlying chunk object
     * @return
     */
    public abstract T getChunk();

    /**
     * Set a tile entity at a location<br>
     *     - May throw an error if an invalid block is at the location
     * @param x
     * @param y
     * @param z
     * @param tile
     */
    public abstract void setTile(int x, int y, int z, CompoundTag tile);

    public abstract void setEntity(CompoundTag entity);

    public abstract void removeEntity(UUID uuid);

    public abstract void setBlock(final int x, final int y, final int z, final int id, final byte data);

    public abstract Set<CompoundTag> getEntities();

    /**
     * Get the UUID of entities being removed
     * @return
     */
    public abstract Set<UUID> getEntityRemoves();

    /**
     * Get the map of location to tile entity<br>
     *     - The byte pair represents the location in the chunk<br>
     * @see com.boydti.fawe.util.MathMan#unpair16x (get0) => x
     * @see com.boydti.fawe.util.MathMan#unpair16y (get0) => z
     * get1 => y
     * @return
     */
    public abstract Map<BytePair, CompoundTag> getTiles();

    /**
     * Get the tile at a location
     * @param x
     * @param y
     * @param z
     * @return
     */
    public abstract CompoundTag getTile(int x, int y, int z);

    public abstract void setBiome(final int x, final int z, final BaseBiome biome);

    /**
     * Spend time now so that the chunk can be more efficiently dispatched later<br>
     *     - Modifications after this call will be ignored
     */
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

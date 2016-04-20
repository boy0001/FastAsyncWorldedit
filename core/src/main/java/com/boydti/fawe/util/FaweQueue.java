package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.biome.BaseBiome;

public abstract class FaweQueue {

    public final String world;

    public FaweQueue(String world) {
        this.world = world;
    }

    public abstract boolean setBlock(final int x, final int y, final int z, final short id, final byte data);

    public abstract boolean setBiome(final int x, final int z, final BaseBiome biome);

    public abstract FaweChunk<?> getChunk(int x, int z);

    public abstract void setChunk(final FaweChunk<?> chunk);

    public abstract boolean fixLighting(final FaweChunk<?> chunk, final boolean fixAll);

    public abstract boolean isChunkLoaded(final int x, final int z);

    public abstract boolean regenerateChunk(int x, int z);

    /**
     * Gets the FaweChunk and sets the requested blocks
     * @return
     */
    public abstract FaweChunk next();

    public void saveMemory() {
        MainUtil.sendAdmin(BBC.OOM.s());
        // Set memory limited
        MemUtil.memoryLimitedTask();
        // Clear block placement
        clear();
        Fawe.get().getWorldEdit().clearSessions();
        // GC
        System.gc();
        System.gc();
        // Unload chunks
    }

    /**
     * This method is called when the server is < 1% available memory
     */
    protected abstract void clear();
    
    public abstract void addTask(int x, int z, Runnable runnable);

    public abstract int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException;

    public int getCombinedId4Data(int x, int y, int z, int def) {
        try {
            return getCombinedId4Data(x, y, z);
        } catch (FaweException ignore) {
            return def;
        }
    }

    public int getCombinedId4DataDebug(int x, int y, int z, int def, EditSession session) {
        try {
            return getCombinedId4Data(x, y, z);
        } catch (FaweException ignore) {
            session.debug(BBC.WORLDEDIT_FAILED_LOAD_CHUNK.format(x >> 4, z >> 4));
            return def;
        }
    }

    public abstract int size();

    public void enqueue() {
        SetQueue.IMP.enqueue(this);
    }
}

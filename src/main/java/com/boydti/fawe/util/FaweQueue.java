package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.ChunkLoc;
import com.boydti.fawe.object.FaweChunk;
import com.sk89q.worldedit.world.biome.BaseBiome;

public abstract class FaweQueue {
    public abstract boolean setBlock(final String world, final int x, final int y, final int z, final short id, final byte data);

    public abstract boolean setBiome(final String world, final int x, final int z, final BaseBiome biome);

    public abstract FaweChunk<?> getChunk(final ChunkLoc wrap);

    public abstract void setChunk(final FaweChunk<?> chunk);

    public abstract boolean fixLighting(final FaweChunk<?> chunk, final boolean fixAll);

    public abstract boolean isChunkLoaded(final String world, final int x, final int z);

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
        SetQueue.IMP.queue.clear();
        Fawe.get().getWorldEdit().clearSessions();
        // GC
        System.gc();
        System.gc();
        // Unload chunks
    }

    protected abstract void clear();
}

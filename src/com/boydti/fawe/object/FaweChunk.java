package com.boydti.fawe.object;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.SetBlockQueue;
import com.sk89q.worldedit.world.biome.BaseBiome;

public abstract class FaweChunk<T> {
    
    private ChunkLoc chunk;
    
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
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk location cannot be null!");
        }
        SetBlockQueue.IMP.queue.setChunk(this);
    }
    
    public void fixLighting() {
        SetBlockQueue.IMP.queue.fixLighting(this, Settings.FIX_ALL_LIGHTING);
    }

    public void fill(int id, byte data) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    setBlock(x, y, z, id, data);
                }
            }
        }
    }

    public abstract T getChunk();
    
    public abstract void setBlock(final int x, final int y, final int z, final int id, final byte data);
    
    public abstract void setBiome(int x, int z, BaseBiome biome);
    
    @Override
    public int hashCode() {
        return chunk.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FaweChunk)) {
            return false;
        }
        return chunk.equals(((FaweChunk) obj).chunk);
    }
}

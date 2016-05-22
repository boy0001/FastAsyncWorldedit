package com.boydti.fawe.forge.v0;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweQueue;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class ForgeChunk_All extends CharFaweChunk<Chunk> {
    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public ForgeChunk_All(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public Chunk getNewChunk() {
        World world = ((ForgeQueue_All) getParent()).getWorld();
        return world.getChunkProvider().provideChunk(getX(), getZ());
    }
}

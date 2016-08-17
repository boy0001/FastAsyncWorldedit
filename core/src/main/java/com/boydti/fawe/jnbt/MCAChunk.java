package com.boydti.fawe.jnbt;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweQueue;

public class MCAChunk extends CharFaweChunk<Void> {
    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public MCAChunk(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public Void getNewChunk() {
        return null;
    }
}

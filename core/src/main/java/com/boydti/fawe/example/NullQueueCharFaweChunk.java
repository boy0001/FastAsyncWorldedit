package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.MainUtil;

public class NullQueueCharFaweChunk extends CharFaweChunk {

    public NullQueueCharFaweChunk(int cx, int cz) {
        super(null, cx, cz);
    }

    public NullQueueCharFaweChunk(int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(null, x, z, ids, count, air, heightMap);
    }

    @Override
    public Object getNewChunk() {
        return null;
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        if (shallow) {
            return new NullQueueCharFaweChunk(getX(), getZ(), ids, count, air, heightMap);
        } else {
            return new NullQueueCharFaweChunk(getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
        }
    }

    @Override
    public FaweChunk call() {
        return null;
    }
}

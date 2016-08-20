package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MainUtil;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DefaultFaweQueueMap implements IFaweQueueMap {

    private final MappedFaweQueue parent;

    public DefaultFaweQueueMap(MappedFaweQueue parent) {
        this.parent = parent;
    }

    /**
     * Map of chunks in the queue
     */
    public ConcurrentHashMap<Long, FaweChunk> blocks = new ConcurrentHashMap<>();
    public ConcurrentLinkedDeque<FaweChunk> chunks = new ConcurrentLinkedDeque<FaweChunk>() {
        @Override
        public boolean add(FaweChunk o) {
            if (parent.getProgressTask() != null) {
                parent.getProgressTask().run(FaweQueue.ProgressType.QUEUE, size() + 1);
            }
            return super.add(o);
        }
    };

    @Override
    public Collection<FaweChunk> getFaweCunks() {
        return new HashSet<>(chunks);
    }

    @Override
    public void forEachChunk(RunnableVal<FaweChunk> onEach) {
        for (FaweChunk chunk : chunks) {
            onEach.run(chunk);
        }
    }

    @Override
    public FaweChunk getFaweChunk(int cx, int cz) {
        if (cx == lastX && cz == lastZ) {
            return lastWrappedChunk;
        }
        long pair = (long) (lastX = cx) << 32 | (lastX = cz) & 0xFFFFFFFFL;
        FaweChunk chunk = this.blocks.get(pair);
        if (chunk == null) {
            chunk = this.getNewFaweChunk(cx, cz);
            FaweChunk previous = this.blocks.put(pair, chunk);
            if (previous != null) {
                blocks.put(pair, previous);
                return previous;
            }
            this.blocks.put(pair, previous);
            chunks.add(previous);
        }
        return chunk;
    }

    @Override
    public void add(FaweChunk chunk) {
        long pair = (long) (chunk.getX()) << 32 | (chunk.getZ()) & 0xFFFFFFFFL;
        FaweChunk previous = this.blocks.put(pair, chunk);
        if (previous == null) {
            chunks.add(chunk);
        } else {
            blocks.put(pair, previous);
        }
    }


    @Override
    public void clear() {
        blocks.clear();
        chunks.clear();
    }

    @Override
    public int size() {
        return chunks.size();
    }

    private FaweChunk getNewFaweChunk(int cx, int cz) {
        return parent.getFaweChunk(cx, cz);
    }

    private FaweChunk lastWrappedChunk;
    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;

    @Override
    public boolean next() {
        lastX = Integer.MIN_VALUE;
        lastZ = Integer.MIN_VALUE;
        try {
            if (this.blocks.size() == 0) {
                return false;
            }
            synchronized (blocks) {
                FaweChunk chunk = chunks.poll();
                if (chunk != null) {
                    blocks.remove(chunk.longHash());
                    parent.execute(chunk);
                    return true;
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return false;
    }
}

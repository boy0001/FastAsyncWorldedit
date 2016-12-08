package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;

public class DefaultFaweQueueMap implements IFaweQueueMap {

    private final MappedFaweQueue parent;

    public DefaultFaweQueueMap(MappedFaweQueue parent) {
        this.parent = parent;
    }

    /**
     * Map of chunks in the queue
     */
    public final ConcurrentHashMap<Long, FaweChunk> blocks = new ConcurrentHashMap<Long, FaweChunk>(8, 0.9f, 1) {
        @Override
        public FaweChunk put(Long key, FaweChunk value) {
            if (parent.getProgressTask() != null) {
                try {
                    parent.getProgressTask().run(FaweQueue.ProgressType.QUEUE, size() + 1);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            return super.put(key, value);
        }
    };

    @Override
    public Collection<FaweChunk> getFaweCunks() {
        return new HashSet<>(blocks.values());
    }

    @Override
    public void forEachChunk(RunnableVal<FaweChunk> onEach) {
        for (Map.Entry<Long, FaweChunk> entry : blocks.entrySet()) {
            onEach.run(entry.getValue());
        }
    }

    @Override
    public FaweChunk getFaweChunk(int cx, int cz) {
        if (cx == lastX && cz == lastZ) {
            return lastWrappedChunk;
        }
        long pair = MathMan.pairInt(cx, cz);
        FaweChunk chunk = this.blocks.get(pair);
        if (chunk == null) {
            chunk = this.getNewFaweChunk(cx, cz);
            FaweChunk previous = this.blocks.put(pair, chunk);
            if (previous != null) {
                blocks.put(pair, previous);
                return previous;
            }
            this.blocks.put(pair, chunk);
        }
        return chunk;
    }

    @Override
    public FaweChunk getCachedFaweChunk(int cx, int cz) {
        if (cx == lastX && cz == lastZ) {
            return lastWrappedChunk;
        }
        long pair = MathMan.pairInt(cx, cz);
        FaweChunk chunk = this.blocks.get(pair);
        lastWrappedChunk = chunk;
        return chunk;
    }

    @Override
    public void add(FaweChunk chunk) {
        long pair = MathMan.pairInt(chunk.getX(), chunk.getZ());
        FaweChunk previous = this.blocks.put(pair, chunk);
        if (previous != null) {
            blocks.put(pair, previous);
        }
    }


    @Override
    public void clear() {
        blocks.clear();
    }

    @Override
    public int size() {
        return blocks.size();
    }

    private FaweChunk getNewFaweChunk(int cx, int cz) {
        return parent.getFaweChunk(cx, cz);
    }

    private volatile FaweChunk lastWrappedChunk;
    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;

    @Override
    public boolean next(int amount, ExecutorCompletionService pool, long time) {
        try {
            boolean skip = parent.getStage() == SetQueue.QueueStage.INACTIVE;
            int added = 0;
            Iterator<Map.Entry<Long, FaweChunk>> iter = blocks.entrySet().iterator();
            if (amount == 1) {
                long start = System.currentTimeMillis();
                do {
                    if (iter.hasNext()) {
                        FaweChunk chunk = iter.next().getValue();
                        if (skip && chunk == lastWrappedChunk) {
                            continue;
                        }
                        iter.remove();
                        parent.start(chunk);
                        chunk.call();
                        parent.end(chunk);
                    } else {
                        break;
                    }
                } while (System.currentTimeMillis() - start < time);
            } else {
                boolean result = true;
                // amount = 8;
                for (int i = 0; i < amount && (result = iter.hasNext()); i++, added++) {
                    Map.Entry<Long, FaweChunk> item = iter.next();
                    FaweChunk chunk = item.getValue();
                    if (skip && chunk == lastWrappedChunk) {
                        i--;
                        added--;
                        continue;
                    }
                    iter.remove();
                    parent.start(chunk);
                    pool.submit(chunk);
                }
                // if result, then submitted = amount
                if (result) {
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < time && result) {
                        if (result = iter.hasNext()) {
                            Map.Entry<Long, FaweChunk> item = iter.next();
                            FaweChunk chunk = item.getValue();
                            if (skip && chunk == lastWrappedChunk) {
                                continue;
                            }
                            iter.remove();
                            parent.start(chunk);
                            pool.submit(chunk);
                            FaweChunk fc = ((FaweChunk) pool.take().get());
                            parent.end(fc);
                        }
                    }
                }
                for (int i = 0; i < added; i++) {
                    FaweChunk fc = ((FaweChunk) pool.take().get());
                    parent.end(fc);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return !blocks.isEmpty();
    }
}

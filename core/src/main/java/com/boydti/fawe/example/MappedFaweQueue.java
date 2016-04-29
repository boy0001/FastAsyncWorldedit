package com.boydti.fawe.example;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public abstract class MappedFaweQueue<WORLD, CHUNK, SECTION> extends FaweQueue {

    private WORLD impWorld;

    /**
     * Map of chunks in the queue
     */
    private ConcurrentHashMap<Long, FaweChunk> blocks = new ConcurrentHashMap<>();
    private LinkedBlockingDeque<FaweChunk> chunks = new LinkedBlockingDeque<>();

    @Override
    public void optimize() {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (final FaweChunk chunk : chunks) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    chunk.optimize();
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public MappedFaweQueue(final String world) {
        super(world);
    }

    public abstract WORLD getWorld(String world);

    public abstract boolean isChunkLoaded(WORLD world, int x, int z);

    public abstract boolean regenerateChunk(WORLD world, int x, int z);

    public abstract void sendChunk(FaweChunk chunk);

    public abstract boolean setComponents(FaweChunk fc);

    @Override
    public abstract FaweChunk getChunk(int x, int z);

    @Override
    public abstract boolean fixLighting(FaweChunk fc, boolean fixAll);

    public abstract boolean loadChunk(WORLD world, int x, int z, boolean generate);

    public abstract CHUNK getCachedChunk(WORLD world, int cx, int cz);

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return isChunkLoaded(getWorld(), x, z);
    };

    public WORLD getWorld() {
        if (impWorld != null) {
            return impWorld;
        }
        return impWorld = getWorld(world);
    }

    @Override
    public boolean regenerateChunk(int x, int z) {
        return regenerateChunk(getWorld(), x, z);
    }

    @Override
    public void addTask(int x, int z, Runnable runnable) {
        long pair = (long) (x) << 32 | (z) & 0xFFFFFFFFL;
        FaweChunk result = this.blocks.get(pair);
        if (result == null) {
            result = this.getChunk(x, z);
            result.addTask(runnable);
            FaweChunk previous = this.blocks.put(pair, result);
            if (previous == null) {
                chunks.add(result);
                return;
            }
            this.blocks.put(pair, previous);
            result = previous;
        }
        result.addTask(runnable);
    }

    private FaweChunk lastWrappedChunk;
    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;

    @Override
    public boolean setBlock(int x, int y, int z, short id, byte data) {
        if ((y > 255) || (y < 0)) {
            return false;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        if (cx != lastX || cz != lastZ) {
            lastX = cx;
            lastZ = cz;
            long pair = (long) (cx) << 32 | (cz) & 0xFFFFFFFFL;
            lastWrappedChunk = this.blocks.get(pair);
            if (lastWrappedChunk == null) {
                lastWrappedChunk = this.getChunk(x >> 4, z >> 4);
                lastWrappedChunk.setBlock(x & 15, y, z & 15, id, data);
                FaweChunk previous = this.blocks.put(pair, lastWrappedChunk);
                if (previous == null) {
                    chunks.add(lastWrappedChunk);
                    return true;
                }
                this.blocks.put(pair, previous);
                lastWrappedChunk = previous;
            }
        }
        lastWrappedChunk.setBlock(x & 15, y, z & 15, id, data);
        return true;
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        long pair = (long) (x >> 4) << 32 | (z >> 4) & 0xFFFFFFFFL;
        FaweChunk result = this.blocks.get(pair);
        if (result == null) {
            result = this.getChunk(x >> 4, z >> 4);
            FaweChunk previous = this.blocks.put(pair, result);
            if (previous != null) {
                this.blocks.put(pair, previous);
                result = previous;
            } else {
                chunks.add(result);
            }
        }
        result.setBiome(x & 15, z & 15, biome);
        return true;
    }

    @Override
    public FaweChunk next() {
        lastX = Integer.MIN_VALUE;
        lastZ = Integer.MIN_VALUE;
        try {
            if (this.blocks.size() == 0) {
                return null;
            }
            synchronized (blocks) {
                FaweChunk chunk = chunks.poll();
                if (chunk != null) {
                    blocks.remove(chunk.longHash());
                    this.execute(chunk);
                    return chunk;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int size() {
        return chunks.size();
    }

    private LinkedBlockingDeque<FaweChunk> toUpdate = new LinkedBlockingDeque<>();

    public boolean execute(FaweChunk fc) {
        if (fc == null) {
            return false;
        }
        // Set blocks / entities / biome
        if (!this.setComponents(fc)) {
            return false;
        }
        fc.executeTasks();
        return true;
    }

    @Override
    public void clear() {
        this.blocks.clear();
        this.chunks.clear();
    }

    @Override
    public void setChunk(FaweChunk chunk) {
        FaweChunk previous = this.blocks.put(chunk.longHash(), (FaweChunk) chunk);
        if (previous != null) {
            chunks.remove(previous);
        }
        chunks.add((FaweChunk) chunk);
    }

    public Collection<FaweChunk> sendChunk(Collection<FaweChunk> fcs) {
        for (final FaweChunk fc : fcs) {
            sendChunk(fc);
        }
        return new ArrayList<>();
    }

    public int lastChunkX = Integer.MIN_VALUE;
    public int lastChunkZ = Integer.MIN_VALUE;
    public int lastChunkY = Integer.MIN_VALUE;

    private CHUNK lastChunk;
    private SECTION lastSection;

    public SECTION getCachedSection(CHUNK chunk, int cy) {
        return (SECTION) lastChunk;
    }

    public abstract int getCombinedId4Data(SECTION section, int x, int y, int z);

    private final RunnableVal<IntegerPair> loadChunk = new RunnableVal<IntegerPair>() {
        @Override
        public void run(IntegerPair coord) {
            loadChunk(getWorld(), coord.x, coord.z, true);
        }
    };

    long average = 0;

    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        if (y < 0 || y > 255) {
            return 0;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastChunkX || cz != lastChunkZ) {
            lastChunkX = cx;
            lastChunkZ = cz;
            if (!isChunkLoaded(cx, cz)) {
                long start = System.currentTimeMillis();
                boolean sync = Thread.currentThread() == Fawe.get().getMainThread();
                if (sync) {
                    loadChunk(getWorld(), cx, cz, true);
                } else if (Settings.CHUNK_WAIT > 0) {
                    loadChunk.value = new IntegerPair(cx, cz);
                    TaskManager.IMP.sync(loadChunk, Settings.CHUNK_WAIT);
                    if (!isChunkLoaded(cx, cz)) {
                        throw new FaweException.FaweChunkLoadException();
                    }
                } else {
                    return 0;
                }
            }
            lastChunk = getCachedChunk(getWorld(), cx, cz);
            lastSection = getCachedSection(lastChunk, cy);
        } else if (cy != lastChunkY) {
            if (lastChunk == null) {
                return 0;
            }
            lastSection = getCachedSection(lastChunk, cy);
        }

        if (lastSection == null) {
            return 0;
        }
        return getCombinedId4Data(lastSection, x, y, z);
    }
}

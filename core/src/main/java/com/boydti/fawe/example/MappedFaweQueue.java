package com.boydti.fawe.example;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.LightingExtent;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public abstract class MappedFaweQueue<WORLD, CHUNK, CHUNKSECTIONS, SECTION> extends FaweQueue implements LightingExtent {

    private WORLD impWorld;

    private IFaweQueueMap map;

    public int lastSectionX = Integer.MIN_VALUE;
    public int lastSectionZ = Integer.MIN_VALUE;
    public int lastSectionY = Integer.MIN_VALUE;
    public CHUNK lastChunk;
    public CHUNKSECTIONS lastChunkSections;
    public SECTION lastSection;

    private CHUNK cachedLoadChunk;
    public final RunnableVal<IntegerPair> loadChunk = new RunnableVal<IntegerPair>() {

        {
            this.value = new IntegerPair(0, 0);
        }

        @Override
        public void run(IntegerPair coord) {
            cachedLoadChunk = loadChunk(getWorld(), coord.x, coord.z, true);
        }
    };

    public MappedFaweQueue(final World world) {
        this(world, null);
    }

    public MappedFaweQueue(final String world) {
        super(world);
        map = Settings.IMP.PREVENT_CRASHES ? new WeakFaweQueueMap(this) : new DefaultFaweQueueMap(this);
    }

    public MappedFaweQueue(final String world, IFaweQueueMap map) {
        super(world);
        if (map == null) {
            map = Settings.IMP.PREVENT_CRASHES ? new WeakFaweQueueMap(this) : new DefaultFaweQueueMap(this);
        }
        this.map = map;
    }

    public MappedFaweQueue(final World world, IFaweQueueMap map) {
        super(world);
        if (map == null) {
            map = Settings.IMP.PREVENT_CRASHES ? new WeakFaweQueueMap(this) : new DefaultFaweQueueMap(this);
        }
        this.map = map;
    }

    public IFaweQueueMap getFaweQueueMap() {
        return map;
    }

    @Override
    public Collection<FaweChunk> getFaweChunks() {
        return map.getFaweCunks();
    }

    @Override
    public void optimize() {
        final ForkJoinPool pool = TaskManager.IMP.getPublicForkJoinPool();
        map.forEachChunk(new RunnableVal<FaweChunk>() {
            @Override
            public void run(final FaweChunk chunk) {
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        chunk.optimize();
                    }
                });
            }
        });
        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public abstract WORLD getImpWorld();

    public abstract boolean regenerateChunk(WORLD world, int x, int z, BaseBiome biome, Long seed);

    @Override
    public abstract FaweChunk getFaweChunk(int x, int z);

    public abstract CHUNK loadChunk(WORLD world, int x, int z, boolean generate);

    public abstract CHUNKSECTIONS getSections(CHUNK chunk);

    public abstract CHUNKSECTIONS getCachedSections(WORLD world, int cx, int cz);

    public abstract CHUNK getCachedChunk(WORLD world, int cx, int cz);

    public WORLD getWorld() {
        if (impWorld != null) {
            return impWorld;
        }
        return impWorld = getImpWorld();
    }

    @Override
    public boolean regenerateChunk(int x, int z, BaseBiome biome, Long seed) {
        return regenerateChunk(getWorld(), x, z, biome, seed);
    }

    @Override
    public void addNotifyTask(int x, int z, Runnable runnable) {
        FaweChunk chunk = map.getFaweChunk(x, z);
        chunk.addNotifyTask(runnable);
    }

    @Override
    public boolean setBlock(int x, int y, int z, int id, int data) {
        int cx = x >> 4;
        int cz = z >> 4;
        FaweChunk chunk = map.getFaweChunk(cx, cz);
        chunk.setBlock(x & 15, y, z & 15, id, data);
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, int id) {
        int cx = x >> 4;
        int cz = z >> 4;
        FaweChunk chunk = map.getFaweChunk(cx, cz);
        chunk.setBlock(x & 15, y, z & 15, id);
        return true;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        if ((y >= FaweChunk.HEIGHT) || (y < 0)) {
            return;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        FaweChunk chunk = map.getFaweChunk(cx, cz);
        chunk.setTile(x & 15, y, z & 15, tag);
    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {
        if ((y >= FaweChunk.HEIGHT) || (y < 0)) {
            return;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        FaweChunk chunk = map.getFaweChunk(cx, cz);
        chunk.setEntity(tag);
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        if ((y >= FaweChunk.HEIGHT) || (y < 0)) {
            return;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        FaweChunk chunk = map.getFaweChunk(cx, cz);
        chunk.removeEntity(uuid);
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        int cx = x >> 4;
        int cz = z >> 4;
        FaweChunk chunk = map.getFaweChunk(cx, cz);
        chunk.setBiome(x & 15, z & 15, biome);
        return true;
    }

    @Override
    public boolean next(int amount, long time) {
        return map.next(amount, time);
    }

    public void start(FaweChunk chunk) {
        chunk.start();
    }

    public void end(FaweChunk chunk) {
        if (getProgressTask() != null) {
            getProgressTask().run(ProgressType.DISPATCH, size() + 1);
        }
        chunk.end();
    }

    @Override
    public void runTasks() {
        super.runTasks();
        if (getProgressTask() != null) {
            try {
                getProgressTask().run(ProgressType.DONE, 1);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        ArrayDeque<Runnable> tmp = new ArrayDeque<>(tasks);
        tasks.clear();
        for (Runnable run : tmp) {
            try {
                run.run();
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
        }
    }

    @Override
    public int size() {
        int size = map.size();
        if (size == 0 && getStage() == SetQueue.QueueStage.NONE) {
            runTasks();
        }
        return size;
    }

    @Override
    public void clear() {
        lastSectionX = Integer.MIN_VALUE;
        lastSectionZ = Integer.MIN_VALUE;
        lastSectionY = -1;
        lastChunk = null;
        lastChunkSections = null;
        map.clear();
        runTasks();
    }

    @Override
    public void setChunk(FaweChunk chunk) {
        map.add(chunk);
    }

    public SECTION getCachedSection(CHUNKSECTIONS chunk, int cy) {
        return (SECTION) lastChunkSections;
    }

    public abstract int getCombinedId4Data(SECTION section, int x, int y, int z);

    public abstract int getBiome(CHUNK chunk, int x, int z);

    public abstract CompoundTag getTileEntity(CHUNK chunk, int x, int y, int z);

    public CHUNK ensureChunkLoaded(int cx, int cz) throws FaweException.FaweChunkLoadException {
        CHUNK chunk = getCachedChunk(getWorld(), cx, cz);
        if (chunk != null) {
            return chunk;
        }
        boolean sync = Thread.currentThread() == Fawe.get().getMainThread();
        if (sync) {
            return loadChunk(getWorld(), cx, cz, true);
        } else if (Settings.IMP.HISTORY.CHUNK_WAIT_MS > 0) {
            cachedLoadChunk = null;
            loadChunk.value.x = cx;
            loadChunk.value.z = cz;
            TaskManager.IMP.syncWhenFree(loadChunk, Settings.IMP.HISTORY.CHUNK_WAIT_MS);
            return cachedLoadChunk;
        } else {
            return null;
        }
    }

    public boolean queueChunkLoad(final int cx, final int cz) {
        CHUNK chunk = getCachedChunk(getWorld(), cx, cz);
        if (chunk == null) {
            SetQueue.IMP.addTask(new Runnable() {
                @Override
                public void run() {
                    loadChunk(getWorld(), cx, cz, true);
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean hasBlock(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return false;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return false;
            }
        }
        if (lastSection == null) {
            return false;
        }
        return hasBlock(lastSection, x, y, z);
    }

    public boolean hasBlock(SECTION section, int x, int y, int z) {
        return getCombinedId4Data(lastSection, x, y, z) != 0;
    }

    public int getOpacity(SECTION section, int x, int y, int z) {
        int combined = getCombinedId4Data(section, x, y, z);
        if (combined == 0) {
            return 0;
        }
        BlockMaterial block = BundledBlockData.getInstance().getMaterialById(FaweCache.getId(combined));
        if (block == null) {
            return 15;
        }
        return Math.min(15, block.getLightOpacity());
    }

    public int getBrightness(SECTION section, int x, int y, int z) {
        int combined = getCombinedId4Data(section, x, y, z);
        if (combined == 0) {
            return 0;
        }
        BlockMaterial block = BundledBlockData.getInstance().getMaterialById(FaweCache.getId(combined));
        if (block == null) {
            return 15;
        }
        return Math.min(15, block.getLightValue());
    }

    public int getOpacityBrightnessPair(SECTION section, int x, int y, int z) {
        return MathMan.pair16(Math.min(15, getOpacity(section, x, y, z)), getBrightness(section, x, y, z));
    }

    public abstract int getSkyLight(SECTION sections, int x, int y, int z);

    public abstract int getEmmittedLight(SECTION sections, int x, int y, int z);

    public int getLight(SECTION sections, int x, int y, int z) {
        if (!hasSky()) {
            return getEmmittedLight(sections, x, y, z);
        }
        return Math.max(getSkyLight(sections, x, y, z), getEmmittedLight(sections, x, y, z));
    }

    @Override
    public int getLight(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return 0;
            }
        }
        if (lastSection == null) {
            return 0;
        }
        return getLight(lastSection, x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return 0;
            }
        }
        if (lastSection == null) {
            if (lastChunkSections == null) {
                return 0;
            }
            int max = FaweChunk.HEIGHT >> 4;
            do {
                if (++cy >= max) {
                    return 15;
                }
                lastSection = getCachedSection(lastChunkSections, cy);
            } while (lastSection == null);
        }
        if (lastSection == null) {

            return getSkyLight(x, y + 16, z);
        }
        return getSkyLight(lastSection, x, y, z);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        return getEmmittedLight(x, y, z);
    }

    @Override
    public int getEmmittedLight(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return 0;
            }
        }
        if (lastSection == null) {
            return 0;
        }
        return getEmmittedLight(lastSection, x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return 0;
            }
        }
        if (lastSection == null) {
            return 0;
        }
        return getOpacity(lastSection, x, y, z);
    }

    @Override
    public int getOpacityBrightnessPair(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return 0;
            }
        }
        if (lastSection == null) {
            return 0;
        }
        return getOpacityBrightnessPair(lastSection, x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return 0;
            }
        }
        if (lastSection == null) {
            return 0;
        }
        return getBrightness(lastSection, x, y, z);
    }

    @Override
    public int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        int cx = x >> 4;
        int cz = z >> 4;
        FaweChunk fc = map.getCachedFaweChunk(cx, cz);
        if (fc != null) {
            int combined = fc.getBlockCombinedId(x & 15, y, z & 15);
            if (combined != 0) {
                return combined;
            }
        }
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return 0;
            }
        }
        if (lastSection == null) {
            return 0;
        }
        return getCombinedId4Data(lastSection, x, y, z);
    }

    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return 0;
            }
        }
        if (lastSection == null) {
            return 0;
        }
        return getCombinedId4Data(lastSection, x, y, z);
    }

    @Override
    public int getBiomeId(int x, int z) throws FaweException.FaweChunkLoadException {
        int cx = x >> 4;
        int cz = z >> 4;
        lastSectionY = -1;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
            } else {
                lastChunkSections = null;
                return 0;
            }
        } else if (lastChunk == null) {
            return 0;
        }
        return getBiome(lastChunk, x, z) & 0xFF;
    }

    @Override
    public CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        int cx = x >> 4;
        int cz = z >> 4;
        lastSectionY = -1;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
            } else {
                lastChunkSections = null;
                return null;
            }
        } else if (lastChunk == null) {
            return null;
        }
        return getTileEntity(lastChunk, x, y, z);
    }
}

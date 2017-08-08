package com.boydti.fawe.object;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.NullRelighter;
import com.boydti.fawe.example.Relighter;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.annotation.Nullable;

public abstract class FaweQueue implements HasFaweQueue, Extent {

    private World weWorld;
    private String world;
    private ConcurrentLinkedDeque<EditSession> sessions;
    private long modified = System.currentTimeMillis();
    private RunnableVal2<FaweChunk, FaweChunk> changeTask;
    private RunnableVal2<ProgressType, Integer> progressTask;
    private SetQueue.QueueStage stage;
    private Settings settings = Settings.IMP;

    public FaweQueue(String world) {
        this.world = world;
    }

    public FaweQueue(World world) {
        if (world != null) {
            this.weWorld = world;
            this.world = Fawe.imp().getWorldName(world);
        }
    }

    public Relighter getRelighter() {
        return NullRelighter.INSTANCE;
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(-30000000, 0, -30000000);
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(30000000, getMaxY(), 30000000);
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        int combinedId4Data = getCachedCombinedId4Data(x, y, z, 0);
        int id = FaweCache.getId(combinedId4Data);
        if (!FaweCache.hasNBT(id)) {
            return FaweCache.CACHE_BLOCK[combinedId4Data];
        }
        try {
            CompoundTag tile = getTileEntity(x, y, z);
            if (tile != null) {
                return new BaseBlock(id, FaweCache.getData(combinedId4Data), tile);
            } else {
                return FaweCache.CACHE_BLOCK[combinedId4Data];
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
            return FaweCache.CACHE_BLOCK[combinedId4Data];
        }
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        return setBlock(x, y, z, block.getId(), block.getData(), block.getNbtData());
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return getLazyBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return null;
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block) throws WorldEditException {
        return setBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ(), block);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return setBiome(position.getBlockX(), position.getBlockZ(), biome);
    }

    public enum ProgressType {
        QUEUE,
        DISPATCH,
        DONE,
    }

    public enum RelightMode {
        NONE,
        OPTIMAL,
        ALL,
    }

    @Override
    public FaweQueue getQueue() {
        return this;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings == null ? Settings.IMP : settings;
    }

    public void setWorld(String world) {
        this.world = world;
        this.weWorld = null;
    }

    public void addEditSession(EditSession session) {
        if (session == null) {
            return;
        }
        if (this.getSessions() == null) {
            setSessions(new ConcurrentLinkedDeque<EditSession>());
        }
        getSessions().add(session);
    }

    public World getWEWorld() {
        return weWorld != null ? weWorld : (weWorld = FaweAPI.getWorld(world));
    }

    public String getWorldName() {
        return world;
    }

    /**
     * Add a progress task<br>
     * - Progress type
     * - Amount of type
     *
     * @param progressTask
     */
    public void setProgressTracker(RunnableVal2<ProgressType, Integer> progressTask) {
        this.setProgressTask(progressTask);
    }

    public Set<EditSession> getEditSessions() {
        return getSessions() == null ? new HashSet<EditSession>() : new HashSet<>(getSessions());
    }

    public ConcurrentLinkedDeque<EditSession> getSessions() {
        return sessions;
    }

    public void setSessions(ConcurrentLinkedDeque<EditSession> sessions) {
        this.sessions = sessions;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public RunnableVal2<ProgressType, Integer> getProgressTask() {
        return progressTask;
    }

    public void setProgressTask(RunnableVal2<ProgressType, Integer> progressTask) {
        this.progressTask = progressTask;
    }

    public void setChangeTask(RunnableVal2<FaweChunk, FaweChunk> changeTask) {
        this.changeTask = changeTask;
    }

    public RunnableVal2<FaweChunk, FaweChunk> getChangeTask() {
        return changeTask;
    }

    public void optimize() {
    }

    public int setBlocks(CuboidRegion cuboid, final int id, final int data) {
        RegionWrapper current = new RegionWrapper(cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
        final int minY = cuboid.getMinimumY();
        final int maxY = cuboid.getMaximumY();

        final FaweChunk<?> fc = getFaweChunk(0, 0);
        final byte dataByte = (byte) data;
        fc.fillCuboid(0, 15, minY, maxY, 0, 15, id, dataByte);
        fc.optimize();

        int bcx = (current.minX) >> 4;
        int bcz = (current.minZ) >> 4;

        int tcx = (current.maxX) >> 4;
        int tcz = (current.maxZ) >> 4;
        // [chunkx, chunkz, pos1x, pos1z, pos2x, pos2z, isedge]
        MainUtil.chunkTaskSync(current, new RunnableVal<int[]>() {
            @Override
            public void run(int[] value) {
                FaweChunk newChunk;
                if (value[6] == 0) {
                    newChunk = fc.copy(true);
                    newChunk.setLoc(FaweQueue.this, value[0], value[1]);
                } else {
                    int bx = value[2] & 15;
                    int tx = value[4] & 15;
                    int bz = value[3] & 15;
                    int tz = value[5] & 15;
                    if (bx == 0 && tx == 15 && bz == 0 && tz == 15) {
                        newChunk = fc.copy(true);
                        newChunk.setLoc(FaweQueue.this, value[0], value[1]);
                    } else {
                        newChunk = FaweQueue.this.getFaweChunk(value[0], value[1]);
                        newChunk.fillCuboid(value[2] & 15, value[4] & 15, minY, maxY, value[3] & 15, value[5] & 15, id, dataByte);
                    }
                }
                newChunk.addToQueue();
            }
        });
        return cuboid.getArea();
    }

    public abstract boolean setBlock(final int x, final int y, final int z, final int id, final int data);

    public boolean setBlock(int x, int y, int z, int id) {
        return setBlock(x, y, z, id, 0);
    }

    public boolean setBlock(int x, int y, int z, int id, int data, CompoundTag nbt) {
        if (nbt != null) {
            if (setBlock(x, y, z, id, data)) {
                MainUtil.setPosition(nbt, x, y, z);
                setTile(x, y, z, nbt);
                return true;
            }
            return false;
        } else {
            return setBlock(x, y, z, id, data);
        }
    }

    public abstract void setTile(int x, int y, int z, CompoundTag tag);

    public abstract void setEntity(int x, int y, int z, CompoundTag tag);

    public abstract void removeEntity(int x, int y, int z, UUID uuid);

    public abstract boolean setBiome(final int x, final int z, final BaseBiome biome);

    public abstract FaweChunk getFaweChunk(int x, int z);

    public abstract Collection<FaweChunk> getFaweChunks();

    public boolean setMCA(int mcaX, int mcaZ, RegionWrapper region, Runnable whileLocked, boolean load) {
        if (whileLocked != null) whileLocked.run();
        return true;
    }

    public abstract void setChunk(final FaweChunk chunk);

    public abstract File getSaveFolder();

    public int getMaxY() {
        return weWorld == null ? 255 : weWorld.getMaxY();
    }

    public void forEachBlockInChunk(int cx, int cz, RunnableVal2<Vector, BaseBlock> onEach) {
        int bx = cx << 4;
        int bz = cz << 4;
        MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            int xx = x + bx;
            mutable.mutX(xx);
            for (int z = 0; z < 16; z++) {
                int zz = z + bz;
                mutable.mutZ(zz);
                for (int y = 0; y <= getMaxY(); y++) {
                    int combined = getCombinedId4Data(xx, y, zz);
                    if (combined == 0) {
                        continue;
                    }
                    int id = FaweCache.getId(combined);
                    mutable.mutY(y);
                    if (FaweCache.hasNBT(id)) {
                        CompoundTag tile = getTileEntity(x, y, z);
                        BaseBlock block = new BaseBlock(id, FaweCache.getData(combined), tile);
                        onEach.run(mutable, block);
                    } else {
                        onEach.run(mutable, FaweCache.CACHE_BLOCK[combined]);
                    }
                }
            }
        }
    }

    public void forEachTileInChunk(int cx, int cz, RunnableVal2<Vector, BaseBlock> onEach) {
        int bx = cx << 4;
        int bz = cz << 4;
        MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            int xx = x + bx;
            for (int z = 0; z < 16; z++) {
                int zz = z + bz;
                for (int y = 0; y < getMaxY(); y++) {
                    int combined = getCombinedId4Data(xx, y, zz);
                    if (combined == 0) {
                        continue;
                    }
                    int id = FaweCache.getId(combined);
                    if (FaweCache.hasNBT(id)) {
                        mutable.mutX(xx);
                        mutable.mutZ(zz);
                        mutable.mutY(y);
                        CompoundTag tile = getTileEntity(x, y, z);
                        BaseBlock block = new BaseBlock(id, FaweCache.getData(combined), tile);
                        onEach.run(mutable, block);
                    }
                }
            }
        }
    }

    @Deprecated
    public boolean regenerateChunk(int x, int z) {
        return regenerateChunk(x, z, null, null);
    }

    public abstract boolean regenerateChunk(int x, int z, @Nullable BaseBiome biome, @Nullable Long seed);

    public void startSet(boolean parallel) {
    }

    public void endSet(boolean parallel) {
    }

    public int cancel() {
        clear();
        int count = 0;
        for (EditSession session : getSessions()) {
            if (session.cancel()) {
                count++;
            }
        }
        return count;
    }

    public abstract void sendBlockUpdate(FaweChunk chunk, FawePlayer... players);

    @Deprecated
    public boolean next() {
        int amount = Settings.IMP.QUEUE.PARALLEL_THREADS;
        long time = 20; // 30ms
        return next(amount, time);
    }

    /**
     * Gets the FaweChunk and sets the requested blocks
     *
     * @return
     */
    public abstract boolean next(int amount, long time);

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

    public abstract void sendChunk(FaweChunk chunk);

    public abstract void sendChunk(int x, int z, int bitMask);

    /**
     * This method is called when the server is < 1% available memory
     */
    public abstract void clear();

    public abstract void addNotifyTask(int x, int z, Runnable runnable);

    public boolean hasBlock(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getCombinedId4Data(x, y, z) != 0;
    }

    public abstract int getBiomeId(int x, int z) throws FaweException.FaweChunkLoadException;

    public abstract int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException;

    public abstract int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException;

    public int getAdjacentLight(int x, int y, int z) {
        int light = 0;
        if ((light = Math.max(light, getSkyLight(x - 1, y, z))) == 15) {
            return light;
        }
        if ((light = Math.max(light, getSkyLight(x + 1, y, z))) == 15) {
            return light;
        }
        if ((light = Math.max(light, getSkyLight(x, y, z - 1))) == 15) {
            return light;
        }
        return Math.max(light, getSkyLight(x, y, z + 1));
    }

    public abstract boolean hasSky();

    public abstract int getSkyLight(int x, int y, int z);

    public int getLight(int x, int y, int z) {
        if (!hasSky()) {
            return getEmmittedLight(x, y, z);
        }
        return Math.max(getSkyLight(x, y, z), getEmmittedLight(x, y, z));
    }

    public abstract int getEmmittedLight(int x, int y, int z);

    public abstract CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException;

    public int getCombinedId4Data(int x, int y, int z, int def) {
        try {
            return getCombinedId4Data(x, y, z);
        } catch (FaweException ignore) {
            return def;
        }
    }

    public int getCachedCombinedId4Data(int x, int y, int z, int def) {
        try {
            return getCachedCombinedId4Data(x, y, z);
        } catch (FaweException ignore) {
            return def;
        }
    }

    public int getCombinedId4DataDebug(int x, int y, int z, int def, EditSession session) {
        try {
            return getCombinedId4Data(x, y, z);
        } catch (FaweException ignore) {
            session.debug(BBC.WORLDEDIT_FAILED_LOAD_CHUNK, x >> 4, z >> 4);
            return def;
        } catch (Throwable e) {
            return 0;
        }
    }

    public int getBrightness(int x, int y, int z) {
        int combined = getCombinedId4Data(x, y, z);
        if (combined == 0) {
            return 0;
        }
        BlockMaterial block = BundledBlockData.getInstance().getMaterialById(FaweCache.getId(combined));
        if (block == null) {
            return 255;
        }
        return block.getLightValue();
    }

    public int getOpacityBrightnessPair(int x, int y, int z) {
        return MathMan.pair16(Math.min(15, getOpacity(x, y, z)), getBrightness(x, y, z));
    }

    public int getOpacity(int x, int y, int z) {
        int combined = getCombinedId4Data(x, y, z);
        if (combined == 0) {
            return 0;
        }
        BlockMaterial block = BundledBlockData.getInstance().getMaterialById(FaweCache.getId(combined));
        if (block == null) {
            return 255;
        }
        return block.getLightOpacity();
    }

    public abstract int size();

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Lock the thread until the queue is empty
     */
    public void flush() {
        flush(10000);
    }

    public SetQueue.QueueStage getStage() {
        return stage;
    }

    public void setStage(SetQueue.QueueStage stage) {
        this.stage = stage;
    }

    /**
     * Lock the thread until the queue is empty
     */
    public void flush(int time) {
        if (size() > 0) {
            if (Fawe.isMainThread()) {
                SetQueue.IMP.flush(this);
            } else {
                if (enqueue()) {
                    while (!isEmpty() && getStage() == SetQueue.QueueStage.ACTIVE) {
                        synchronized (this) {
                            try {
                                this.wait(time);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public ConcurrentLinkedDeque<Runnable> tasks = new ConcurrentLinkedDeque<>();

    public void addNotifyTask(Runnable runnable) {
        this.tasks.add(runnable);
    }


    public void runTasks() {
        synchronized (this) {
            this.notifyAll();
        }
        if (getProgressTask() != null) {
            try {
                getProgressTask().run(ProgressType.DONE, 1);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        while (!tasks.isEmpty()) {
            Runnable task = tasks.poll();
            if (task != null) {
                try {
                    task.run();
                } catch (Throwable e) {
                    MainUtil.handleError(e);
                }
            }
        }
    }

    public void addTask(Runnable whenFree) {
        tasks.add(whenFree);
    }

    public boolean enqueue() {
        return SetQueue.IMP.enqueue(this);
    }

    public void dequeue() {
        SetQueue.IMP.dequeue(this);
    }
}

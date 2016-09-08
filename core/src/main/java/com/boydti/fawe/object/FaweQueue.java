package com.boydti.fawe.object;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FaweQueue {

    private World weWorld;
    private final String world;
    private ConcurrentLinkedDeque<EditSession> sessions;
    private long modified = System.currentTimeMillis();
    private RunnableVal2<FaweChunk, FaweChunk> changeTask;
    private RunnableVal2<ProgressType, Integer> progressTask;

    public FaweQueue(World world) {
        this.weWorld = world;
        this.world = Fawe.imp().getWorldName(world);
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
        return weWorld;
    }

    public String getWorldName() {
        return world;
    }

    /**
     * Add a progress task<br>
     *      - Progress type
     *      - Amount of type
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

    public void optimize() {}

    public abstract boolean setBlock(final int x, final int y, final int z, final int id, final int data);

    public boolean setBlock(int x, int y, int z, int id) {
        return setBlock(x, y, z, id, 0);
    }

    public boolean setBlock(int x, int y, int z, int id, int data, CompoundTag nbt) {
        if (nbt != null) {
            MainUtil.setPosition(nbt, x, y, z);
            setTile(x, y, z, nbt);
        }
        return setBlock(x, y, z, id, data);
    }

    public abstract void setTile(int x, int y, int z, CompoundTag tag);

    public abstract void setEntity(int x, int y, int z, CompoundTag tag);

    public abstract void removeEntity(int x, int y, int z, UUID uuid);

    public abstract boolean setBiome(final int x, final int z, final BaseBiome biome);

    public abstract FaweChunk getFaweChunk(int x, int z);

    public abstract Collection<FaweChunk> getFaweChunks();

    public abstract void setChunk(final FaweChunk chunk);

    public abstract File getSaveFolder();

    public int getMaxY() {
        return weWorld == null ? 255 : weWorld.getMaxY();
    }

    public void forEachBlockInChunk(int cx, int cz, RunnableVal2<Vector, BaseBlock> onEach) {
        int bx = cx << 4;
        int bz = cz << 4;
        Vector mutable = new Vector(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            int xx = x + bx;
            mutable.x = xx;
            for (int z = 0; z < 16; z++) {
                int zz = z + bz;
                mutable.z = zz;
                for (int y = 0; y <= getMaxY(); y++) {
                    int combined = getCombinedId4Data(xx, y, zz);
                    if (combined == 0) {
                        continue;
                    }
                    int id = FaweCache.getId(combined);
                    mutable.y = y;
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
        Vector mutable = new Vector(0, 0, 0);
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
                        mutable.x = xx;
                        mutable.z = zz;
                        mutable.y = y;
                        CompoundTag tile = getTileEntity(x, y, z);
                        BaseBlock block = new BaseBlock(id, FaweCache.getData(combined), tile);
                        onEach.run(mutable, block);
                    }
                }
            }
        }
    }

    public abstract boolean isChunkLoaded(final int x, final int z);

    public abstract boolean regenerateChunk(int x, int z);

    public void startSet(boolean parallel) {}

    public void endSet(boolean parallel) {}

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

    /**
     * Gets the FaweChunk and sets the requested blocks
     * @return
     */
    public abstract boolean next();

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

    /**
     * This method is called when the server is < 1% available memory
     */
    public abstract void clear();
    
    public abstract void addNotifyTask(int x, int z, Runnable runnable);

    public abstract void addNotifyTask(Runnable runnable);

    public boolean hasBlock(int x, int y, int z) throws  FaweException.FaweChunkLoadException {
        return getCombinedId4Data(x, y, z) != 0;
    }

    public abstract int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException;

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

    /**
     * Lock the thread until the queue is empty
     */
    public void flush() {
        flush(10000);
    }

    /**
     * Lock the thread until the queue is empty
     */
    public void flush(int time) {
        if (size() > 0) {
            if (Fawe.get().isMainThread()) {
                SetQueue.IMP.flush(this);
            } else {
                if (enqueue()) {
                    final AtomicBoolean running = new AtomicBoolean(true);
                    addNotifyTask(new Runnable() {
                        @Override
                        public void run() {
                            TaskManager.IMP.notify(running);
                        }
                    });
                    TaskManager.IMP.wait(running, time);
                }
            }
        }
    }

    public boolean enqueue() {
        return SetQueue.IMP.enqueue(this);
    }

    public void dequeue() {
        SetQueue.IMP.dequeue(this);
    }
}

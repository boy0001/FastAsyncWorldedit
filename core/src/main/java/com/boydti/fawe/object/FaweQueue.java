package com.boydti.fawe.object;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FaweQueue {

    private final String world;
    private ConcurrentLinkedDeque<EditSession> sessions;
    private long modified = System.currentTimeMillis();
    private RunnableVal2<FaweChunk, FaweChunk> changeTask;
    private RunnableVal2<ProgressType, Integer> progressTask;

    public FaweQueue(String world) {
        this.world = world;
    }

    public enum ProgressType {
        QUEUE,
        DISPATCH,
        DONE,
    }

    public enum RelightMode {
        NONE,
        SHADOWLESS,
        MINIMAL,
        FULLBRIGHT,
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

    public abstract FaweChunk<?> getFaweChunk(int x, int z);

    public abstract void setChunk(final FaweChunk<?> chunk);

    public boolean fixLightingSafe(final FaweChunk<?> chunk, final RelightMode mode) {
        if (Settings.LIGHTING.ASYNC || Fawe.get().isMainThread()) {
            try {
                if (fixLighting(chunk, mode)) {
                    return true;
                }
                if (Fawe.get().isMainThread()) {
                    return false;
                }
            } catch (Throwable ignore) {}
        }
        return TaskManager.IMP.syncWhenFree(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = fixLighting(chunk, mode);
            }
        });
    }

    public abstract boolean fixLighting(final FaweChunk<?> chunk, RelightMode mode);

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
    public abstract FaweChunk next();

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

    public abstract void sendChunk(FaweChunk chunk, RelightMode mode);

    /**
     * This method is called when the server is < 1% available memory
     */
    public abstract void clear();
    
    public abstract void addNotifyTask(int x, int z, Runnable runnable);

    public abstract void addNotifyTask(Runnable runnable);

    public abstract int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException;

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
        }
    }

    public abstract int size();

    /**
     * Lock the thread until the queue is empty
     */
    public void flush() {
        flush(Integer.MAX_VALUE);
    }

    /**
     * Lock the thread until the queue is empty
     */
    public void flush(int time) {
        if (size() > 0) {
            if (Fawe.get().isMainThread()) {
                SetQueue.IMP.flush(this);
            } else {
                enqueue();
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

    public void enqueue() {
        SetQueue.IMP.enqueue(this);
    }

    public void dequeue() {
        SetQueue.IMP.dequeue(this);
    }
}

package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

public abstract class FaweQueue {

    public final String world;
    public LinkedBlockingDeque<EditSession> sessions;
    public long modified = System.currentTimeMillis();
    public RunnableVal2<FaweChunk, FaweChunk> changeTask;

    public FaweQueue(String world) {
        this.world = world;
    }

    public void addEditSession(EditSession session) {
        if (session == null) {
            return;
        }
        if (this.sessions == null) {
            sessions = new LinkedBlockingDeque<>();
        }
        sessions.add(session);
    }

    public Set<EditSession> getEditSessions() {
        return sessions == null ? new HashSet<EditSession>() : new HashSet<>(sessions);
    }

    public void setChangeTask(RunnableVal2<FaweChunk, FaweChunk> changeTask) {
        this.changeTask = changeTask;
    }

    public RunnableVal2<FaweChunk, FaweChunk> getChangeTask() {
        return changeTask;
    }

    public void optimize() {}

    public abstract boolean setBlock(final int x, final int y, final int z, final short id, final byte data);

    public abstract void setTile(int x, int y, int z, CompoundTag tag);

    public abstract void setEntity(int x, int y, int z, CompoundTag tag);

    public abstract boolean setBiome(final int x, final int z, final BaseBiome biome);

    public abstract FaweChunk<?> getChunk(int x, int z);

    public abstract void setChunk(final FaweChunk<?> chunk);

    public abstract boolean fixLighting(final FaweChunk<?> chunk, final boolean fixAll);

    public abstract boolean isChunkLoaded(final int x, final int z);

    public abstract boolean regenerateChunk(int x, int z);

    public void startSet(boolean parallel) {}

    public void endSet(boolean parallel) {}

    public int cancel() {
        int count = 0;
        for (EditSession session : sessions) {
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

    /**
     * This method is called when the server is < 1% available memory
     */
    public abstract void clear();
    
    public abstract void addNotifyTask(int x, int z, Runnable runnable);

    public abstract void addNotifyTask(Runnable runnable);

    public abstract int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException;

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

    public void enqueue() {
        SetQueue.IMP.enqueue(this);
    }
}

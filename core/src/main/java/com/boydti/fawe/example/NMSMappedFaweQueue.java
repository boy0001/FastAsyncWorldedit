package com.boydti.fawe.example;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NMSMappedFaweQueue<WORLD, CHUNK, CHUNKSECTION, SECTION> extends MappedFaweQueue<WORLD, CHUNKSECTION, SECTION> {
    public NMSMappedFaweQueue(String world) {
        super(world);
    }

    public boolean isRelighting(int x, int z) {
        long pair = MathMan.pairInt(x, z);
        return relighting.contains(pair) || blocks.contains(pair);
    }

    public final ConcurrentHashMap<Long, Long> relighting = new ConcurrentHashMap<>();

    @Override
    public void sendChunk(final FaweChunk fc, RelightMode mode) {
        if (mode == null) {
            mode = FaweQueue.RelightMode.values()[Settings.LIGHTING.MODE];
        }
        final RelightMode finalMode = mode;
        TaskManager.IMP.taskSoonMain(new Runnable() {
            @Override
            public void run() {
                final long pair = fc.longHash();
                relighting.put(pair, pair);
                final boolean result = finalMode == RelightMode.NONE || fixLighting(fc, finalMode);
                TaskManager.IMP.taskNowMain(new Runnable() {
                    @Override
                    public void run() {
                        if (!result) {
                            fixLighting(fc, finalMode);
                        }
                        CHUNK chunk = (CHUNK) fc.getChunk();
                        refreshChunk(getWorld(), chunk);
                        relighting.remove(pair);
                        if (relighting.isEmpty() && chunks.isEmpty()) {
                            runTasks();
                        }
                    }
                }, false);
            }
        }, Settings.LIGHTING.ASYNC);
    }

    public abstract boolean hasSky();

    public abstract void setFullbright(CHUNKSECTION sections);

    public abstract boolean removeLighting(CHUNKSECTION sections, RelightMode mode, boolean hasSky);

    public abstract boolean initLighting(CHUNK chunk, CHUNKSECTION sections, RelightMode mode);

    public boolean isSurrounded(final char[][] sections, final int x, final int y, final int z) {
        return this.isSolid(this.getId(sections, x, y + 1, z))
                && this.isSolid(this.getId(sections, x + 1, y - 1, z))
                && this.isSolid(this.getId(sections, x - 1, y, z))
                && this.isSolid(this.getId(sections, x, y, z + 1))
                && this.isSolid(this.getId(sections, x, y, z - 1));
    }

    public boolean isSolid(final int id) {
        return !FaweCache.isTransparent(id);
    }

    public int getId(final char[][] sections, final int x, final int y, final int z) {
        if ((x < 0) || (x > 15) || (z < 0) || (z > 15)) {
            return 1;
        }
        if ((y < 0) || (y > 255)) {
            return 1;
        }
        final int i = FaweCache.CACHE_I[y][x][z];
        final char[] section = sections[i];
        if (section == null) {
            return 0;
        }
        final int j = FaweCache.CACHE_J[y][x][z];
        return section[j] >> 4;
    }

    public abstract void relight(int x, int y, int z);

    public abstract int getSkyLight(CHUNKSECTION sections, int x, int y, int z);

    public abstract int getEmmittedLight(CHUNKSECTION sections, int x, int y, int z);

    public int getLight(CHUNKSECTION sections, int x, int y, int z) {
        if (!hasSky()) {
            return getEmmittedLight(sections, x, y, z);
        }
        return Math.max(getSkyLight(sections, x, y, z), getEmmittedLight(sections, x, y, z));
    }

    @Override
    public boolean fixLighting(FaweChunk<?> fc, RelightMode mode) {
        if (mode == RelightMode.NONE) {
            return true;
        }
        try {
            boolean async = Fawe.get().getMainThread() != Thread.currentThread();
            int cx = fc.getX();
            int cz = fc.getZ();
            if (!isChunkLoaded(cx, cz)) {
                if (async) {
                    return false;
                }
                loadChunk(getWorld(), cx, cz, false);
            }
            // Load adjacent
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    if (mode.ordinal() > 3 && !isChunkLoaded(cx + 1, cz)) {
                        if (async) {
                            final int cxx = cx + x;
                            final int czz = cz + z;
                            TaskManager.IMP.sync(new RunnableVal<Object>() {
                                @Override
                                public void run(Object value) {
                                    loadChunk(getWorld(), cxx, czz, false);
                                }
                            });
                        } else {
                            loadChunk(getWorld(), cx + x, cz + z, false);
                        }
                    }
                }
            }
            CHUNKSECTION sections = getCachedSections(getWorld(), cx, cz);
            boolean hasSky = hasSky();
            if (mode.ordinal() < 3) {
                if (hasSky) {
                    setFullbright(sections);
                }
            }
            CHUNK impChunk = (CHUNK) fc.getChunk();
            removeLighting(sections, mode, hasSky);
            if (hasSky) {
                initLighting(impChunk, sections, mode);
            }
            if (mode == RelightMode.SHADOWLESS) {
                return true;
            }
            CharFaweChunk bc = (CharFaweChunk) fc;
            if (((bc.getTotalRelight() != 0) || mode.ordinal() > 3)) {
                if (mode == RelightMode.ALL) {
                    bc = getPrevious(bc, sections, null, null, null, true);
                }
                int total = bc.getTotalCount();
                final int X = cx << 4;
                final int Z = cz << 4;
                for (int j = 15; j >= 0; j--) {
                    if (((bc.getRelight(j) == 0) && mode.ordinal() <= 3) || (bc.getCount(j) == 0 && mode != RelightMode.ALL) || ((bc.getCount(j) >= 4096) && (bc.getAir(j) == 0)) || bc.getAir(j) == 4096) {
                        continue;
                    }
                    final char[] array = bc.getIdArray(j);
                    if (array == null) {
                        continue;
                    }
                    switch (mode) {
                        case ALL: {
                            for (int k = 4095; k >= 0; k--) {
                                final int x = FaweCache.CACHE_X[j][k];
                                final int y = FaweCache.CACHE_Y[j][k];
                                if (y == 0) {
                                    continue;
                                }
                                final int z = FaweCache.CACHE_Z[j][k];
                                final int i = array[k];
                                final short id = (short) (i >> 4);
                                switch (FaweCache.getLight(id)) {
                                    case OCCLUDING:
                                        if (y == 0 || !FaweCache.isTransparent(bc.getCombinedId(x, y - 1, z) >> 4)) {
                                            continue;
                                        }
                                        break;
                                    case TRANSPARENT_EMIT:
                                    case SOLID_EMIT:
                                        if (this.isSurrounded(bc.getCombinedIdArrays(), x, y, z)) {
                                            continue;
                                        }
                                        break;
                                    case TRANSPARENT:
                                        if (y >= 255) {
                                            continue;
                                        }
                                        int light = getSkyLight(sections, x, y, z);
                                        if (light != 0) {
                                            continue;
                                        }
                                        break;
                                }
                                relight(X + x, y, Z + z);
                            }
                            break;
                        }
                        case OPTIMAL: {
                            for (int k = 4095; k >= 0; k--) {
                                final int x = FaweCache.CACHE_X[j][k];
                                final int y = FaweCache.CACHE_Y[j][k];
                                if (y == 0) {
                                    continue;
                                }
                                final int z = FaweCache.CACHE_Z[j][k];
                                final int i = array[k];
                                final short id = (short) (i >> 4);
                                switch (FaweCache.getLight(id)) {
                                    case OCCLUDING:
                                        if (y == 0 || !FaweCache.isTransparent(bc.getCombinedId(x, y - 1, z) >> 4)) {
                                            continue;
                                        }
                                        break;
                                    case TRANSPARENT_EMIT:
                                    case SOLID_EMIT:
                                        if (this.isSurrounded(bc.getCombinedIdArrays(), x, y, z)) {
                                            continue;
                                        }
                                        break;
                                    case TRANSPARENT:
                                        continue;
                                }
                                relight(X + x, y, Z + z);
                            }
                            break;
                        }
                        case FULLBRIGHT:
                        case MINIMAL: {
                            for (int k = 4095; k >= 0; k--) {
                                final int x = FaweCache.CACHE_X[j][k];
                                final int y = FaweCache.CACHE_Y[j][k];
                                final int z = FaweCache.CACHE_Z[j][k];
                                final int i = array[k];
                                final short id = (short) (i >> 4);
                                switch (FaweCache.getLight(id)) {
                                    case TRANSPARENT:
                                    case OCCLUDING:
                                        continue;
                                    case TRANSPARENT_EMIT:
                                    case SOLID_EMIT:
                                        if (this.isSurrounded(bc.getCombinedIdArrays(), x, y, z)) {
                                            continue;
                                        }
                                }
                                relight(X + x, y, Z + z);
                            }
                            break;
                        }
                    }
                }
            }
            return true;
        } catch (Throwable ignore) {}
        return false;
    }

    public abstract void refreshChunk(WORLD world, CHUNK chunk);

    public abstract CharFaweChunk getPrevious(CharFaweChunk fs, CHUNKSECTION sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception;

    public abstract CompoundTag getTileEntity(CHUNK chunk, int x, int y, int z);

    public abstract CHUNK getChunk(WORLD world, int x, int z);

    private CHUNK lastChunk;

    @Override
    public CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        if (y < 0 || y > 255) {
            return null;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        lastChunk = getChunk(getWorld(), cx, cz);
        if (lastChunk == null) {
            return null;
        }
        return getTileEntity(lastChunk, x, y, z);
    }

    @Override
    public int size() {
        if (chunks.size() == 0 && SetQueue.IMP.getStage(this) != SetQueue.QueueStage.INACTIVE && relighting.isEmpty()) {
            runTasks();
        }
        return chunks.size();
    }
}

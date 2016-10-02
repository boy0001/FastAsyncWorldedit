package com.boydti.fawe.example;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.World;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class NMSMappedFaweQueue<WORLD, CHUNK, CHUNKSECTION, SECTION> extends MappedFaweQueue<WORLD, CHUNKSECTION, SECTION> {

    private final int maxY;

    public NMSMappedFaweQueue(World world) {
        super(world);
        this.maxY = world.getMaxY();
        addRelightTask();
    }

    public NMSMappedFaweQueue(String world) {
        super(world);
        this.maxY = 256;
        addRelightTask();
    }

    public NMSMappedFaweQueue(String world, IFaweQueueMap map) {
        super(world, map);
        this.maxY = 256;
        addRelightTask();
    }

    public NMSMappedFaweQueue(World world, IFaweQueueMap map) {
        super(world, map);
        this.maxY = world.getMaxY();
        addRelightTask();
    }

    private void addRelightTask() {
        addNotifyTask(new Runnable() {
            @Override
            public void run() {
                if (relighter != null) {
                    TaskManager.IMP.taskNowAsync(new Runnable() {
                        @Override
                        public void run() {
                            relighter.fixLightingSafe(hasSky());
                        }
                    });
                }
            }
        });
    }

    private NMSRelighter relighter;

    @Override
    public void end(FaweChunk chunk) {
        super.end(chunk);
        if (Settings.LIGHTING.MODE == 0) {
            sendChunk(chunk);
            return;
        }
        if (relighter == null) {
            relighter = new NMSRelighter(this);
        }
        if (Settings.LIGHTING.MODE == 2) {
            relighter.addChunk(chunk.getX(), chunk.getZ(), null, chunk.getBitMask());
            return;
        }
        CharFaweChunk cfc = (CharFaweChunk) chunk;
        boolean relight = false;
        boolean[] fix = new boolean[(maxY + 1) >> 4];
        boolean sky = hasSky();
        for (int i = 0; i < cfc.ids.length; i++) {
            if ((sky && ((cfc.getAir(i) & 4095) != 0 || (cfc.getCount(i) & 4095) != 0)) || cfc.getRelight(i) != 0) {
                relight = true;
                fix[i] = true;
            }
        }
        if (relight) {
            relighter.addChunk(chunk.getX(), chunk.getZ(), fix, chunk.getBitMask());
        } else {
            sendChunk(chunk);
        }
    }

    @Override
    public void sendChunk(final FaweChunk fc) {
        if (Fawe.get().isMainThread()) {
            refreshChunk(fc);
        } else {
            SetQueue.IMP.addTask(new Runnable() {
                @Override
                public void run() {
                    refreshChunk(fc);
                }
            });
        }
    }

    public abstract void setFullbright(CHUNKSECTION sections);

    public abstract boolean removeLighting(CHUNKSECTION sections, RelightMode mode, boolean hasSky);

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
        if ((y < 0) || (y > maxY)) {
            return 1;
        }
        final int i = FaweCache.CACHE_I[y][z][x];
        final char[] section = sections[i];
        if (section == null) {
            return 0;
        }
        final int j = FaweCache.CACHE_J[y][z][x];
        return section[j] >> 4;
    }

    public abstract void relight(int x, int y, int z);

    public abstract void relightBlock(int x, int y, int z);

    public abstract void relightSky(int x, int y, int z);

    public void setSkyLight(int x, int y, int z, int value) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastChunkX || cz != lastChunkZ) {
            lastChunkX = cx;
            lastChunkZ = cz;
            if (!ensureChunkLoaded(cx, cz)) {
                return;
            }
            lastChunkSections = getCachedSections(getWorld(), cx, cz);
            lastSection = getCachedSection(lastChunkSections, cy);
        } else if (cy != lastChunkY) {
            if (lastChunkSections == null) {
                return;
            }
            lastSection = getCachedSection(lastChunkSections, cy);
        }
        if (lastSection == null) {
            return;
        }
        setSkyLight(lastSection, x, y, z, value);
    }

    public void setBlockLight(int x, int y, int z, int value) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastChunkX || cz != lastChunkZ) {
            lastChunkX = cx;
            lastChunkZ = cz;
            if (!ensureChunkLoaded(cx, cz)) {
                return;
            }
            lastChunkSections = getCachedSections(getWorld(), cx, cz);
            lastSection = getCachedSection(lastChunkSections, cy);
        } else if (cy != lastChunkY) {
            if (lastChunkSections == null) {
                return;
            }
            lastSection = getCachedSection(lastChunkSections, cy);
        }
        if (lastSection == null) {
            return;
        }
        setBlockLight(lastSection, x, y, z, value);
    }

    public abstract void setSkyLight(SECTION section, int x, int y, int z, int value);

    public abstract void setBlockLight(SECTION section, int x, int y, int z, int value);

    public abstract void refreshChunk(FaweChunk fs);

    public abstract CharFaweChunk getPrevious(CharFaweChunk fs, CHUNKSECTION sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception;

    public abstract CompoundTag getTileEntity(CHUNK chunk, int x, int y, int z);

    public abstract CHUNK getChunk(WORLD world, int x, int z);

    private CHUNK lastChunk;

    @Override
    public CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        if (y < 0 || y > maxY) {
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
}

package com.boydti.fawe.example;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.jnbt.CompoundTag;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class NMSMappedFaweQueue<WORLD, CHUNK, CHUNKSECTION, SECTION> extends MappedFaweQueue<WORLD, CHUNKSECTION, SECTION> {

    public NMSMappedFaweQueue(String world) {
        super(world);
    }

    private NMSRelighter relighter;

    @Override
    public boolean execute(FaweChunk fc) {
        if (super.execute(fc)) {
            sendChunk(fc);
            if (Settings.LIGHTING.MODE == 0) {
                return true;
            }
            if (relighter == null) {
                relighter = new NMSRelighter(this);
            }
            if (Settings.LIGHTING.MODE == 2) {
                relighter.addChunk(fc.getX(), fc.getZ(), null);
                return true;
            }
            CharFaweChunk chunk = (CharFaweChunk) fc;
            boolean relight = false;
            boolean[] fix = new boolean[16];
            boolean sky = hasSky();
            for (int i = 0; i < 16; i++) {
                if ((sky && ((chunk.getAir(i) & 4095) != 0 || (chunk.getCount(i) & 4095) != 0)) || chunk.getRelight(i) != 0) {
                    relight = true;
                    fix[i] = true;
                }
            }
            if (relight) {
                relighter.addChunk(chunk.getX(), chunk.getZ(), fix);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void runTasks() {
        super.runTasks();
        if (relighter != null) {
            boolean sky = hasSky();
            if (sky) {
                relighter.fixSkyLighting();
            }
            relighter.fixBlockLighting();
            relighter.sendChunks();
        }
    }

    @Override
    public void sendChunk(final FaweChunk fc) {
        refreshChunk(getWorld(), (CHUNK) fc.getChunk());
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
}

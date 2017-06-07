package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

public class BukkitQueue_All extends BukkitQueue_0<ChunkSnapshot, ChunkSnapshot, ChunkSnapshot> {

    public static int ALLOCATE;
    private static int LIGHT_MASK = 0x739C0;

    public BukkitQueue_All(com.sk89q.worldedit.world.World world) {
        super(world);
        if (Settings.IMP.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.IMP.QUEUE.EXTRA_TIME_MS;
            Settings.IMP.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1;
        }
    }

    public BukkitQueue_All(String world) {
        super(world);
        if (Settings.IMP.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.IMP.QUEUE.EXTRA_TIME_MS;
            Settings.IMP.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1;
        }
    }

    @Override
    public void setHeightMap(FaweChunk chunk, byte[] heightMap) {
        // Not supported
    }

    @Override
    public void setSkyLight(ChunkSnapshot chunk, int x, int y, int z, int value) {
        // Not supported
    }

    @Override
    public void setBlockLight(ChunkSnapshot chunk, int x, int y, int z, int value) {
        // Not supported
    }

    @Override
    public int getCombinedId4Data(ChunkSnapshot chunk, int x, int y, int z) {
        if (chunk.isSectionEmpty(y >> 4)) {
            return 0;
        }
        int id = chunk.getBlockTypeId(x & 15, y, z & 15);
        if (FaweCache.hasData(id)) {
            int data = chunk.getBlockData(x & 15, y, z & 15);
            return (id << 4) + data;
        } else {
            return id << 4;
        }
    }

    @Override
    public int getBiome(ChunkSnapshot chunkSnapshot, int x, int z) {
        Biome biome = chunkSnapshot.getBiome(x & 15, z & 15);
        return adapter.getBiomeId(biome);
    }

    @Override
    public ChunkSnapshot getSections(ChunkSnapshot chunkSnapshot) {
        return chunkSnapshot;
    }

    @Override
    public ChunkSnapshot getCachedChunk(World world, int cx, int cz) {
        if (world.isChunkLoaded(cx, cz)) {
            long pair = MathMan.pairInt(cx, cz);
            Long originalKeep = keepLoaded.get(pair);
            keepLoaded.put(pair, Long.MAX_VALUE);
            if (world.isChunkLoaded(cx, cz)) {
                Chunk chunk = world.getChunkAt(cx, cz);
                ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, true, false);
                if (originalKeep != null) {
                    keepLoaded.put(pair, originalKeep);
                } else {
                    keepLoaded.remove(pair);
                }
                return snapshot;
            } else {
                keepLoaded.remove(pair);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public int getEmmittedLight(final ChunkSnapshot chunk, int x, int y, int z) {
        return chunk.getBlockEmittedLight(x & 15, y, z & 15);
    }

    @Override
    public int getSkyLight(final ChunkSnapshot chunk, int x, int y, int z) {
        return chunk.getBlockSkyLight(x & 15, y, z & 15);
    }

    @Override
    public int getLight(final ChunkSnapshot chunk, int x, int y, int z) {
        x = x & 15;
        z = z & 15;
        return Math.max(chunk.getBlockEmittedLight(x, y, z), chunk.getBlockSkyLight(x, y, z));
    }

    @Override
    public ChunkSnapshot loadChunk(World world, int x, int z, boolean generate) {
        Chunk chunk = world.getChunkAt(x, z);
        chunk.load(generate);
        return chunk.isLoaded() ? chunk.getChunkSnapshot(false, true, false) : null;
    }

    @Override
    public ChunkSnapshot getCachedSections(World impWorld, int cx, int cz) {
        return getCachedChunk(impWorld, cx, cz);
    }

    @Override
    public CompoundTag getTileEntity(ChunkSnapshot chunk, int x, int y, int z) {
        if (adapter == null) {
            return null;
        }
        Location loc = new Location(getWorld(), x, y, z);
        BaseBlock block = adapter.getBlock(loc);
        return block != null ? block.getNbtData() : null;
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return new BukkitChunk_All(this, x, z);
    }

    private int skip;

    @Override
    public void startSet(boolean parallel) {
        super.startSet(true);
    }

    private Field fieldNeighbors;
    private Method chunkGetHandle;

    /**
     * Exploiting a bug in the vanilla lighting algorithm for faster block placement
     *  - Could have been achieved without reflection by force unloading specific chunks
     *  - Much faster just setting the variable manually though
     * @param chunk
     * @return
     */
    protected Object[] disableLighting(Chunk chunk) {
        try {
            if (chunkGetHandle == null) {
                chunkGetHandle = chunk.getClass().getDeclaredMethod("getHandle");
                chunkGetHandle.setAccessible(true);
            }
            Object nmsChunk = chunkGetHandle.invoke(chunk);
            if (fieldNeighbors == null) {
                fieldNeighbors = nmsChunk.getClass().getDeclaredField("neighbors");
                fieldNeighbors.setAccessible(true);
            }
            Object value = fieldNeighbors.get(nmsChunk);
            fieldNeighbors.set(nmsChunk, 0);
            return new Object[] {nmsChunk, value};
        } catch (Throwable ignore) {}
        return null;
    }

    protected void disableLighting(Object[] disableResult) {
        if (disableResult != null) {
            try {
                fieldNeighbors.set(disableResult[0], 0);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    protected void resetLighting(Object[] disableResult) {
        if (disableResult != null) {
            try {
                fieldNeighbors.set(disableResult[0], disableResult[1]);
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        }
    }

    protected void enableLighting(Object[] disableResult) {
        if (disableResult != null) {
            try {
                fieldNeighbors.set(disableResult[0], 0x739C0);
            } catch (Throwable ignore) {}
        }
    }

    @Override
    public void endSet(boolean parallel) {
        super.endSet(true);
    }
}

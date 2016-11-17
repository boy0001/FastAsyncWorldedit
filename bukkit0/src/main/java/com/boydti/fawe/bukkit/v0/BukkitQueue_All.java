package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BukkitQueue_All extends BukkitQueue_0<Chunk, Chunk, Chunk> {

    public static int ALLOCATE;
    private static int LIGHT_MASK = 0x739C0;

    public BukkitQueue_All(com.sk89q.worldedit.world.World world) {
        super(world);
        if (Settings.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.QUEUE.EXTRA_TIME_MS;
            Settings.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
            Settings.QUEUE.PARALLEL_THREADS = 1;
        }
    }

    public BukkitQueue_All(String world) {
        super(world);
        if (Settings.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.QUEUE.EXTRA_TIME_MS;
            Settings.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
            Settings.QUEUE.PARALLEL_THREADS = 1;
        }
    }

    @Override
    public void setHeightMap(FaweChunk chunk, byte[] heightMap) {
        // Do nothing
    }

    @Override
    public void setSkyLight(Chunk chunk, int x, int y, int z, int value) {

    }

    @Override
    public void setBlockLight(Chunk chunk, int x, int y, int z, int value) {
//        chunk.getBlock(x & 15, y, z & 15);
    }

    public int getCombinedId4Data(Chunk section, int x, int y, int z) {
        Block block = ((Chunk) section).getBlock(x & 15, y, z & 15);
        int combined = block.getTypeId() << 4;
        if (FaweCache.hasData(combined)) {
            combined += block.getData();
        }
        return combined;
    }

    @Override
    public int getEmmittedLight(final Chunk chunk, int x, int y, int z) {
        if (!chunk.isLoaded()) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    chunk.load(true);
                }
            });
        }
        return chunk.getBlock(x, y, z).getLightFromBlocks();
    }

    @Override
    public int getSkyLight(final Chunk chunk, int x, int y, int z) {
        if (!chunk.isLoaded()) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    chunk.load(true);
                }
            });
        }
        return chunk.getBlock(x, y, z).getLightFromSky();
    }

    @Override
    public int getLight(final Chunk chunk, int x, int y, int z) {
        if (!chunk.isLoaded()) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    chunk.load(true);
                }
            });
        }
        return chunk.getBlock(x, y, z).getLightLevel();
    }

    @Override
    public Chunk getCachedSections(World impWorld, int cx, int cz) {
        return impWorld.getChunkAt(cx, cz);
    }

    @Override
    public CompoundTag getTileEntity(Chunk chunk, int x, int y, int z) {
        if (adapter == null) {
            return null;
        }
        Location loc = new Location(getWorld(), x, y, z);
        BaseBlock block = adapter.getBlock(loc);
        return block != null ? block.getNbtData() : null;
    }

    @Override
    public Chunk getChunk(World world, int x, int z) {
        return world.getChunkAt(x, z);
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

package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BukkitQueue_All extends BukkitQueue_0<Chunk, Chunk, Chunk> {

    public static int ALLOCATE;
    public static double TPS_TARGET = 18.5;

    public BukkitQueue_All(String world) {
        super(world);
        if (Settings.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.QUEUE.EXTRA_TIME_MS;
            Settings.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
        }
    }

    @Override
    public void setSkyLight(int x, int y, int z, int value) {

    }

    @Override
    public void setBlockLight(int x, int y, int z, int value) {

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
    public boolean fixLighting(FaweChunk<?> fc, RelightMode mode) {
        return false;
    }

    @Override
    public Chunk getCachedSections(World impWorld, int cx, int cz) {
        return impWorld.getChunkAt(cx, cz);
    }

    @Override
    public CompoundTag getTileEntity(Chunk chunk, int x, int y, int z) {
        return null;
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
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        if (skip > 0) {
            skip--;
            fc.addToQueue();
            return true;
        }
        long start = System.currentTimeMillis();
        ((BukkitChunk_All) fc).execute(start);
        if (System.currentTimeMillis() - start > 50 || Fawe.get().getTPS() < TPS_TARGET) {
            skip = 10;
        }
        return true;
    }

    @Override
    public void startSet(boolean parallel) {
        super.startSet(true);
    }

    @Override
    public void endSet(boolean parallel) {
        super.endSet(true);
    }
}

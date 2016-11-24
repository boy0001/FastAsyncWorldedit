package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;

public class AsyncChunk implements Chunk {

    private final World world;
    private final int z;
    private final int x;
    private final FaweQueue queue;

    public AsyncChunk(World world, FaweQueue queue, int x, int z) {
        this.world = world instanceof AsyncWorld ? world : new AsyncWorld(world, true);
        this.queue = queue;
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Chunk)) {
            return false;
        }
        Chunk other = (Chunk) obj;
        return other.getX() == x && other.getZ() == z && world.equals(other.getWorld());
    }

    @Override
    public int hashCode() {
        return MathMan.pair((short) x, (short) z);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return new AsyncBlock(world, queue, (this.x << 4) + x, y, (this.z << 4) + z);
    }

    @Override
    public ChunkSnapshot getChunkSnapshot() {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public ChunkSnapshot getChunkSnapshot(boolean includeMaxblocky, boolean includeBiome, boolean includeBiomeTempRain) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public Entity[] getEntities() {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public BlockState[] getTileEntities() {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public boolean isLoaded() {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public boolean load(final boolean generate) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = world.loadChunk(x, z, generate);
            }
        });
    }

    @Override
    public boolean load() {
        return load(false);
    }

    @Override
    public boolean unload(boolean save, boolean safe) {
        return world.unloadChunk(x, z, save, safe);
    }

    @Override
    public boolean unload(boolean save) {
        return unload(true, false);
    }

    @Override
    public boolean unload() {
        return unload(true);
    }
}

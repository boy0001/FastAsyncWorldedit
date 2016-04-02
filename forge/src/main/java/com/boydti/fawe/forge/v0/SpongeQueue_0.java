package com.boydti.fawe.forge.v0;

import com.boydti.fawe.object.ChunkLoc;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

/**
 * Created by Jesse on 4/2/2016.
 */
public abstract class SpongeQueue_0 extends FaweQueue {

    /**
     * Map of chunks in the queue
     */
    private final ConcurrentHashMap<ChunkLoc, FaweChunk<Chunk>> blocks = new ConcurrentHashMap<>();

    @Override
    public boolean isChunkLoaded(String worldName, int x, int z) {
        World world = Sponge.getServer().getWorld(worldName).get();
        Chunk chunk = world.getChunk(x << 4, 0, z << 4).orElse(null);
        return chunk != null && chunk.isLoaded();
    }

    @Override
    public void addTask(String world, int x, int y, int z, Runnable runnable) {
        // TODO Auto-generated method stub
        final ChunkLoc wrap = new ChunkLoc(world, x >> 4, z >> 4);
        FaweChunk<Chunk> result = this.blocks.get(wrap);
        if (result == null) {
            throw new IllegalArgumentException("Task must be accompanied by a block change or manually adding to queue!");
        }
        result.addTask(runnable);
    }

    @Override
    public boolean setBlock(final String world, int x, final int y, int z, final short id, final byte data) {
        if ((y > 255) || (y < 0)) {
            return false;
        }
        final ChunkLoc wrap = new ChunkLoc(world, x >> 4, z >> 4);
        x = x & 15;
        z = z & 15;
        FaweChunk<Chunk> result = this.blocks.get(wrap);
        if (result == null) {
            result = this.getChunk(wrap);
            result.setBlock(x, y, z, id, data);
            final FaweChunk<Chunk> previous = this.blocks.put(wrap, result);
            if (previous == null) {
                return true;
            }
            this.blocks.put(wrap, previous);
            result = previous;
        }
        result.setBlock(x, y, z, id, data);
        return true;
    }

    @Override
    public boolean setBiome(final String world, int x, int z, final BaseBiome biome) {
        final ChunkLoc wrap = new ChunkLoc(world, x >> 4, z >> 4);
        x = x & 15;
        z = z & 15;
        FaweChunk<Chunk> result = this.blocks.get(wrap);
        if (result == null) {
            result = this.getChunk(wrap);
            final FaweChunk<Chunk> previous = this.blocks.put(wrap, result);
            if (previous != null) {
                this.blocks.put(wrap, previous);
                result = previous;
            }
        }
        result.setBiome(x, z, biome);
        return true;
    }

    @Override
    public FaweChunk<Chunk> next() {
        try {
            if (this.blocks.size() == 0) {
                return null;
            }
            final Iterator<Map.Entry<ChunkLoc, FaweChunk<Chunk>>> iter = this.blocks.entrySet().iterator();
            final FaweChunk<Chunk> toReturn = iter.next().getValue();
            if (SetQueue.IMP.isWaiting()) {
                return null;
            }
            iter.remove();
            this.execute(toReturn);
            return toReturn;
        } catch (final Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private final ArrayDeque<FaweChunk<Chunk>> toUpdate = new ArrayDeque<>();

    public boolean execute(final FaweChunk<Chunk> fc) {
        if (fc == null) {
            return false;
        }
        // Load chunk
        final Chunk chunk = fc.getChunk();
        chunk.loadChunk(true);
        // Set blocks / entities / biome
        if (!this.setComponents(fc)) {
            return false;
        }
        fc.executeTasks();
        return true;
    }

    @Override
    public void clear() {
        this.blocks.clear();
    }

    @Override
    public void setChunk(final FaweChunk<?> chunk) {
        this.blocks.put(chunk.getChunkLoc(), (FaweChunk<Chunk>) chunk);
    }

    public abstract Collection<FaweChunk<Chunk>> sendChunk(final Collection<FaweChunk<Chunk>> fcs);

    public abstract boolean setComponents(final FaweChunk<Chunk> fc);

    @Override
    public abstract FaweChunk<Chunk> getChunk(final ChunkLoc wrap);

    @Override
    public abstract boolean fixLighting(final FaweChunk<?> fc, final boolean fixAll);
}

package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BukkitQueue_All extends BukkitQueue_0<Chunk, Chunk, Chunk> {
    public BukkitQueue_All(String world) {
        super(world);
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
}

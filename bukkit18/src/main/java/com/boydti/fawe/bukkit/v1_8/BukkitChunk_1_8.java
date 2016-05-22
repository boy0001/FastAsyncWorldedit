package com.boydti.fawe.bukkit.v1_8;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweQueue;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

public class BukkitChunk_1_8 extends CharFaweChunk<Chunk> {
    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public BukkitChunk_1_8(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public Chunk getNewChunk() {
        return Bukkit.getWorld(getParent().getWorld()).getChunkAt(getX(), getZ());
    }
}

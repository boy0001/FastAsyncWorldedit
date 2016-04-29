package com.boydti.fawe.sponge.v1_8;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.util.FaweQueue;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;

public class SpongeChunk_1_8 extends CharFaweChunk<net.minecraft.world.chunk.Chunk> {

    public SpongeChunk_1_8(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public net.minecraft.world.chunk.Chunk getNewChunk() {
        World world = Sponge.getServer().getWorld(getParent().world).get();
        return (net.minecraft.world.chunk.Chunk) world.loadChunk(getX(), 0, getZ(), true).get();
    }
}

package com.boydti.fawe.bukkit.v1_11;

import java.util.Arrays;
import net.minecraft.server.v1_11_R1.GenLayer;
import net.minecraft.server.v1_11_R1.IntCache;

public class MutableGenLayer extends GenLayer {

    private int biome;

    public MutableGenLayer(long seed) {
        super(seed);
    }

    public MutableGenLayer set(int biome) {
        this.biome = biome;
        return this;
    }

    @Override
    public int[] a(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] biomes = IntCache.a(areaWidth * areaHeight);
        Arrays.fill(biomes, biome);
        return biomes;
    }
}

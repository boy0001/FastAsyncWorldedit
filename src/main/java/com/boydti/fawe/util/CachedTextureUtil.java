package com.boydti.fawe.util;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.blocks.BaseBlock;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.FileNotFoundException;

public class CachedTextureUtil extends DelegateTextureUtil {
    private final TextureUtil parent;
    private transient Int2ObjectOpenHashMap<Integer> colorBlockMap;
    private transient Int2ObjectOpenHashMap<Integer> colorBiomeMap;
    private transient Int2ObjectOpenHashMap<char[]> colorLayerMap;

    public CachedTextureUtil(TextureUtil parent) throws FileNotFoundException {
        super(parent);
        this.parent = parent;
        this.colorBlockMap = new Int2ObjectOpenHashMap<>();
        this.colorLayerMap = new Int2ObjectOpenHashMap<>();
        this.colorBiomeMap = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public char[] getNearestLayer(int color) {
        char[] closest = colorLayerMap.get(color);
        if (closest != null) {
            return closest;
        }
        closest = parent.getNearestLayer(color);
        if (closest != null) {
            colorLayerMap.put(color, closest);
        }
        return closest;
    }

    @Override
    public BiomeColor getNearestBiome(int color) {
        Integer value = colorBiomeMap.get(color);
        if (value != null) {
            return getBiome(value);
        }
        BiomeColor result = parent.getNearestBiome(color);
        if (result != null) {
            colorBiomeMap.put((int) color, (Integer) result.id);
        }
        return result;
    }

    @Override
    public BaseBlock getNearestBlock(int color) {
        Integer value = colorBlockMap.get(color);
        if (value != null) {
            return FaweCache.CACHE_BLOCK[value];
        }
        BaseBlock result = parent.getNearestBlock(color);
        if (result != null) {
            colorBlockMap.put((int) color, (Integer) result.getCombined());
        }
        return result;
    }
}

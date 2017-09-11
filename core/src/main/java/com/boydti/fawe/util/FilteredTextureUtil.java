package com.boydti.fawe.util;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.blocks.BaseBlock;
import java.io.FileNotFoundException;
import java.util.Set;

public class FilteredTextureUtil extends TextureUtil {
    private final Set<BaseBlock> blocks;

    public FilteredTextureUtil(TextureUtil parent, Set<BaseBlock> blocks) throws FileNotFoundException {
        super(parent.getFolder());
        this.blocks = blocks;
        this.validMixBiomeColors = parent.validMixBiomeColors;
        this.validMixBiomeIds = parent.validMixBiomeIds;
        this.validBiomes = parent.validBiomes;
        this.blockColors = parent.blockColors;
        this.blockDistance = parent.blockDistance;
        this.distances = parent.distances;
        this.validColors = new int[distances.length];
        this.validBlockIds = new char[distances.length];
        int num = 0;
        for (int i = 0; i < parent.validBlockIds.length; i++) {
            BaseBlock block = FaweCache.CACHE_BLOCK[parent.validBlockIds[i]];
            if (blocks.contains(block) || blocks.contains(new BaseBlock(block.getId(), -1))) num++;
        }
        this.validBlockIds = new char[num];
        this.validColors = new int[num];
        num = 0;
        for (int i = 0; i < parent.validBlockIds.length; i++) {
            BaseBlock block = FaweCache.CACHE_BLOCK[parent.validBlockIds[i]];
            if (blocks.contains(block) || blocks.contains(new BaseBlock(block.getId(), -1))) {
                validBlockIds[num] = parent.validBlockIds[i];
                validColors[num++] = parent.validColors[i];
            }
        }
        this.calculateLayerArrays();
    }

    public Set<BaseBlock> getBlocks() {
        return blocks;
    }
}
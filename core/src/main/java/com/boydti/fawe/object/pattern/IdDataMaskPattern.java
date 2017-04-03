package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class IdDataMaskPattern extends AbstractPattern {
    private final Extent extent;
    private final Pattern pattern;
    private final int mask;

    public IdDataMaskPattern(Extent extent, Pattern parent, int mask) {
        this.extent = extent;
        this.pattern = parent;
        this.mask = mask;
    }

    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock oldBlock = extent.getBlock(position);
        BaseBlock newBlock = pattern.apply(position);
        int oldData = oldBlock.getData();
        int newData = newBlock.getData() + oldData - (oldData & mask);
        return FaweCache.getBlock(newBlock.getId(), newData);
    }
}
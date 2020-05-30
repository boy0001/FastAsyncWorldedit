package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;

public class IdDataMaskPattern extends AbstractExtentPattern {
    private final Pattern pattern;
    private final int bitMask;

    public IdDataMaskPattern(Extent extent, Pattern parent, int bitMask) {
        super(extent);
        this.pattern = parent;
        this.bitMask = bitMask;
    }

    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock oldBlock = getExtent().getBlock(position);
        BaseBlock newBlock = pattern.apply(position);
        int oldData = oldBlock.getData();
        int newData = newBlock.getData() + oldData - (oldData & bitMask);
        return FaweCache.getBlock(newBlock.getId(), newData);
    }
}
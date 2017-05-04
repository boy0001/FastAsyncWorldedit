package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;


import static com.google.common.base.Preconditions.checkNotNull;

public class DataPattern extends AbstractPattern {
    private final Extent extent;
    private final Pattern pattern;

    public DataPattern(Extent extent, Pattern parent) {
        checkNotNull(extent);
        checkNotNull(parent);
        this.extent = extent;
        this.pattern = parent;
    }

    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock oldBlock = extent.getBlock(position);
        BaseBlock newBlock = pattern.apply(position);
        return FaweCache.getBlock(oldBlock.getId(), newBlock.getData());
    }
}

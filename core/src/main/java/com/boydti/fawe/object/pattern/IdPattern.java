package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class IdPattern extends AbstractPattern {
    private final Extent extent;
    private final Pattern pattern;

    public IdPattern(Extent extent, Pattern parent) {
        this.extent = extent;
        this.pattern = parent;
    }

    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock oldBlock = extent.getBlock(position);
        BaseBlock newBlock = pattern.apply(position);
        return FaweCache.getBlock(newBlock.getId(), oldBlock.getData());
    }
}

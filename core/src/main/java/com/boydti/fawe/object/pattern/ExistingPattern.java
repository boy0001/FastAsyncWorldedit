package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;

public class ExistingPattern extends AbstractExtentPattern {
    public ExistingPattern(Extent extent) {
        super(extent);
    }

    @Override
    public BaseBlock apply(Vector position) {
        return getExtent().getBlock(position);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        if (set.getBlockX() == get.getBlockX() && set.getBlockZ() == get.getBlockZ() && set.getBlockY() == get.getBlockY()) {
            return false;
        }
        return extent.setBlock(set, extent.getBlock(get));
    }
}

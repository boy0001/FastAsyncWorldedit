package com.boydti.fawe.object.mask;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;

public class SurfaceMask extends AdjacentAnyMask {
    public SurfaceMask(Extent extent) {
        super(extent);
        for (int id = 0; id < 256; id++) {
            if (FaweCache.canPassThrough(id, 0)) {
                add(new BaseBlock(id, -1));
            }
        }
    }

    @Override
    public boolean test(Vector v) {
        BaseBlock block = getExtent().getBlock(v);
        return !test(block.getId()) && super.test(v);
    }
}

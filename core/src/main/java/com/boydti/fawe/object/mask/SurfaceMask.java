package com.boydti.fawe.object.mask;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;

public class SurfaceMask extends AdjacentAnyMask {
    private final Extent extent;

    public SurfaceMask(Extent extent) {
        super(new BlockMask(extent, new BaseBlock(0)));
        BlockMask mask = (BlockMask) getMask();
        for (int id = 0; id < 256; id++) {
            if (FaweCache.canPassThrough(id, 0)) {
                mask.add(new BaseBlock(id, -1));
            }
        }
        this.extent = extent;
    }

    @Override
    public boolean test(Vector v) {
        return !getMask().test(v) && super.test(v);
    }
}

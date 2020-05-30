package com.sk89q.worldedit.function.mask;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;

public abstract class ConditionalMask extends BlockMask {
    public ConditionalMask(Extent extent) {
        super(extent);
        for (BaseBlock block : FaweCache.CACHE_BLOCK) {
            if (applies(block)) {
                add(block);
            }
        }
    }

    public abstract boolean applies(BaseBlock block);
}

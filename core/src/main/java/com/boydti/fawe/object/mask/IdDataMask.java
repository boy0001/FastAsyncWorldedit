package com.boydti.fawe.object.mask;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class IdDataMask implements Mask, ResettableMask {

    private final Extent extent;

    public IdDataMask(Extent extent) {
        this.extent = extent;
    }

    int combined = -1;

    @Override
    public boolean test(Vector vector) {
        if (combined != -1) {
            return FaweCache.getCombined(extent.getLazyBlock(vector)) == combined;
        } else {
            combined = FaweCache.getCombined(extent.getLazyBlock(vector));
            return true;
        }
    }

    @Override
    public void reset() {
        this.combined = -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

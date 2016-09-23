package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class DataMask implements Mask, ResettableMask {

    private final Extent extent;

    public DataMask(Extent extent) {
        this.extent = extent;
    }

    int data = -1;

    @Override
    public boolean test(Vector vector) {
        if (data != -1) {
            return extent.getLazyBlock(vector).getData() == data;
        } else {
            data = extent.getLazyBlock(vector).getData();
            return true;
        }
    }

    @Override
    public void reset() {
        this.data = -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

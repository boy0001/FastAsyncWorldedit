package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class IdMask implements Mask, ResettableMask {

    private Extent extent;

    public IdMask(Extent extent) {
        this.extent = extent;
    }

    int id = -1;

    @Override
    public boolean test(Vector vector) {
        if (id != -1) {
            return extent.getLazyBlock(vector).getId() == id;
        } else {
            id = extent.getLazyBlock(vector).getId();
            return true;
        }
    }

    @Override
    public void reset() {
        this.id = -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

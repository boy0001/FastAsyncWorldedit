package com.boydti.fawe.object.function.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class AbstractDelegateMask implements Mask {

    private final Mask parent;

    public AbstractDelegateMask(Mask parent) {
        this.parent = parent;
    }

    @Override
    public boolean test(Vector vector) {
        return parent.test(vector);
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return parent.toMask2D();
    }
}

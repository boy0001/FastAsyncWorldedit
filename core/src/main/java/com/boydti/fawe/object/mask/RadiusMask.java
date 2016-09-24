package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class RadiusMask implements Mask, ResettableMask{

    private final int minSqr, maxSqr;

    public RadiusMask(int min, int max) {
        this.minSqr = min * min;
        this.maxSqr = max * max;
    }

    @Override
    public void reset() {
        pos = null;
    }

    private Vector pos;

    @Override
    public boolean test(Vector to) {
        if (pos == null) {
            pos = new Vector(to);
        }
        int dx = Math.abs((int) (pos.x - to.x));
        int dy = Math.abs((int) (pos.x - to.x));
        int dz = Math.abs((int) (pos.x - to.x));
        int d = dx * dx;
        if (d < minSqr || d > maxSqr) {
            return false;
        }
        d += dz * dz;
        if (d < minSqr || d > maxSqr) {
            return false;
        }
        d += dy * dy;
        if (d < minSqr || d > maxSqr) {
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

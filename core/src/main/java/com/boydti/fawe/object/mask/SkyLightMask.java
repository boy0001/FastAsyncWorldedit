package com.boydti.fawe.object.mask;

import com.boydti.fawe.object.extent.LightingExtent;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import javax.annotation.Nullable;

public class SkyLightMask implements Mask {

    private final Extent extent;
    private final int min,max;

    public SkyLightMask(Extent extent, int min, int max) {
        this.extent = extent;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(Vector vector) {
        if (extent instanceof LightingExtent) {
            int light = ((LightingExtent) extent).getSkyLight((int) vector.x, (int) vector.y, (int) vector.z);
            return light >= min && light <= max;
        }
        return false;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

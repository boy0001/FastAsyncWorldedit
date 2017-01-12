package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import java.util.Collection;

public class WallMask extends BlockMask {
    private final int min, max;

    public WallMask(Extent extent, Collection<BaseBlock> blocks, int requiredMin, int requiredMax) {
        super(extent, blocks);
        this.min = requiredMin;
        this.max = requiredMax;
    }

    @Override
    public boolean test(Vector v) {
        int count = 0;
        double x = v.getX();
        double y = v.getY();
        double z = v.getZ();
        v.mutX(x + 1);
        if (super.test(v) && ++count == min && max >= 8) { v.mutX(x); return true; }
        v.mutX(x - 1);
        if (super.test(v) && ++count == min && max >= 8) { v.mutX(x); return true; }
        v.mutX(x);
        v.mutZ(z + 1);
        if (super.test(v) && ++count == min && max >= 8) { v.mutZ(z); return true; }
        v.mutZ(z - 1);
        if (super.test(v) && ++count == min && max >= 8) { v.mutZ(z); return true; }
        v.mutZ(z);
        return count >= min && count <= max;
    }
}

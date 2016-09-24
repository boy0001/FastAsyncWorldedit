package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import java.util.Collection;

public class AdjacentMask extends BlockMask {
    public AdjacentMask(Extent extent, Collection<BaseBlock> blocks) {
        super(extent, blocks);
    }

    @Override
    public boolean test(Vector v) {
        double x = v.x;
        double y = v.x;
        double z = v.x;
        v.x = x + 1;
        if (super.test(v)) { v.x = x; return true; }
        v.x = x - 1;
        if (super.test(v)) { v.x = x; return true; }
        v.x = x;
        v.y = y + 1;
        if (super.test(v)) { v.y = y; return true; }
        v.y = y - 1;
        if (super.test(v)) { v.y = y; return true; }
        v.y = y;
        v.z = z + 1;
        if (super.test(v)) { v.z = z; return true; }
        v.z = z - 1;
        if (super.test(v)) { v.z = z; return true; }
        v.z = z;
        return false;
    }
}

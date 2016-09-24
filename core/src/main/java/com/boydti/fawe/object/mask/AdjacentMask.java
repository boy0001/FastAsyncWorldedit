package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import java.util.Collection;

public class AdjacentMask extends BlockMask {
    private final int required;

    public AdjacentMask(Extent extent, Collection<BaseBlock> blocks, int required) {
        super(extent, blocks);
        this.required = required;
    }

    @Override
    public boolean test(Vector v) {
        int count = 0;
        double x = v.x;
        double y = v.y;
        double z = v.z;
        v.x = x + 1;
        if (super.test(v) && ++count == required) { v.x = x; return true; }
        v.x = x - 1;
        if (super.test(v) && ++count == required) { v.x = x; return true; }
        v.x = x;
        v.y = y + 1;
        if (super.test(v) && ++count == required) { v.y = y; return true; }
        v.y = y - 1;
        if (super.test(v) && ++count == required) { v.y = y; return true; }
        v.y = y;
        v.z = z + 1;
        if (super.test(v) && ++count == required) { v.z = z; return true; }
        v.z = z - 1;
        if (super.test(v) && ++count == required) { v.z = z; return true; }
        v.z = z;
        return false;
    }
}

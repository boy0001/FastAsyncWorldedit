package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import java.util.Collection;

/**
 * Just an optimized version of the Adjacent Mask for single adjacency
 */
public class AdjacentAnyMask extends BlockMask {
    public AdjacentAnyMask(Extent extent, Collection<BaseBlock> blocks) {
        super(extent, blocks);
    }

    @Override
    public boolean test(Vector v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        v.mutY(y + 1);
        if (super.test(v)) { v.mutY(y); return true; }
        v.mutY(y - 1);
        if (super.test(v)) { v.mutY(y); return true; }
        v.mutY(y);
        v.mutX(x + 1);
        if (super.test(v)) { v.mutX(x); return true; }
        v.mutX(x - 1);
        if (super.test(v)) { v.mutX(x); return true; }
        v.mutX(x);
        v.mutZ(z + 1);
        if (super.test(v)) { v.mutZ(z); return true; }
        v.mutZ(z - 1);
        if (super.test(v)) { v.mutZ(z); return true; }
        v.mutZ(z);
        return false;
    }

    public Vector direction(Vector v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        v.mutY(y + 1);
        if (super.test(v)) { v.mutY(y); return MutableBlockVector.get(0, 1, 0); }
        v.mutY(y - 1);
        if (super.test(v)) { v.mutY(y); return MutableBlockVector.get(0, -1, 0); }
        v.mutY(y);
        v.mutX(x + 1);
        if (super.test(v)) { v.mutX(x); return MutableBlockVector.get(1, 0, 0); }
        v.mutX(x - 1);
        if (super.test(v)) { v.mutX(x); return MutableBlockVector.get(-1, 0, 0); }
        v.mutX(x);
        v.mutZ(z + 1);
        if (super.test(v)) { v.mutZ(z); return MutableBlockVector.get(0, 0, 1); }
        v.mutZ(z - 1);
        if (super.test(v)) { v.mutZ(z); return MutableBlockVector.get(0, 0, - 1); }
        v.mutZ(z);
        return null;
    }
}
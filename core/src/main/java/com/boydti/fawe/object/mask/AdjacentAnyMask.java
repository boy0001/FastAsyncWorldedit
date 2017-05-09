package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.mask.Mask;

/**
 * Just an optimized version of the Adjacent Mask for single adjacency
 */
public class AdjacentAnyMask implements Mask {

    private final Mask mask;
    private MutableBlockVector mutable = new MutableBlockVector();

    public AdjacentAnyMask(Mask mask) {
        this.mask = mask;
    }

    public Mask getMask() {
        return mask;
    }

    @Override
    public boolean test(Vector v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        v.mutY(y + 1);
        if (mask.test(v)) { v.mutY(y); return true; }
        v.mutY(y - 1);
        if (mask.test(v)) { v.mutY(y); return true; }
        v.mutY(y);
        v.mutX(x + 1);
        if (mask.test(v)) { v.mutX(x); return true; }
        v.mutX(x - 1);
        if (mask.test(v)) { v.mutX(x); return true; }
        v.mutX(x);
        v.mutZ(z + 1);
        if (mask.test(v)) { v.mutZ(z); return true; }
        v.mutZ(z - 1);
        if (mask.test(v)) { v.mutZ(z); return true; }
        v.mutZ(z);
        return false;
    }

    public Vector direction(Vector v) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        v.mutY(y + 1);
        if (mask.test(v)) { v.mutY(y); return mutable.setComponents(0, 1, 0); }
        v.mutY(y - 1);
        if (mask.test(v)) { v.mutY(y); return mutable.setComponents(0, -1, 0); }
        v.mutY(y);
        v.mutX(x + 1);
        if (mask.test(v)) { v.mutX(x); return mutable.setComponents(1, 0, 0); }
        v.mutX(x - 1);
        if (mask.test(v)) { v.mutX(x); return mutable.setComponents(-1, 0, 0); }
        v.mutX(x);
        v.mutZ(z + 1);
        if (mask.test(v)) { v.mutZ(z); return mutable.setComponents(0, 0, 1); }
        v.mutZ(z - 1);
        if (mask.test(v)) { v.mutZ(z); return mutable.setComponents(0, 0, - 1); }
        v.mutZ(z);
        return null;
    }
}
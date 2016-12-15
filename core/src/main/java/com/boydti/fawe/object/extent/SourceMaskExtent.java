package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;


import static com.google.common.base.Preconditions.checkNotNull;

public class SourceMaskExtent extends TemporalExtent {
    private Mask mask;
    private Vector mutable = new Vector();


    /**
     * Get the mask.
     *
     * @return the mask
     */
    public Mask getMask() {
        return mask;
    }

    /**
     * Set a mask.
     *
     * @param mask a mask
     */
    public void setMask(Mask mask) {
        checkNotNull(mask);
        this.mask = mask;
    }

    public SourceMaskExtent(Extent extent, Mask mask) {
        super(extent);
        checkNotNull(mask);
        this.mask = mask;
    }
    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        set((int) location.x, (int) location.y, (int) location.z, block);
        return mask.test(location) && super.setBlock(location, block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        set(x, y, z, block);
        mutable.x = x;
        mutable.y = y;
        mutable.z = z;
        return mask.test(mutable) && super.setBlock(x, y, z, block);
    }
}

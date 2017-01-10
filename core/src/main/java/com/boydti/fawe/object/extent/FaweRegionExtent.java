package com.boydti.fawe.object.extent;

import com.boydti.fawe.object.RegionWrapper;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import java.util.Collection;

public abstract class FaweRegionExtent extends AbstractDelegateExtent {
    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public FaweRegionExtent(Extent extent) {
        super(extent);
    }

    public abstract boolean contains(int x, int y, int z);

    public abstract Collection<RegionWrapper> getRegions();
}

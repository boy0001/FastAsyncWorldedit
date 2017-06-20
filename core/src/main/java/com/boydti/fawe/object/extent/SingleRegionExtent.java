package com.boydti.fawe.object.extent;

import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.RegionWrapper;
import com.sk89q.worldedit.extent.Extent;
import java.util.Arrays;
import java.util.Collection;

public class SingleRegionExtent extends FaweRegionExtent {

    private final RegionWrapper region;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public SingleRegionExtent(Extent extent, FaweLimit limit, RegionWrapper region) {
        super(extent, limit);
        this.region = region;
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return region.isIn(x, y, z);
    }

    @Override
    public boolean contains(int x, int z) {
        return region.isIn(x, z);
    }

    @Override
    public Collection<RegionWrapper> getRegions() {
        return Arrays.asList(region);
    }
}

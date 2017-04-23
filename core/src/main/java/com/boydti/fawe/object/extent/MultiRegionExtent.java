package com.boydti.fawe.object.extent;

import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.RegionWrapper;
import com.sk89q.worldedit.extent.Extent;
import java.util.Arrays;
import java.util.Collection;

public class MultiRegionExtent extends FaweRegionExtent {

    private RegionWrapper region;
    private final RegionWrapper[] regions;
    private int index;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public MultiRegionExtent(Extent extent, FaweLimit limit, RegionWrapper[] regions) {
        super(extent, limit);
        this.index = 0;
        this.region = regions[0];
        this.regions = regions;
    }

    @Override
    public boolean contains(int x, int y, int z) {
        if (region.isIn(x, y, z)) {
            return true;
        }
        for (int i = 0; i < regions.length; i++) {
            if (i != index) {
                RegionWrapper current = regions[i];
                if (current.isIn(x, y, z)) {
                    region = current;
                    index = i;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean contains(int x, int z) {
        if (region.isIn(x, z)) {
            return true;
        }
        for (int i = 0; i < regions.length; i++) {
            if (i != index) {
                RegionWrapper current = regions[i];
                if (current.isIn(x, z)) {
                    region = current;
                    index = i;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Collection<RegionWrapper> getRegions() {
        return Arrays.asList(regions);
    }
}

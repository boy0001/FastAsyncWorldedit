package com.boydti.fawe.regions.general;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RegionWrapper;
import java.util.HashSet;

public class PlotSquaredFeature extends FaweMaskManager {
    public PlotSquaredFeature() {
        super("PlotSquared");
        PS.get().worldedit = null;
    }

    @Override
    public FaweMask getMask(FawePlayer fp) {
        final PlotPlayer pp = PlotPlayer.wrap(fp.parent);
        Plot plot = pp.getCurrentPlot();
        Location loc = pp.getLocation();
        final String world = loc.getWorld();
        if (plot == null) {
            int min = Integer.MAX_VALUE;
            for (final Plot p : pp.getPlots()) {
                if (p.getArea().worldname.equals(world)) {
                    final double d = p.getHome().getEuclideanDistanceSquared(loc);
                    if (d < min) {
                        min = (int) d;
                        plot = p;
                    }
                }
            }
        }
        if (plot != null) {
            final PlotId id = plot.getId();
            boolean hasPerm = false;
            if (plot.owner != null) {
                if (plot.owner.equals(pp.getUUID())) {
                    hasPerm = true;
                } else if (plot.isAdded(pp.getUUID()) && pp.hasPermission("fawe.plotsquared.member")) {
                    hasPerm = true;
                }
                if (hasPerm) {
                    RegionWrapper region = plot.getLargestRegion();
                    HashSet<RegionWrapper> regions = plot.getRegions();

                    final Location pos1 = new Location(world, region.minX, 0, region.minZ);
                    final Location pos2 = new Location(world, region.maxX, 256, region.maxZ);

                    final HashSet<com.boydti.fawe.object.RegionWrapper> faweRegions = new HashSet<com.boydti.fawe.object.RegionWrapper>();
                    for (final com.intellectualcrafters.plot.object.RegionWrapper current : regions) {
                        faweRegions.add(new com.boydti.fawe.object.RegionWrapper(current.minX, current.maxX, current.minZ, current.maxZ));
                    }
                    return new BukkitMask(pos1, pos2) {
                        @Override
                        public String getName() {
                            return "PLOT^2:" + id;
                        }

                        @Override
                        public HashSet<com.boydti.fawe.object.RegionWrapper> getRegions() {
                            return faweRegions;
                        }
                    };
                }
            }
        }
        return null;
    }
}

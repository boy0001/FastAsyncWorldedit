package com.boydti.fawe.regions.general;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RegionWrapper;
import com.plotsquared.listener.WEManager;
import com.sk89q.worldedit.BlockVector;
import java.util.HashSet;
import org.bukkit.entity.Player;

public class PlotSquaredFeature extends FaweMaskManager {
    public PlotSquaredFeature() {
        super("PlotSquared");
        PS.get().worldedit = null;
    }

    @Override
    public FaweMask getMask(FawePlayer fp) {
        final PlotPlayer pp = PlotPlayer.wrap((Player) fp.parent);
        final HashSet<RegionWrapper> regions = WEManager.getMask(pp);
        if (regions.size() == 0) {
            Plot plot = pp.getCurrentPlot();
            if (plot != null && plot.isOwner(pp.getUUID())) {
                System.out.println("INVALID MASK? " + WEManager.getMask(pp) + " | " + plot + " | " + pp.getName());
            }
            return null;
        }
        final HashSet<com.boydti.fawe.object.RegionWrapper> faweRegions = new HashSet<>();
        for (final RegionWrapper current : regions) {
            faweRegions.add(new com.boydti.fawe.object.RegionWrapper(current.minX, current.maxX, current.minZ, current.maxZ));
        }
        final RegionWrapper region = regions.iterator().next();
        final BlockVector pos1 = new BlockVector(region.minX, 0, region.minZ);
        final BlockVector pos2 = new BlockVector(region.maxX, 256, region.maxZ);
        return new FaweMask(pos1, pos2) {
            @Override
            public String getName() {
                return "PLOT^2";
            }

            @Override
            public boolean contains(BlockVector loc) {
                return WEManager.maskContains(regions, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            }

            @Override
            public HashSet<com.boydti.fawe.object.RegionWrapper> getRegions() {
                return faweRegions;
            }
        };
    }
}

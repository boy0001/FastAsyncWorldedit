package com.boydti.fawe.bukkit.regions;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.plotsquared.bukkit.BukkitMain;

public class PlotSquaredFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;

    public PlotSquaredFeature(final Plugin plotPlugin, final FaweBukkit p3) {
        super(plotPlugin.getName());
        this.plugin = p3;
        BukkitMain.worldEdit = null;
        PS.get().worldedit = null;
    }

    @Override
    public FaweMask getMask(final FawePlayer<Player> fp) {
        final PlotPlayer pp = PlotPlayer.wrap(fp.parent);
        Plot plot = pp.getCurrentPlot();
        if (plot == null) {
            final com.intellectualcrafters.plot.object.Location loc = pp.getLocation();
            final String world = loc.getWorld();
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
                    final World world = fp.parent.getWorld();
                    final com.intellectualcrafters.plot.object.RegionWrapper region = plot.getLargestRegion();
                    final HashSet<com.intellectualcrafters.plot.object.RegionWrapper> regions = plot.getRegions();

                    final Location pos1 = new Location(world, region.minX, 0, region.minZ);
                    final Location pos2 = new Location(world, region.maxX, 256, region.maxZ);

                    final HashSet<RegionWrapper> faweRegions = new HashSet<RegionWrapper>();
                    for (final com.intellectualcrafters.plot.object.RegionWrapper current : regions) {
                        faweRegions.add(new RegionWrapper(current.minX, current.maxX, current.minZ, current.maxZ));
                    }
                    return new FaweMask(pos1, pos2) {
                        @Override
                        public String getName() {
                            return "PLOT^2:" + id;
                        }

                        @Override
                        public HashSet<RegionWrapper> getRegions() {
                            return faweRegions;
                        }
                    };
                }
            }
        }
        return null;
    }
}

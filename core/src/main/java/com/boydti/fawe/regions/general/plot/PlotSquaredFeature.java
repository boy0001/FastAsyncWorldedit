package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.generator.HybridPlotManager;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RegionWrapper;
import com.intellectualcrafters.plot.util.ChunkManager;
import com.intellectualcrafters.plot.util.SchematicHandler;
import com.intellectualcrafters.plot.util.block.GlobalBlockQueue;
import com.intellectualcrafters.plot.util.block.QueueProvider;
import com.plotsquared.listener.WEManager;
import com.sk89q.worldedit.BlockVector;
import java.util.HashSet;

public class PlotSquaredFeature extends FaweMaskManager {
    public PlotSquaredFeature() {
        super("PlotSquared");
        Fawe.debug("Optimizing PlotSquared");
        PS.get().worldedit = null;
        setupBlockQueue();
        setupSchematicHandler();
        setupChunkManager();
        if (Settings.PLATFORM.equals("Bukkit")) {
            new FaweTrim();
        }
        if (MainCommand.getInstance().getCommand("generatebiome") == null) {
            new PlotSetBiome();
        }
    }

    private void setupBlockQueue() {
        try {
            // If it's going to fail, throw an error now rather than later
            QueueProvider provider = QueueProvider.of(FaweLocalBlockQueue.class, null);
            GlobalBlockQueue.IMP.setProvider(provider);
            HybridPlotManager.REGENERATIVE_CLEAR = false;
            Fawe.debug(" - QueueProvider: " + FaweLocalBlockQueue.class);
            Fawe.debug(" - HybridPlotManager.REGENERATIVE_CLEAR: " + HybridPlotManager.REGENERATIVE_CLEAR);
        } catch (Throwable e) {
            Fawe.debug("Please update PlotSquared: http://ci.athion.net/job/PlotSquared/");
        }
    }

    private void setupChunkManager() {
        try {
            ChunkManager.manager = new FaweChunkManager(ChunkManager.manager);
            Fawe.debug(" - ChunkManager: " + ChunkManager.manager);
        } catch (Throwable e) {
            Fawe.debug("Please update PlotSquared: http://ci.athion.net/job/PlotSquared/");
        }
    }

    private void setupSchematicHandler() {
        try {
            SchematicHandler.manager = new FaweSchematicHandler();
            Fawe.debug(" - SchematicHandler: " + SchematicHandler.manager);
        } catch (Throwable e) {
            Fawe.debug("Please update PlotSquared: http://ci.athion.net/job/PlotSquared/");
        }
    }

    @Override
    public FaweMask getMask(FawePlayer fp, MaskType type) {
        final PlotPlayer pp = PlotPlayer.wrap(fp.parent);
        final HashSet<RegionWrapper> regions;
        Plot plot = pp.getCurrentPlot();
        PlotArea area = pp.getApplicablePlotArea();
        if (plot != null && (plot.isOwner(pp.getUUID()) || (type == MaskType.MEMBER && (plot.getTrusted().contains(pp.getUUID()) || (plot.getMembers().contains(pp.getUUID()) && pp.hasPermission("fawe.plotsquared.member")))))) {
            regions = plot.getRegions();
        } else {
            regions = WEManager.getMask(pp);
        }
        if (regions.size() == 0) {
            return null;
        }
        final HashSet<com.boydti.fawe.object.RegionWrapper> faweRegions = new HashSet<>();
        for (final RegionWrapper current : regions) {
            faweRegions.add(new com.boydti.fawe.object.RegionWrapper(current.minX, current.maxX, current.minZ, current.maxZ));
        }
        int min = area != null ? area.MIN_BUILD_HEIGHT : 0;
        int max = area != null ? area.MAX_BUILD_HEIGHT : 255;
        final RegionWrapper region = regions.iterator().next();
        final BlockVector pos1 = new BlockVector(region.minX, min, region.minZ);
        final BlockVector pos2 = new BlockVector(region.maxX, max, region.maxZ);
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

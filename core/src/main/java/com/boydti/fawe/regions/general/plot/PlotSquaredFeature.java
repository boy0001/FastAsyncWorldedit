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
import java.util.UUID;

public class PlotSquaredFeature extends FaweMaskManager {
    public PlotSquaredFeature() {
        super("PlotSquared");
        Fawe.debug("Optimizing PlotSquared");
        PS.get().worldedit = null;
        setupBlockQueue();
        setupSchematicHandler();
        setupChunkManager();
        if (Settings.PLATFORM.equalsIgnoreCase("bukkit")) {
            new FaweTrim();
        }
        if (MainCommand.getInstance().getCommand("generatebiome") == null) {
            new PlotSetBiome();
        }
        try {
            new MoveTo512();
            if (Settings.Enabled_Components.WORLDS) {
                new CreateFromImage();
                new ReplaceAll();
            }
        } catch (Throwable e) {
            Fawe.debug("You need to update PlotSquared to access the CFI and REPLACEALL commands");
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

    public boolean isAllowed(FawePlayer fp, Plot plot, MaskType type) {
        if (plot == null) {
            return false;
        }
        UUID uid = fp.getUUID();
        return (plot.isOwner(uid) || (type == MaskType.MEMBER && (plot.getTrusted().contains(uid) || (plot.getMembers().contains(uid) && fp.hasPermission("fawe.plotsquared.member")))));
    }

    @Override
    public FaweMask getMask(FawePlayer fp, MaskType type) {
        final PlotPlayer pp = PlotPlayer.wrap(fp.parent);
        final HashSet<RegionWrapper> regions;
        Plot plot = pp.getCurrentPlot();
        if (isAllowed(fp, plot, type)) {
            regions = plot.getRegions();
        } else {
            plot = null;
            regions = WEManager.getMask(pp);
            if (regions.size() == 1) {
                RegionWrapper region = regions.iterator().next();
                if (region.minX == Integer.MIN_VALUE && region.maxX == Integer.MAX_VALUE) {
                    regions.clear();
                }
            }
        }
        if (regions == null || regions.size() == 0) {
            return null;
        }
        final HashSet<com.boydti.fawe.object.RegionWrapper> faweRegions = new HashSet<>();
        for (final RegionWrapper current : regions) {
            faweRegions.add(new com.boydti.fawe.object.RegionWrapper(current.minX, current.maxX, current.minZ, current.maxZ));
        }
        PlotArea area = pp.getApplicablePlotArea();
        int min = area != null ? area.MIN_BUILD_HEIGHT : 0;
        int max = area != null ? area.MAX_BUILD_HEIGHT : 255;
        final RegionWrapper region = regions.iterator().next();
        final BlockVector pos1 = new BlockVector(region.minX, min, region.minZ);
        final BlockVector pos2 = new BlockVector(region.maxX, max, region.maxZ);
        final Plot finalPlot = plot;
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
            public boolean isValid(FawePlayer player, MaskType type) {
                return isAllowed(player, finalPlot, type);
            }

            @Override
            public HashSet<com.boydti.fawe.object.RegionWrapper> getRegions() {
                return faweRegions;
            }
        };
    }
}

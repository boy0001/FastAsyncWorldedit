package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.object.RunnableVal;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.generator.HybridGen;
import com.intellectualcrafters.plot.generator.HybridPlotWorld;
import com.intellectualcrafters.plot.generator.IndependentPlotGenerator;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.util.expiry.ExpireManager;
import com.sk89q.worldguard.util.collect.LongHashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

public class PlotTrimFilter extends DeleteUninhabitedFilter {
    private final HybridPlotWorld hpw;
    private final HybridGen hg;
    private final MCAChunk reference;
    private final LongHashSet occupiedRegions;
    private final LongHashSet unoccupiedChunks;

    public static boolean shouldSuggest(PlotArea area) {
        IndependentPlotGenerator gen = area.getGenerator();
        if (area instanceof HybridPlotWorld && gen instanceof HybridGen) {
            HybridPlotWorld hpw = (HybridPlotWorld) area;
            return hpw.PLOT_BEDROCK && !hpw.PLOT_SCHEMATIC && hpw.MAIN_BLOCK.length == 1 && hpw.TOP_BLOCK.length == 1;
        }
        return false;
    }

    public PlotTrimFilter(PlotArea area, long fileAgeMillis, long inhabitedTicks) {
        super(fileAgeMillis, inhabitedTicks);
        IndependentPlotGenerator gen = area.getGenerator();
        if (!(area instanceof HybridPlotWorld) || !(gen instanceof HybridGen)) {
            throw new UnsupportedOperationException("Trim does not support non hybrid plot worlds");
        }
        this.hg = (HybridGen) gen;
        this.hpw = (HybridPlotWorld) area;

        if (hpw.PLOT_BEDROCK && !hpw.PLOT_SCHEMATIC && hpw.MAIN_BLOCK.length == 1  && hpw.TOP_BLOCK.length == 1) {
            this.reference = new MCAChunk(null, 0, 0);
            this.reference.fillCuboid(0, 15, 0, 0, 0, 15, 7, (byte) 0);
            this.reference.fillCuboid(0, 15, 1, hpw.PLOT_HEIGHT - 1, 0, 15, hpw.MAIN_BLOCK[0].id, (byte) 0);
            this.reference.fillCuboid(0, 15, hpw.PLOT_HEIGHT, hpw.PLOT_HEIGHT, 0, 15, hpw.TOP_BLOCK[0].id, (byte) 0);
        } else {
            this.reference = null;
        }
        this.occupiedRegions = new LongHashSet();
        this.unoccupiedChunks = new LongHashSet();
        ArrayList<Plot> plots = new ArrayList<>();
        plots.addAll(PS.get().getPlots(area));
        if (ExpireManager.IMP != null) {
            plots.removeAll(ExpireManager.IMP.getPendingExpired());
        }
        for (Plot plot : plots) {
            Location pos1 = plot.getBottom();
            Location pos2 = plot.getTop();
            int ccx1 = pos1.getX() >> 9;
            int ccz1 = pos1.getZ() >> 9;
            int ccx2 = pos2.getX() >> 9;
            int ccz2 = pos2.getZ() >> 9;
            for (int x = ccx1; x <= ccx2; x++) {
                for (int z = ccz1; z <= ccz2; z++) {
                    int bcx = x << 5;
                    int bcz = z << 5;
                    int tcx = bcx + 32;
                    int tcz = bcz + 32;
                    if (!occupiedRegions.containsKey(x, z)) {
                        occupiedRegions.add(x, z);
                    } else {
                        for (int cz = bcz; cz < tcz; cz++) {
                            for (int cx = bcx; cx < tcx; cx++) {
                                unoccupiedChunks.add(cx, cz);
                            }
                        }
                    }
                }
            }
            int cx1 = pos1.getX() >> 4;
            int cz1 = pos1.getZ() >> 4;
            int cx2 = pos2.getX() >> 4;
            int cz2 = pos2.getZ() >> 4;
            for (int cz = cx1; cz < cx2; cz++) {
                for (int cx = cz1; cx < cz2; cx++) {
                    unoccupiedChunks.remove(cx, cz);
                }
            }
        }
    }

    @Override
    public boolean shouldDelete(MCAFile mca) throws IOException {
        int x = mca.getX();
        int z = mca.getZ();
        return !occupiedRegions.containsKey(x, z) || super.shouldDelete(mca);
    }

    @Override
    public boolean shouldDeleteChunk(MCAFile mca, int cx, int cz) {
        return !unoccupiedChunks.containsKey(cx, cz);
    }

    @Override
    public void filter(MCAFile mca, ForkJoinPool pool) throws IOException {
        if (reference == null) {
            super.filter(mca, pool);
            return;
        }
        mca.forEachChunk(new RunnableVal<MCAChunk>() {
            @Override
            public void run(MCAChunk value) {
                if (value.getInhabitedTime() < getInhabitedTicks()) {
                    value.setDeleted(true);
                    return;
                }
                if (reference.idsEqual(value, false)) {
                    value.setDeleted(true);
                    return;
                }
            }
        });
    }
}
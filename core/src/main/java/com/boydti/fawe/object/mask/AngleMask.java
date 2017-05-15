package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import javax.annotation.Nullable;

public class AngleMask extends SolidBlockMask implements ResettableMask {
    public static double ADJACENT_MOD = 0.5;
    public static double DIAGONAL_MOD = 1 / Math.sqrt(8);

    private final double max;
    private final double min;
    private final boolean overlay;
    private int maxY;

    private transient MutableBlockVector mutable = new MutableBlockVector();

    public AngleMask(Extent extent, double min, double max, boolean overlay) {
        super(extent);
        this.min = min;
        this.max = max;
        this.maxY = extent.getMaximumPoint().getBlockY();
        this.overlay = overlay;
    }

    @Override
    public void reset() {
        mutable = new MutableBlockVector();
    }

    @Override
    public boolean test(Vector vector) {
        int x = vector.getBlockX();
        int y = vector.getBlockY();
        int z = vector.getBlockZ();
        BaseBlock block = getExtent().getLazyBlock(x, y, z);
        if (!test(block.getId(), block.getData())) {
            return false;
        }
        block = getExtent().getLazyBlock(x, y + 1, z);
        if (overlay && test(block.getId(), block.getData())) {
            return false;
        }
        double slope;
        boolean aboveMin;
        slope = Math.abs(getExtent().getNearestSurfaceTerrainBlock(x + 1, z, y, 0, maxY) - getExtent().getNearestSurfaceTerrainBlock(x - 1, z, y, 0, maxY)) * ADJACENT_MOD;
        if (slope >= min && max >= Math.max(maxY - y, y)) {
            return true;
        }
        slope = Math.max(slope, Math.abs(getExtent().getNearestSurfaceTerrainBlock(x, z + 1, y, 0, maxY) - getExtent().getNearestSurfaceTerrainBlock(x, z - 1, y, 0, maxY)) * ADJACENT_MOD);
        slope = Math.max(slope, Math.abs(getExtent().getNearestSurfaceTerrainBlock(x + 1, z + 1, y, 0, maxY) - getExtent().getNearestSurfaceTerrainBlock(x - 1, z - 1, y, 0, maxY)) * DIAGONAL_MOD);
        slope = Math.max(slope, Math.abs(getExtent().getNearestSurfaceTerrainBlock(x - 1, z + 1, y, 0, maxY) - getExtent().getNearestSurfaceTerrainBlock(x + 1, z - 1, y, 0, maxY)) * DIAGONAL_MOD);
        return (slope >= min && slope <= max);
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

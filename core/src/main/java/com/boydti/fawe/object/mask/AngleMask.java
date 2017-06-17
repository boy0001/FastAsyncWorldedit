package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import java.util.Arrays;
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
        cacheBotX = Integer.MIN_VALUE;
        cacheBotZ = Integer.MIN_VALUE;
        if (cacheHeights != null) {
            Arrays.fill(cacheHeights, (byte) 0);
        }
    }

    private transient int cacheCenX;
    private transient int cacheCenZ;
    private transient int cacheBotX = Integer.MIN_VALUE;
    private transient int cacheBotZ = Integer.MIN_VALUE;
    private transient int cacheCenterZ;
    private transient byte[] cacheHeights = null;
    private transient int lastY = 0;

    public int getHeight(int x, int y, int z) {
        try {
            int rx = x - cacheBotX;
            int rz = z - cacheBotZ;
            int index = rx + (rz << 8);
            if (index < 0 || index >= 65536) {
                cacheBotX = x - 16;
                cacheBotZ = z - 16;
                rx = x - cacheBotX;
                rz = z - cacheBotZ;
                index = rx + (rz << 8);
                if (cacheHeights == null) {
                    cacheHeights = new byte[65536];
                } else {
                    Arrays.fill(cacheHeights, (byte) 0);
                }
            }
            int result = cacheHeights[index] & 0xFF;
            if (result == 0) {
                cacheHeights[index] = (byte) (result = lastY = getExtent().getNearestSurfaceTerrainBlock(x, z, lastY, 0, maxY));
            }
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
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
        slope = Math.abs(getHeight(x + 1, y, z) - getHeight(x - 1, y, z)) * ADJACENT_MOD;
        if (slope >= min && max >= Math.max(maxY - y, y)) {
            return true;
        }
        slope = Math.max(slope, Math.abs(getHeight(x, y, z + 1) - getHeight(x, y, z - 1)) * ADJACENT_MOD);
        slope = Math.max(slope, Math.abs(getHeight(x + 1, y, z + 1) - getHeight(x - 1, y, z - 1)) * DIAGONAL_MOD);
        slope = Math.max(slope, Math.abs(getHeight(x - 1, y, z + 1) - getHeight(x + 1, y, z - 1)) * DIAGONAL_MOD);
        return (slope >= min && slope <= max);
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

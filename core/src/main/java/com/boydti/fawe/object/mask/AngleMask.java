package com.boydti.fawe.object.mask;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import java.util.HashMap;
import javax.annotation.Nullable;

public class AngleMask extends SolidBlockMask implements ResettableMask {
    private final int max;
    private final int min;
    private int maxY;

    public AngleMask(Extent extent, int min, int max) {
        super(extent);
        this.maxY = extent.getMaximumPoint().getBlockY();
        this.min = min;
        this.max = max;
    }

    private HashMap<Long, Integer> heights = new HashMap<>();
    private long tick = 0;

    @Override
    public boolean test(Vector vector) {
        return testAngle(vector) && getExtent().getLazyBlock(vector).getId() != 0;
    }

    private boolean testAngle(Vector vector) {
        long currentTick = Fawe.get().getTimer().getTick();
        if (tick != (tick = currentTick)) {
            heights.clear();
            tick = currentTick;
        }
        return getAngle(vector);
    }



    @Override
    public void reset() {
        this.heights.clear();
    }

    public boolean getAngle(Vector vector) {
        int x = vector.getBlockX();
        int z = vector.getBlockZ();
//        int o = getHighestTerrainBlock(x, z, 0, maxY);
        int y = vector.getBlockY();
        if (getHighestTerrainBlock(x - 1, z, y + min, y + max) != -1) {
            return true;
        }
        if (getHighestTerrainBlock(x + 1, z, y + min, y + max) != -1) {
            return true;
        }
        if (getHighestTerrainBlock(x, z - 1, y + min, y + max) != -1) {
            return true;
        }
        if (getHighestTerrainBlock(x, z + 1, y + min, y + max) != -1) {
            return true;
        }
        return false;
    }

    private Vector mutable = new Vector();

    private int getHighestTerrainBlock(final int x, final int z, int minY, int maxY) {
        long pair = MathMan.pairInt(x, z);
        Integer height = heights.get(pair);
        if (height != null) {
            if (height >= minY && height <= maxY && height >= 0 && height <= this.maxY) {
                return height;
            } else {
                return -1;
            }
        }
        int maxSearchY = Math.min(this.maxY, Math.max(0, maxY));
        int minSearchY = Math.min(this.maxY, Math.max(0, minY));
        mutable.x = x;
        mutable.z = z;
        boolean air = false;
        if (maxSearchY != this.maxY) {
            mutable.y = maxSearchY + 1;
            air = !super.test(mutable);
        }
        for (int y = maxSearchY; y >= minSearchY; --y) {
            mutable.y = y;
            if (super.test(mutable)) {
                if (!air) {
                    break;
                }
                heights.put(pair, y);
                return y;

            } else {
                air = true;
            }
        }
        if (minSearchY == 0 && maxSearchY == this.maxY) {
            heights.put(pair, -1);
        } else {
            int value = getHighestTerrainBlock(x, z, 0, this.maxY);
            heights.put(pair, value);
        }
        return -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

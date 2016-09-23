package com.boydti.fawe.object.mask;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import java.util.HashMap;
import javax.annotation.Nullable;

public class AngleMask implements Mask, ResettableMask {
    private final Extent extent;
    private final int max;
    private final int min;
    private int maxY;

    public AngleMask(Extent extent, int min, int max) {
        this.extent = extent;
        this.maxY = extent.getMaximumPoint().getBlockY();
        this.min = min;
        this.max = max;
    }

    private HashMap<Long, Boolean> angles = new HashMap<>();
    private long tick = 0;

    @Override
    public boolean test(Vector vector) {
        long currentTick = Fawe.get().getTimer().getTick();
        if (tick != (tick = currentTick)) {
            angles.clear();
            tick = currentTick;
        }
        long pair = MathMan.pairInt(vector.getBlockX(), vector.getBlockZ());
        if (!angles.isEmpty()) {
            Boolean value = angles.get(pair);
            if (value != null) {
                return value;
            }
        }
        boolean result = getAngle(vector);
        angles.put(pair, result);
        return result;
    }

    @Override
    public void reset() {
        this.angles.clear();
    }

    public boolean getAngle(Vector vector) {
        int x = vector.getBlockX();
        int z = vector.getBlockZ();
        boolean n = false;
        int o = getHighestTerrainBlock(x, z, 0, maxY, n);
        if (getHighestTerrainBlock(x - 1, z, o - min, o - max, n) != -1) {
            return true;
        }
        if (getHighestTerrainBlock(x + 1, z, o - min, o - max, n) != -1) {
            return true;
        }
        if (getHighestTerrainBlock(x, z - 1, o - min, o - max, n) != -1) {
            return true;
        }
        if (getHighestTerrainBlock(x, z + 1, o - min, o - max, n) != -1) {
            return true;
        }
        return false;
    }

    private Vector mutable = new Vector();

    private int getHighestTerrainBlock(final int x, final int z, int minY, int maxY, final boolean naturalOnly) {
        maxY = Math.min(this.maxY, Math.max(0, maxY));
        minY = Math.max(0, minY);
        mutable.x = x;
        mutable.z = z;
        for (int y = maxY; y >= minY; --y) {
            mutable.y = y;
            BaseBlock block = extent.getLazyBlock(mutable);
            final int id = block.getId();
            int data;
            switch (id) {
                case 0: {
                    continue;
                }
                case 2:
                case 4:
                case 13:
                case 14:
                case 15:
                case 20:
                case 21:
                case 22:
                case 25:
                case 30:
                case 32:
                case 37:
                case 39:
                case 40:
                case 41:
                case 42:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 51:
                case 52:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 60:
                case 61:
                case 62:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 73:
                case 74:
                case 78:
                case 79:
                case 80:
                case 81:
                case 82:
                case 83:
                case 84:
                case 85:
                case 87:
                case 88:
                case 101:
                case 102:
                case 103:
                case 110:
                case 112:
                case 113:
                case 117:
                case 121:
                case 122:
                case 123:
                case 124:
                case 129:
                case 133:
                case 138:
                case 137:
                case 140:
                case 165:
                case 166:
                case 169:
                case 170:
                case 172:
                case 173:
                case 174:
                case 176:
                case 177:
                case 181:
                case 182:
                case 188:
                case 189:
                case 190:
                case 191:
                case 192:
                    return y;
                default:
                    data = 0;
            }
            if (naturalOnly ? BlockType.isNaturalTerrainBlock(id, data) : !BlockType.canPassThrough(id, data)) {
                return y;
            }
        }
        return -1;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}

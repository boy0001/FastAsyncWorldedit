package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.io.InputStream;

public class StencilBrush extends HeightBrush {
    private final boolean onlyWhite;
    private final int depth;

    public StencilBrush(InputStream stream, int depth, int rotation, double yscale, boolean onlyWhite, Clipboard clipboard) {
        super(stream, rotation, yscale, clipboard);
        this.onlyWhite = onlyWhite;
        this.depth = depth;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        int size = (int) sizeDouble;
        Mask mask = new ExistingBlockMask(editSession);
        int maxY = editSession.getMaxY();
        double scale = (yscale / sizeDouble) * (maxY + 1);
        heightMap.setSize(size);
        int cutoff = onlyWhite ? maxY : 0;

        for (int x = -size; x <= size; x++) {
            int xx = position.getBlockX() + x;
            for (int z = -size; z <= size; z++) {
                double raise;
                switch (rotation) {
                    default:raise = heightMap.getHeight(x, z); break;
                    case 1: raise = heightMap.getHeight(z, x); break;
                    case 2: raise = heightMap.getHeight(-x, -z); break;
                    case 3: raise = heightMap.getHeight(-z, -x);break;
                }
                int val = (int) Math.ceil(raise * scale);
                if (val <= cutoff) {
                    continue;
                }
                if (val >= 255 || PseudoRandom.random.random(maxY) < val) {
                    int zz = position.getBlockZ() + z;
                    int y = editSession.getNearestSurfaceTerrainBlock(xx, zz, position.getBlockY(), 0, maxY);
                    for (int i = 0; i < depth; i++) {
                        editSession.setBlock(xx, y - i, zz, pattern);
                    }
                }
            }
        }
    }
}
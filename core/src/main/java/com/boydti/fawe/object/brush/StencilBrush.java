package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.io.InputStream;

public class StencilBrush extends HeightBrush {
    private final boolean onlyWhite;

    public StencilBrush(InputStream stream, int rotation, double yscale, boolean onlyWhite, Clipboard clipboard) {
        super(stream, rotation, yscale, clipboard);
        this.onlyWhite = onlyWhite;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        int size = (int) sizeDouble;
        Mask mask = editSession.getMask();
        if (mask == Masks.alwaysTrue() || mask == Masks.alwaysTrue2D()) {
            mask = null;
        }
        heightMap.setSize(size);
        for (int x = -size; x <= size; x++) {
            int xx = position.getBlockX() + x;
            for (int z = -size; z <= size; z++) {
                int zz = position.getBlockZ() + z;
                double raise;
                switch (rotation) {
                    default:raise = heightMap.getHeight(x, z); break;
                    case 1: raise = heightMap.getHeight(z, x); break;
                    case 2: raise = heightMap.getHeight(-x, -z); break;
                    case 3: raise = heightMap.getHeight(-z, -x);break;
                }
                raise *= yscale;
                if (raise == 0 || (onlyWhite && raise < 255)) {
                    continue;
                }
            }
        }


        int[] data = heightMap.generateHeightData(editSession, mask, position, size, rotation, yscale, true, false);
        int diameter = size * 2;
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
    }

}

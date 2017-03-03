package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.io.InputStream;

public class FlattenBrush extends HeightBrush {

    public FlattenBrush(InputStream stream, int rotation, double yscale, BrushTool tool, Clipboard clipboard, ScalableHeightMap.Shape shape) {
        super(stream, rotation, yscale, tool, clipboard, shape);
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        int size = (int) sizeDouble;
        Mask mask = tool.getMask();
        if (mask == Masks.alwaysTrue() || mask == Masks.alwaysTrue2D()) {
            mask = null;
        }
        heightMap.setSize(size);
        heightMap.apply(editSession, mask, position, size, rotation, yscale, true, true);
    }
}

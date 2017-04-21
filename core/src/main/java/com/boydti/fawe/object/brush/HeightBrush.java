package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.brush.heightmap.HeightMap;
import com.boydti.fawe.object.brush.heightmap.RotatableHeightMap;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.io.IOException;
import java.io.InputStream;

public class HeightBrush implements Brush {

    private HeightMap heightMap;
    private boolean randomRotate;
    public final int rotation;
    public final double yscale;
    public final boolean layers;
    public final boolean smooth;

    public HeightBrush(InputStream stream, int rotation, double yscale, boolean layers, boolean smooth, Clipboard clipboard) {
        this(stream, rotation, yscale, layers, smooth, clipboard, ScalableHeightMap.Shape.CONE);
    }

    public HeightBrush(InputStream stream, int rotation, double yscale, boolean layers, boolean smooth, Clipboard clipboard, ScalableHeightMap.Shape shape) {
        this.rotation = (rotation / 90) % 4;
        this.yscale = yscale;
        this.layers = layers;
        this.smooth = smooth;
        if (stream != null) {
            try {
                heightMap = ScalableHeightMap.fromPNG(stream);
            } catch (IOException e) {
                throw new FaweException(BBC.BRUSH_HEIGHT_INVALID);
            }
        } else if (clipboard != null) {
            heightMap = ScalableHeightMap.fromClipboard(clipboard);
        } else {
            heightMap = ScalableHeightMap.fromShape(shape);
        }
    }

    public HeightMap getHeightMap() {
        if (randomRotate) {
            if (!(heightMap instanceof RotatableHeightMap)) {
                heightMap = new RotatableHeightMap(heightMap);
            }
            RotatableHeightMap rotatable = (RotatableHeightMap) heightMap;
            rotatable.rotate(PseudoRandom.random.nextInt(360));
        }
        return heightMap;
    }

    public void setRandomRotate(boolean randomRotate) {
        this.randomRotate = randomRotate;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        int size = (int) sizeDouble;
        Mask mask = editSession.getMask();
        if (mask == Masks.alwaysTrue() || mask == Masks.alwaysTrue2D()) {
            mask = null;
        }
        HeightMap map = getHeightMap();
        map.setSize(size);
        map.perform(editSession, mask, position, size, rotation, yscale, smooth, false, layers);
    }
}

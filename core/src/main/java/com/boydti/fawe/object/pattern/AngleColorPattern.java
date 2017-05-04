package com.boydti.fawe.object.pattern;

import com.boydti.fawe.object.DataAngleMask;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;

public class AngleColorPattern extends DataAngleMask {
    private final TextureUtil util;
    private final int maxY;
    private final double factor = 1d/196;

    public AngleColorPattern(TextureUtil util, Extent extent) {
        super(extent);
        this.util = util;
        this.maxY = extent.getMaximumPoint().getBlockY();
    }

    public int getColor(int color, int slope) {
        if (slope == 0) return color;
        double newFactor = (196 - Math.min(196, slope)) * factor;
        int newRed = (int) (((color >> 16) & 0xFF) * newFactor);
        int newGreen = (int) (((color >> 8) & 0xFF) * newFactor);
        int newBlue = (int) (((color >> 0) & 0xFF) * newFactor);
        return (((color >> 24) & 0xFF) << 24) + (newRed << 16) + (newGreen << 8) + (newBlue << 0);
    }

    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock block = extent.getBlock(position);
        int slope = getSlope(block, position);
        if (slope == -1) return block;
        int color = util.getColor(block);
        if (color == 0) return block;
        int newColor = getColor(color, slope);
        return util.getNearestBlock(newColor);
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        BaseBlock block = extent.getBlock(getPosition);
        int slope = getSlope(block, getPosition);
        if (slope == -1) return false;
        int color = util.getColor(block);
        if (color == 0) return false;
        int newColor = getColor(color, slope);
        BaseBlock newBlock = util.getNearestBlock(newColor);
        if (newBlock == null) return false;
        return extent.setBlock(setPosition, newBlock);
    }
}
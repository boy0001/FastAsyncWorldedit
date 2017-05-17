package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import java.awt.Color;
import java.io.IOException;

public class AverageColorPattern extends AbstractExtentPattern {
    private transient TextureUtil util;
    private final boolean randomize;
    private final int complexity;
    private final int color;

    public AverageColorPattern(Extent extent, int color, int complexity, boolean randomize) {
        super(extent);
        this.complexity = complexity;
        this.randomize = randomize;
        this.util = Fawe.get().getCachedTextureUtil(randomize, 0, complexity);
        this.color = new Color(color).getRGB();
    }

    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock block = getExtent().getBlock(position);
        int currentColor = util.getColor(block);
        int newColor = util.averageColor(currentColor, color);
        return util.getNearestBlock(newColor);
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        BaseBlock block = extent.getBlock(getPosition);
        int currentColor = util.getColor(block);
        if (currentColor == 0) return false;
        int newColor = util.averageColor(currentColor, color);
        BaseBlock newBlock = util.getNearestBlock(newColor);
        if (newBlock.equals(block)) return false;
        return extent.setBlock(setPosition, newBlock);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        util = Fawe.get().getCachedTextureUtil(randomize, 0, complexity);
    }
}
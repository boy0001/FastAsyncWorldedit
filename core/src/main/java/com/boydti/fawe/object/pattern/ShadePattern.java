package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import java.io.IOException;


import static com.google.common.base.Preconditions.checkNotNull;

public class ShadePattern extends AbstractPattern{
    private transient TextureUtil util;
    private final boolean randomize;
    private final int complexity;
    private final Extent extent;
    private final boolean darken;

    public ShadePattern(Extent extent, boolean darken, int complexity, boolean randomize) {
        checkNotNull(extent);
        this.extent = extent;
        this.complexity = complexity;
        this.randomize = randomize;
        this.util = Fawe.get().getCachedTextureUtil(randomize, 0, complexity);
        this.darken = darken;
    }
    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock block = extent.getBlock(position);
        return darken ? util.getDarkerBlock(block) : util.getLighterBlock(block);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        util = Fawe.get().getCachedTextureUtil(randomize, 0, complexity);
    }
}
package com.boydti.fawe.object.pattern;

import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;


import static com.google.common.base.Preconditions.checkNotNull;

public class ShadePattern extends AbstractPattern{
    private final TextureUtil util;
    private final Extent extent;
    private final boolean darken;

    public ShadePattern(Extent extent, TextureUtil util, boolean darken) {
        checkNotNull(extent);
        checkNotNull(util);
        this.extent = extent;
        this.util = util;
        this.darken = darken;
    }
    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock block = extent.getBlock(position);
        return darken ? util.getDarkerBlock(block) : util.getLighterBlock(block);
    }
}
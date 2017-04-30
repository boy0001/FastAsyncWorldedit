package com.boydti.fawe.object.pattern;

import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;

public class ShadePattern extends AbstractPattern{
    private final TextureUtil util;
    private final Extent extent;

    public ShadePattern(Extent extent, TextureUtil util) {
        this.extent = extent;
        this.util = util;
    }
    @Override
    public BaseBlock apply(Vector position) {
        return null;
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        return false;
    }
}
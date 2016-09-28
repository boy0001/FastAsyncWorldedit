package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;

public class ExistingPattern extends AbstractPattern {
    private final Extent extent;

    public ExistingPattern(Extent extent) {
        this.extent = extent;
    }

    @Override
    public BaseBlock apply(Vector position) {
        return extent.getBlock(position);
    }
}

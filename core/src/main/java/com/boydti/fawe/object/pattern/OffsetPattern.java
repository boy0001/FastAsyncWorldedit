package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.pattern.AbstractPattern;

public class OffsetPattern extends AbstractPattern {

    private final int dx,dy,dz;
    private final Vector mutable = new Vector();

    public OffsetPattern(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public BaseBlock apply(Vector position) {
        mutable.x = position.x + dx;
        mutable.y = position.y + dy;
        mutable.z = position.z + dz;
        return null;
    }
}

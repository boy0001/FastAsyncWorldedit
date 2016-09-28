package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class OffsetPattern extends AbstractPattern {

    private final int dx,dy,dz;
    private final Vector mutable = new Vector();
    private final Pattern pattern;

    public OffsetPattern(Pattern pattern, int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.pattern = pattern;
    }

    @Override
    public BaseBlock apply(Vector position) {
        mutable.x = position.x + dx;
        mutable.y = position.y + dy;
        mutable.z = position.z + dz;
        return pattern.apply(mutable);
    }
}

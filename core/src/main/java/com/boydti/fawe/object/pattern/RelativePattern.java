package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class RelativePattern extends AbstractPattern implements ResettablePattern {

    private final Pattern pattern;

    public RelativePattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private Vector origin;
    private Vector mutable = new Vector();

    @Override
    public BaseBlock apply(Vector pos) {
        if (origin == null) {
            origin = new Vector(pos);
        }
        mutable.x = pos.x - origin.x;
        mutable.y = pos.y - origin.y;
        mutable.z = pos.z - origin.z;
        return pattern.apply(mutable);
    }

    @Override
    public void reset() {
        origin = null;
    }
}

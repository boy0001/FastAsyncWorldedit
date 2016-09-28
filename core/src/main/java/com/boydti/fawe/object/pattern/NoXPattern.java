package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class NoXPattern extends AbstractPattern {

    private final Pattern pattern;

    public NoXPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private Vector mutable = new Vector();

    @Override
    public BaseBlock apply(Vector pos) {
        mutable.y = pos.y;
        mutable.z = pos.z;
        return pattern.apply(mutable);
    }
}

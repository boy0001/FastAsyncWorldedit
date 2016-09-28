package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class NoYPattern extends AbstractPattern {

    private final Pattern pattern;

    public NoYPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private Vector mutable = new Vector();

    @Override
    public BaseBlock apply(Vector pos) {
        mutable.x = pos.x;
        mutable.z = pos.z;
        return pattern.apply(mutable);
    }
}

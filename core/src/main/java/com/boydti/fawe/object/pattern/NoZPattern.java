package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class NoZPattern extends AbstractPattern {

    private final Pattern pattern;

    public NoZPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private Vector mutable = new Vector();

    @Override
    public BaseBlock apply(Vector pos) {
        mutable.x = pos.x;
        mutable.y = pos.y;
        return pattern.apply(mutable);
    }
}

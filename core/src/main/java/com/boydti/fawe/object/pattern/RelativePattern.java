package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class RelativePattern extends AbstractPattern implements ResettablePattern {

    private final Pattern pattern;

    public RelativePattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private Vector origin;
    private MutableBlockVector mutable = new MutableBlockVector();

    @Override
    public BaseBlock apply(Vector pos) {
        if (origin == null) {
            origin = new Vector(pos);
        }
        mutable.mutX((pos.getX() - origin.getX()));
        mutable.mutY((pos.getY() - origin.getY()));
        mutable.mutZ((pos.getZ() - origin.getZ()));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, Vector pos) throws WorldEditException {
        if (origin == null) {
            origin = new Vector(pos);
        }
        mutable.mutX((pos.getX() - origin.getX()));
        mutable.mutY((pos.getY() - origin.getY()));
        mutable.mutZ((pos.getZ() - origin.getZ()));
        return pattern.apply(extent, mutable);
    }

    @Override
    public void reset() {
        origin = null;
    }
}

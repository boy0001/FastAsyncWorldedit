package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;

public class SurfaceRandomOffsetPattern extends AbstractPattern {
    private final Pattern pattern;
    private final Extent extent;

    private int moves;

    private final MutableBlockVector cur = new MutableBlockVector();
    private final MutableBlockVector[] buffer;
    private final MutableBlockVector[] allowed;
    private MutableBlockVector next;

    public SurfaceRandomOffsetPattern(Extent extent, Pattern pattern, int distance) {
        this.pattern = pattern;
        this.extent = extent;
        this.moves = Math.min(255, distance);
        this.buffer = new MutableBlockVector[BreadthFirstSearch.DIAGONAL_DIRECTIONS.length];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new MutableBlockVector();
        }
        allowed = new MutableBlockVector[buffer.length];
    }

    @Override
    public BaseBlock apply(Vector position) {
        return pattern.apply(travel(position));
    }

    private Vector travel(Vector pos) {
        cur.setComponents(pos);
        for (int move = 0; move < moves; move++) {
            int index = 0;
            for (int i = 0; i < allowed.length; i++) {
                next = buffer[i];
                Vector dir = BreadthFirstSearch.DIAGONAL_DIRECTIONS[i];
                next.setComponents(cur.getBlockX() + dir.getBlockX(), cur.getBlockY() + dir.getBlockY(), cur.getBlockZ() + dir.getBlockZ());
                if (allowed(next)) {
                    allowed[index++] = next;
                }
            }
            if (index == 0) {
                return cur;
            }
            next = allowed[PseudoRandom.random.nextInt(index)];
            cur.setComponents(next.getBlockX(), next.getBlockY(), next.getBlockZ());
        }
        return cur;
    }

    private boolean allowed(Vector v) {
        BaseBlock block = pattern.apply(v);
        if (FaweCache.canPassThrough(block.getId(), block.getData())) {
            return false;
        }
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        v.mutY(y + 1);
        if (canPassthrough(v)) { v.mutY(y); return true; }
        v.mutY(y - 1);
        if (canPassthrough(v)) { v.mutY(y); return true; }
        v.mutY(y);
        v.mutX(x + 1);
        if (canPassthrough(v)) { v.mutX(x); return true; }
        v.mutX(x - 1);
        if (canPassthrough(v)) { v.mutX(x); return true; }
        v.mutX(x);
        v.mutZ(z + 1);
        if (canPassthrough(v)) { v.mutZ(z); return true; }
        v.mutZ(z - 1);
        if (canPassthrough(v)) { v.mutZ(z); return true; }
        v.mutZ(z);
        return false;
    }

    private boolean canPassthrough(Vector v) {
        BaseBlock block = pattern.apply(v);
        return FaweCache.canPassThrough(block.getId(), block.getData());
    }
//
//    @Override
//    public BaseBlock apply(Vector position) {
//        mutable.mutX((position.getX() + r.nextInt(dx2) - dx));
//        mutable.mutY((position.getY() + r.nextInt(dy2) - dy));
//        mutable.mutZ((position.getZ() + r.nextInt(dz2) - dz));
//        BaseBlock block = pattern.apply(mutable);
//        if (solid[FaweCache.getCombined(block)]) {
//            if (solid[FaweCache.getCombined(mutable)])
//            mutable.mutY(mutable.getY() + 1);
//            if (!solid[FaweCache.getCombined(pattern.apply(mutable))]) {
//                return block;
//            }
//        }
//        return pattern.apply(position);
//    }
//
//    private Vector get(Vector input) {
//        for (dir :
//             BreadthFirstSearch.DIAGONAL_DIRECTIONS)
//    }
//
//    @Override
//    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
//        mutable.mutX((get.getX() + r.nextInt(dx2) - dx));
//        mutable.mutY((get.getY() + r.nextInt(dy2) - dy));
//        mutable.mutZ((get.getZ() + r.nextInt(dz2) - dz));
//        BaseBlock block = pattern.apply(mutable);
//        if (solid[FaweCache.getCombined(block)]) {
//            mutable.mutY(mutable.getY() + 1);
//            if (!solid[FaweCache.getCombined(pattern.apply(mutable))]) {
//                return pattern.apply(extent, set, mutable);
//            }
//        }
//        return pattern.apply(extent, set, get);
//    }
}
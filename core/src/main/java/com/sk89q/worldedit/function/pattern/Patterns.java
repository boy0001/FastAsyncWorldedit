package com.sk89q.worldedit.function.pattern;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

/**
 * Utility methods related to {@link Pattern}s.
 */
public final class Patterns {

    private Patterns() {
    }

    /**
     * Wrap an old-style pattern and return a new pattern.
     *
     * @param pattern the pattern
     * @return a new-style pattern
     */
    public static Pattern wrap(final com.sk89q.worldedit.patterns.Pattern pattern) {
        return pattern;
    }

    /**
     * Wrap a new-style pattern and return an old-style pattern.
     *
     * @param pattern the pattern
     * @return an old-style pattern
     */
    public static com.sk89q.worldedit.patterns.Pattern wrap(final Pattern pattern) {
        if (pattern instanceof com.sk89q.worldedit.patterns.Pattern) {
            return (com.sk89q.worldedit.patterns.Pattern) pattern;
        }
        return new com.sk89q.worldedit.patterns.Pattern() {
            private MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);
            @Override
            public BaseBlock next(Vector position) {
                return pattern.apply(position);
            }

            @Override
            public BaseBlock next(int x, int y, int z) {
                mutable.mutX(x);
                mutable.mutY(y);
                mutable.mutZ(z);
                return next(mutable);
            }
        };
    }

    public static Class<?> inject() {
        return Patterns.class;
    }
}

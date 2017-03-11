package com.sk89q.worldedit.function.pattern;

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
        if (pattern instanceof Pattern) {
            return (Pattern) pattern;
        }
        return position -> pattern.next(position);
    }

    /**
     * Wrap a new-style pattern and return an old-style pattern.
     *
     * @param pattern the pattern
     * @return an old-style pattern
     */
    public static com.sk89q.worldedit.patterns.Pattern wrap(final Pattern pattern) {
        return pattern;
    }

    public static Class<?> inject() {
        return Patterns.class;
    }
}

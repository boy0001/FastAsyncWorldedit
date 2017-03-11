package com.sk89q.worldedit.function.pattern;

import com.boydti.fawe.object.collection.RandomCollection;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uses a random pattern of a weighted list of patterns.
 */
public class RandomPattern extends AbstractPattern {

    private Map<Pattern, Double> weights = new HashMap<>();
    private RandomCollection<Pattern> collection;
    private Set<Pattern> patterns;

    /**
     * Add a pattern to the weight list of patterns.
     *
     * <p>The probability for the pattern added is chance / max where max is
     * the sum of the probabilities of all added patterns.</p>
     *
     * @param pattern the pattern
     * @param chance the chance, which can be any positive number
     */
    public void add(Pattern pattern, double chance) {
        checkNotNull(pattern);
        weights.put(pattern, chance);
        collection = RandomCollection.of(weights);
        this.patterns = weights.keySet();
    }

    @Override
    public BaseBlock apply(Vector position) {
        return collection.next().apply(position);
    }

    @Override
    public boolean apply(Extent extent, Vector position) throws WorldEditException {
        return collection.next().apply(extent, position);
    }

    private static class Chance {
        private Pattern pattern;
        private double chance;

        private Chance(Pattern pattern, double chance) {
            this.pattern = pattern;
            this.chance = chance;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public double getChance() {
            return chance;
        }
    }

    public static Class<?> inject() {
        return RandomPattern.class;
    }

}
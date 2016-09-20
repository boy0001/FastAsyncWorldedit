package com.boydti.fawe.object.collection;

import java.util.Map;

public abstract class RandomCollection<T> {
    public RandomCollection(Map<T, Double> weights) {}

    public static <T> RandomCollection<T> of(Map<T, Double> weights) {
        try {
            return new FastRandomCollection<>(weights);
        } catch (IllegalArgumentException ignore) {
            return new SimpleRandomCollection<>(weights);
        }
    }

    public abstract T next();
}

package com.boydti.fawe.object.collection;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class SimpleRandomCollection<E> extends RandomCollection<E> {

    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
    private final Random random;
    private double total = 0;

    public SimpleRandomCollection(Map<E, Double> weights) {
        super(weights);
        this.random = new Random();
        for (Map.Entry<E, Double> entry : weights.entrySet()) {
            add(entry.getValue(), entry.getKey());
        }

    }

    public void add(double weight, E result) {
        if (weight <= 0) return;
        total += weight;
        map.put(total, result);
    }

    public E next() {
        double value = random.nextDouble() * total;
        return map.ceilingEntry(value).getValue();
    }
}

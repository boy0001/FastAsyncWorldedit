package com.boydti.fawe.object.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class IterableThreadLocal<T> extends ThreadLocal<T> implements Iterable<T> {
    private ThreadLocal<T> flag;
    private ConcurrentLinkedDeque<T> allValues = new ConcurrentLinkedDeque<T>();

    public IterableThreadLocal() {
    }

    @Override
    protected final T initialValue() {
        T value = init();
        if (value != null) {
            allValues.add(value);
        }
        return value;
    }

    @Override
    public final Iterator<T> iterator() {
        return getAll().iterator();
    }

    public T init() {
        return null;
    }

    public final Collection<T> getAll() {
        return Collections.unmodifiableCollection(allValues);
    }
}

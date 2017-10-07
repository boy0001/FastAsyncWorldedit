package com.sk89q.jnbt;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Some data with a name
 */
public class NamedData {
    private final String name;
    private final Object data;

    /**
     * Create a new named tag.
     *
     * @param name the name
     * @param data the data
     */
    public NamedData(String name, Object data) {
        checkNotNull(name);
        this.name = name;
        this.data = data;
    }

    /**
     * Get the name of the tag.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the tag.
     *
     * @return the tag
     */
    public Object getValue() {
        return data;
    }
}

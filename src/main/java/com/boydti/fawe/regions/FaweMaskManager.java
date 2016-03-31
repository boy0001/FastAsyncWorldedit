package com.boydti.fawe.regions;

import com.boydti.fawe.bukkit.regions.FaweMask;
import com.boydti.fawe.object.FawePlayer;

public abstract class FaweMaskManager<T> {
    private final String key;

    public FaweMaskManager(final String plugin) {
        this.key = plugin.toLowerCase();
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.key;
    }

    public abstract FaweMask getMask(final FawePlayer<T> player);
}

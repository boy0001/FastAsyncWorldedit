package com.boydti.fawe.object;

public abstract class FaweCommand<T> {
    public final String perm;

    public FaweCommand(final String perm) {
        this.perm = perm;
    }

    public String getPerm() {
        return this.perm;
    }

    public abstract boolean execute(final FawePlayer<T> player, final String... args);
}

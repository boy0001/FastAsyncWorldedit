package com.boydti.fawe.example;

public interface Relighter {
    boolean addChunk(int cx, int cz, boolean[] fix, int bitmask);

    void addLightUpdate(int x, int y, int z);

    void fixLightingSafe(boolean sky);

    void fixBlockLighting();

    void fixSkyLighting();

    boolean isEmpty();
}

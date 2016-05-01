package com.boydti.fawe.object.brush.heightmap;

import com.boydti.fawe.util.MathMan;

public class HeightMap {
    public int size2;
    public int size;

    public HeightMap() {
        setSize(5);
    }

    public HeightMap(int size) {
        setSize(size);
    }

    public void setSize(int size) {
        this.size = size;
        this.size2 = size * size;
    }

    public int getHeight(int x, int z) {
        int dx = Math.abs(x);
        int dz = Math.abs(z);
        int d2 = dx * dx + dz * dz;
        if (d2 > size2) {
            return 0;
        }
        return size - MathMan.sqrt(d2);
    }
}

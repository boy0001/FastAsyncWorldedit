package com.boydti.fawe.object;

public class IntegerTrio {
    private final int z;
    private final int x;
    private final int y;

    public IntegerTrio(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int hashCode() {
        int hash = 13;
        hash = hash * 13 + x;
        hash = hash * 13 + y;
        hash = hash * 13 + z;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof  IntegerTrio) {
            IntegerTrio other = (IntegerTrio) obj;
            return other.x == x && other.z == z && other.y == y;
        }
        return false;
    }
}

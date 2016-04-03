package com.boydti.fawe.object;

public class IntegerPair {
    public int x;
    public int z;

    public IntegerPair(final int x, final int z) {
        this.x = x;
        this.z = z;
    }

    int hash;

    @Override
    public int hashCode() {
        if (hash == 0) {
            this.hash = (x << 16) | (z & 0xFFFF);
        }
        return this.hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (this.hashCode() != obj.hashCode()) || (this.getClass() != obj.getClass())) {
            return false;
        }
        final IntegerPair other = (IntegerPair) obj;
        return ((this.x == other.x) && (this.z == other.z));
    }
}

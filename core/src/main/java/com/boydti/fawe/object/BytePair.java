package com.boydti.fawe.object;

public class BytePair {
    public byte[] pair;

    public BytePair(final byte x, final byte z) {
        this.pair = new byte[] { x, z};
    }

    int hash;

    @Override
    public int hashCode() {
        return pair[0] + (pair[1] << 8);
    }

    @Override
    public String toString() {
        return pair[0] + "," + pair[1];
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (this.hashCode() != obj.hashCode()) || (this.getClass() != obj.getClass())) {
            return false;
        }
        final BytePair other = (BytePair) obj;
        return pair[0] == other.pair[0] && pair[1] == other.pair[1];
    }
}

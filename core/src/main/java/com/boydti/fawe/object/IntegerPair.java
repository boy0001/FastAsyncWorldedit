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
        if (this.hash == 0) {
            long val = 0;
            if (this.x >= 0) {
                if (this.z >= 0) {
                    val = (this.x * this.x) + (3 * this.x) + (2 * this.x * this.z) + this.z + (this.z * this.z);
                } else {
                    final int z1 = -this.z;
                    val = (this.x * this.x) + (3 * this.x) + (2 * this.x * z1) + z1 + (z1 * z1) + 1;
                }
            } else {
                final int x1 = -this.x;
                if (this.z >= 0) {
                    val = -((x1 * x1) + (3 * x1) + (2 * x1 * this.z) + this.z + (this.z * this.z));
                } else {
                    final int z1 = -this.z;
                    val = -((x1 * x1) + (3 * x1) + (2 * x1 * z1) + z1 + (z1 * z1) + 1);
                }
            }
            this.hash = (int) (val % Integer.MAX_VALUE);
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

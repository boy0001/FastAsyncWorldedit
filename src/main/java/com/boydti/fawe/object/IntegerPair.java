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
            long val = 0;
            if (x >= 0) {
                if (z >= 0) {
                    val = (x * x) + (3 * x) + (2 * x * z) + z + (z * z);
                } else {
                    final int z1 = -z;
                    val = (x * x) + (3 * x) + (2 * x * z1) + z1 + (z1 * z1) + 1;
                }
            } else {
                final int x1 = -x;
                if (z >= 0) {
                    val = -((x1 * x1) + (3 * x1) + (2 * x1 * z) + z + (z * z));
                } else {
                    final int z1 = -z;
                    val = -((x1 * x1) + (3 * x1) + (2 * x1 * z1) + z1 + (z1 * z1) + 1);
                }
            }
            hash = (int) (val % Integer.MAX_VALUE);
        }
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (hashCode() != obj.hashCode()) || (getClass() != obj.getClass())) {
            return false;
        }
        final IntegerPair other = (IntegerPair) obj;
        return ((x == other.x) && (z == other.z));
    }
}

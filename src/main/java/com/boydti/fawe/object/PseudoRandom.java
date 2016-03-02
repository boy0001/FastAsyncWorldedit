package com.boydti.fawe.object;

public class PseudoRandom {
    
    public static PseudoRandom random = new PseudoRandom();

    private long state;
    
    public PseudoRandom() {
        state = System.nanoTime();
    }
    
    public PseudoRandom(final long state) {
        this.state = state;
    }
    
    public long nextLong() {
        final long a = state;
        state = xorShift64(a);
        return a;
    }
    
    public long xorShift64(long a) {
        a ^= (a << 21);
        a ^= (a >>> 35);
        a ^= (a << 4);
        return a;
    }
    
    public int random(final int n) {
        if (n == 1) {
            return 0;
        }
        final long r = ((nextLong() >>> 32) * n) >> 32;
        return (int) r;
    }
}

package com.boydti.fawe;

import com.boydti.fawe.object.PseudoRandom;

public class FaweCache {
    public final static short[][][] CACHE_I = new short[256][16][16];
    public final static short[][][] CACHE_J = new short[256][16][16];
    
    public final static byte[][] CACHE_X = new byte[16][4096];
    public final static short[][] CACHE_Y = new short[16][4096];
    public final static byte[][] CACHE_Z = new byte[16][4096];
    
    public final static short[] CACHE_ID = new short[65535];
    public final static byte[] CACHE_DATA = new byte[65535];
    
    public final static PseudoRandom RANDOM = new PseudoRandom();
    
    static {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    final short i = (short) (y >> 4);
                    final short j = (short) (((y & 0xF) << 8) | (z << 4) | x);
                    CACHE_I[y][x][z] = i;
                    CACHE_J[y][x][z] = j;
                    CACHE_X[i][j] = (byte) x;
                    CACHE_Y[i][j] = (short) y;
                    CACHE_Z[i][j] = (byte) z;
                }
            }
        }
        for (int i = 0; i < 65535; i++) {
            final int j = i >> 4;
            final int k = i & 0xF;
            CACHE_ID[i] = (short) j;
            CACHE_DATA[i] = (byte) k;
        }
    }
}

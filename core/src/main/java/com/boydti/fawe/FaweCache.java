package com.boydti.fawe;

import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.blocks.BaseBlock;

public class FaweCache {
    /**
     * [ y | x | z ] => index
     */
    public final static short[][][] CACHE_I = new short[256][16][16];
    /**
     * [ y | x | z ] => index
     */
    public final static short[][][] CACHE_J = new short[256][16][16];

    /**
     *  [ i | j ] => x
     */
    public final static byte[][] CACHE_X = new byte[16][4096];
    /**
     *  [ i | j ] => y
     */
    public final static short[][] CACHE_Y = new short[16][4096];
    /**
     *  [ i | j ] => z
     */
    public final static byte[][] CACHE_Z = new byte[16][4096];

    /**
     * [ combined ] => id
     * (combined >> 4) = id
     */
    public final static short[] CACHE_ID = new short[65535];
    /**
     * [ combined ] => data
     * (combined & 0xF) = data
     */
    public final static byte[] CACHE_DATA = new byte[65535];

    /**
     * Immutable BaseBlock cache
     * [ combined ] => block
     */
    public final static BaseBlock[] CACHE_BLOCK = new BaseBlock[Short.MAX_VALUE];

    /**
     * Faster than java random (since it just needs to look random)
     */
    public final static PseudoRandom RANDOM = new PseudoRandom();

    /**
     * Get the cached BaseBlock object for an id/data<br>
     *  - The block is immutable
     * @param id
     * @param data
     * @return
     */
    public static BaseBlock getBlock(int id, int data) {
        return CACHE_BLOCK[(id << 4) + data];
    }

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

        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int id = i >> 4;
            int data = i & 0xf;
            CACHE_BLOCK[i] = new BaseBlock(id, data) {
                @Override
                public void setData(int data) {
                    throw new IllegalStateException("Cannot set data");
                }

                @Override
                public void setId(int id) {
                    throw new IllegalStateException("Cannot set id");
                }

                @Override
                public BaseBlock flip() {
                    BaseBlock clone = new BaseBlock(getId(), getData(), getNbtData());
                    return clone.flip();
                }

                @Override
                public BaseBlock flip(CuboidClipboard.FlipDirection direction) {
                    BaseBlock clone = new BaseBlock(getId(), getData(), getNbtData());
                    return clone.flip(direction);
                }

                @Override
                public boolean hasWildcardData() {
                    return true;
                }
            };
        }
    }

    /**
     * Check if an id might have data
     * @param id
     * @return
     */
    public static boolean hasData(int id) {
        switch (id) {
            case 0:
            case 2:
            case 4:
            case 13:
            case 14:
            case 15:
            case 20:
            case 21:
            case 22:
            case 25:
            case 30:
            case 32:
            case 37:
            case 39:
            case 40:
            case 41:
            case 42:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 51:
            case 52:
            case 56:
            case 57:
            case 58:
            case 60:
            case 7:
            case 9:
            case 11:
            case 73:
            case 74:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 87:
            case 88:
            case 101:
            case 102:
            case 103:
            case 110:
            case 112:
            case 113:
            case 117:
            case 121:
            case 122:
            case 123:
            case 124:
            case 129:
            case 133:
            case 138:
            case 137:
            case 140:
            case 165:
            case 166:
            case 169:
            case 170:
            case 172:
            case 173:
            case 174:
            case 181:
            case 182:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
                return false;
            default:
                return true;
        }
    }

    /**
     * Check if an id might have nbt
     * @param id
     * @return
     */
    public static boolean hasNBT(int id) {
        switch (id) {
            case 54:
            case 130:
            case 142:
            case 27:
            case 137:
            case 52:
            case 154:
            case 84:
            case 25:
            case 144:
            case 138:
            case 176:
            case 177:
            case 63:
            case 119:
            case 68:
            case 323:
            case 117:
            case 116:
            case 28:
            case 66:
            case 157:
            case 61:
            case 62:
            case 140:
            case 146:
            case 149:
            case 150:
            case 158:
            case 23:
            case 123:
            case 124:
            case 29:
            case 33:
            case 151:
            case 178:
                return true;
            default:
                return false;
        }
    }
}

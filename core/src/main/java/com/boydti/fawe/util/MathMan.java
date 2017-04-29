package com.boydti.fawe.util;

public class MathMan {

    private static final int ATAN2_BITS = 7;
    private static final int ATAN2_BITS2 = ATAN2_BITS << 1;
    private static final int ATAN2_MASK = ~(-1 << ATAN2_BITS2);
    private static final int ATAN2_COUNT = ATAN2_MASK + 1;
    private static final int ATAN2_DIM = (int) Math.sqrt(ATAN2_COUNT);
    private static final float INV_ATAN2_DIM_MINUS_1 = 1.0f / (ATAN2_DIM - 1);
    private static final float[] atan2 = new float[ATAN2_COUNT];

    static {
        for (int i = 0; i < ATAN2_DIM; i++) {
            for (int j = 0; j < ATAN2_DIM; j++) {
                float x0 = (float) i / ATAN2_DIM;
                float y0 = (float) j / ATAN2_DIM;

                atan2[(j * ATAN2_DIM) + i] = (float) Math.atan2(y0, x0);
            }
        }
    }

    private static float[] ANGLES = new float[65536];

    public static float sinInexact(double paramFloat) {
        return ANGLES[(int)(paramFloat * 10430.378F) & 0xFFFF];
    }

    public static float cosInexact(double paramFloat) {
        return ANGLES[(int)(paramFloat * 10430.378F + 16384.0F) & 0xFFFF];
    }

    public static int floorZero(double d0) {
        int i = (int)d0;
        return d0 < (double) i ? i - 1 : i;
    }

    public static double max(double... values) {
        double max = Double.MIN_VALUE;
        for (double d : values) {
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    public static int max(int... values) {
        int max = Integer.MIN_VALUE;
        for (int d : values) {
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    public static int min(int... values) {
        int min = Integer.MAX_VALUE;
        for (int d : values) {
            if (d < min) {
                min = d;
            }
        }
        return min;
    }

    public static double min(double... values) {
        double min = Double.MAX_VALUE;
        for (double d : values) {
            if (d < min) {
                min = d;
            }
        }
        return min;
    }

    public static int ceilZero(float floatNumber) {
        int floor = (int)floatNumber;
        return floatNumber > (float) floor ? floor + 1 : floor;
    }

    public static int clamp(int check, int min, int max) {
        return check > max ? max : (check < min ? min : check);
    }

    public static float clamp(float check, float min, float max) {
        return check > max ? max : (check < min ? min : check);
    }

    static {
        for(int i = 0; i < 65536; ++i) {
            ANGLES[i] = (float)Math.sin((double)i * 3.141592653589793D * 2.0D / 65536.0D);
        }
    }

    private final static int[] table = {
            0,    16,  22,  27,  32,  35,  39,  42,  45,  48,  50,  53,  55,  57,
            59,   61,  64,  65,  67,  69,  71,  73,  75,  76,  78,  80,  81,  83,
            84,   86,  87,  89,  90,  91,  93,  94,  96,  97,  98,  99, 101, 102,
            103, 104, 106, 107, 108, 109, 110, 112, 113, 114, 115, 116, 117, 118,
            119, 120, 121, 122, 123, 124, 125, 126, 128, 128, 129, 130, 131, 132,
            133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 144, 145,
            146, 147, 148, 149, 150, 150, 151, 152, 153, 154, 155, 155, 156, 157,
            158, 159, 160, 160, 161, 162, 163, 163, 164, 165, 166, 167, 167, 168,
            169, 170, 170, 171, 172, 173, 173, 174, 175, 176, 176, 177, 178, 178,
            179, 180, 181, 181, 182, 183, 183, 184, 185, 185, 186, 187, 187, 188,
            189, 189, 190, 191, 192, 192, 193, 193, 194, 195, 195, 196, 197, 197,
            198, 199, 199, 200, 201, 201, 202, 203, 203, 204, 204, 205, 206, 206,
            207, 208, 208, 209, 209, 210, 211, 211, 212, 212, 213, 214, 214, 215,
            215, 216, 217, 217, 218, 218, 219, 219, 220, 221, 221, 222, 222, 223,
            224, 224, 225, 225, 226, 226, 227, 227, 228, 229, 229, 230, 230, 231,
            231, 232, 232, 233, 234, 234, 235, 235, 236, 236, 237, 237, 238, 238,
            239, 240, 240, 241, 241, 242, 242, 243, 243, 244, 244, 245, 245, 246,
            246, 247, 247, 248, 248, 249, 249, 250, 250, 251, 251, 252, 252, 253,
            253, 254, 254, 255
    };

    public static double hypot(final double... pars) {
        double sum = 0;
        for (final double d : pars) {
            sum += Math.pow(d, 2);
        }
        return Math.sqrt(sum);
    }

    public static double hypot2(final double... pars) {
        double sum = 0;
        for (final double d : pars) {
            sum += Math.pow(d, 2);
        }
        return sum;
    }

    public static final int wrap(int value, int min, int max) {
        if (max < min) {
            return value;
        }
        if (min == max) {
            return min;
        }
        int diff = max - min + 1;
        if (value < min) {
            return max - ((min - value) % diff);
        } else if (value > max) {
            return min + ((value - min) % diff);
        } else {
            return value;
        }
    }

    public static final long inverseRound(double val) {
        long round = Math.round(val);
        return (long) (round + Math.signum(val - round));
    }

    public static final short pairByte(int x, int y) {
        return (short) ((x << 8) | (y & 0xFF));
    }

    public static final byte unpairShortX(short pair) {
        return (byte) (pair >> 8);
    }

    public static final byte unpairShortY(short pair) {
        return (byte) pair;
    }

    public static final long pairInt(int x, int y) {
        return (((long)x) << 32) | (y & 0xffffffffL);
    }

    public static final long tripleWorldCoord(int x, int y, int z) {
        return y + (((long) x & 0x3FFFFFF) << 8) + (((long) z & 0x3FFFFFF) << 34);
    }

    public static final long untripleWorldCoordX(long triple) {
        return (((triple >> 8) & 0x3FFFFFF) << 38) >> 38;
    }

    public static final long untripleWorldCoordY(long triple) {
        return triple & 0xFF;
    }

    public static final long untripleWorldCoordZ(long triple) {
        return (((triple >> 34) & 0x3FFFFFF) << 38) >> 38;
    }

    public static final short tripleBlockCoord(int x, int y, int z) {
        return (short) ((x & 15) << 12 | (z & 15) << 8 | y);
    }

    public static final char tripleBlockCoordChar(int x, int y, int z) {
        return (char) ((x & 15) << 12 | (z & 15) << 8 | y);
    }

    public static final int untripleBlockCoordX(int triple) {
        return (triple >> 12) & 0xF;
    }

    public static final int untripleBlockCoordY(int triple) {
        return (triple & 0xFF);
    }

    public static final int untripleBlockCoordZ(int triple) {
        return (triple >> 8) & 0xF;
    }

    public static int tripleSearchCoords(int x, int y, int z) {
        byte b1 = (byte) y;
        byte b3 = (byte) (x);
        byte b4 = (byte) (z);
        int x16 = (x >> 8) & 0x7;
        int z16 = (z >> 8) & 0x7;
        byte b2 = MathMan.pair8(x16, z16);
        return ((b1 & 0xFF)
                + ((b2 & 0x7F) << 8)
                + ((b3 & 0xFF) << 15)
                + ((b4 & 0xFF) << 23))
                ;
    }

    public static final long chunkXZ2Int(int x, int z) {
        return (long)x & 4294967295L | ((long)z & 4294967295L) << 32;
    }

    public static final int unpairIntX(long pair) {
        return (int)(pair >> 32);
    }

    public static final int unpairIntY(long pair) {
        return (int)pair;
    }

    public static final byte pair16(int x, int y) {
        return (byte) (x + (y << 4));
    }

    public static final byte unpair16x(byte value) {
        return (byte) (value & 0xF);
    }

    public static final byte unpair16y(byte value) {
        return (byte) ((value >> 4) & 0xF);
    }

    public static final byte pair8(int x, int y) {
        return (byte) (x + (y << 3));
    }

    public static byte unpair8x(int value) {
        return (byte) (value & 0x7);
    }

    public static byte unpair8y(int value) {
        return (byte) ((value >> 3) & 0x7F);
    }

    public static final int lossyFastDivide(int a, int b) {
        return (a*((1<<16)/b))>>16;
    }

    public static final int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    public static final int gcd(int[] a) {
        int result = a[0];
        for (int i = 1; i < a.length; i++) {
            result = gcd(result, a[i]);
        }
        return result;
    }
    
    public static final int sqrt(int x) {
        int xn;

        if (x >= 0x10000) {
            if (x >= 0x1000000) {
                if (x >= 0x10000000) {
                    if (x >= 0x40000000) {
                        xn = table[x >> 24] << 8;
                    } else {
                        xn = table[x >> 22] << 7;
                    }
                } else {
                    if (x >= 0x4000000) {
                        xn = table[x >> 20] << 6;
                    } else {
                        xn = table[x >> 18] << 5;
                    }
                }

                xn = (xn + 1 + (x / xn)) >> 1;
                xn = (xn + 1 + (x / xn)) >> 1;
                return ((xn * xn) > x) ? --xn : xn;
            } else {
                if (x >= 0x100000) {
                    if (x >= 0x400000) {
                        xn = table[x >> 16] << 4;
                    } else {
                        xn = table[x >> 14] << 3;
                    }
                } else {
                    if (x >= 0x40000) {
                        xn = table[x >> 12] << 2;
                    } else {
                        xn = table[x >> 10] << 1;
                    }
                }

                xn = (xn + 1 + (x / xn)) >> 1;

                return ((xn * xn) > x) ? --xn : xn;
            }
        } else {
            if (x >= 0x100) {
                if (x >= 0x1000) {
                    if (x >= 0x4000) {
                        xn = (table[x >> 8]) + 1;
                    } else {
                        xn = (table[x >> 6] >> 1) + 1;
                    }
                } else {
                    if (x >= 0x400) {
                        xn = (table[x >> 4] >> 2) + 1;
                    } else {
                        xn = (table[x >> 2] >> 3) + 1;
                    }
                }

                return ((xn * xn) > x) ? --xn : xn;
            } else {
                if (x >= 0) {
                    return table[x] >> 4;
                }
            }
        }
        throw new IllegalArgumentException("Invalid number:" + x);
    }


    public static final double getMean(int[] array) {
        double count = 0;
        for (int i : array) {
            count += i;
        }
        return count / array.length;
    }

    public static final double getMean(double[] array) {
        double count = 0;
        for (double i : array) {
            count += i;
        }
        return count / array.length;
    }

    public static final int pair(short x, short y) {
        return (x << 16) | (y & 0xFFFF);
    }

    public static final short unpairX(int hash) {
        return (short) (hash >> 16);
    }

    public static final short unpairY(int hash) {
        return (short) (hash & 0xFFFF);
    }

    /**
     * Returns [x, y, z]
     * @param yaw
     * @param pitch
     * @return
     */
    public static final float[] getDirection(float yaw, float pitch) {
        double pitch_sin = Math.sin(pitch);
        return new float[]{(float) (pitch_sin * Math.cos(yaw)), (float) (pitch_sin * Math.sin(yaw)), (float) Math.cos(pitch)};
    }

    public static final int roundInt(double value) {
        return (int) (value < 0 ? (value == (int) value) ? value : value - 1 : value);
    }

    /**
     * Returns [ pitch, yaw ]
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static final float[] getPitchAndYaw(float x, float y, float z) {
        float distance = sqrtApprox((z * z) + (x * x));
        return new float[]{atan2(y, distance), atan2(x, z)};
    }

    public static final float atan2(float y, float x) {
        float add, mul;

        if (x < 0.0f) {
            if (y < 0.0f) {
                x = -x;
                y = -y;

                mul = 1.0f;
            } else {
                x = -x;
                mul = -1.0f;
            }

            add = -3.141592653f;
        } else {
            if (y < 0.0f) {
                y = -y;
                mul = -1.0f;
            } else {
                mul = 1.0f;
            }

            add = 0.0f;
        }

        float invDiv = 1.0f / (((x < y) ? y : x) * INV_ATAN2_DIM_MINUS_1);

        int xi = (int) (x * invDiv);
        int yi = (int) (y * invDiv);

        return (atan2[(yi * ATAN2_DIM) + xi] + add) * mul;
    }

    public static final float sqrtApprox(float f) {
        return f * Float.intBitsToFloat(0x5f375a86 - (Float.floatToIntBits(f) >> 1));
    }

    public static final double sqrtApprox(double d) {
        return Double.longBitsToDouble(((Double.doubleToLongBits(d) - (1l << 52)) >> 1) + (1l << 61));
    }

    public static final float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - (xhalf * x * x));
        return x;
    }

    public static final int getPositiveId(int i) {
        if (i < 0) {
            return (-i * 2) - 1;
        }
        return i * 2;
    }

    public static final boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if ((c <= '/') || (c >= ':')) {
                return false;
            }
        }
        return true;
    }

    public static final double getSD(double[] array, double av) {
        double sd = 0;
        for (double element : array) {
            sd += Math.pow(Math.abs(element - av), 2);
        }
        return Math.sqrt(sd / array.length);
    }

    public static final double getSD(int[] array, double av) {
        double sd = 0;
        for (int element : array) {
            sd += Math.pow(Math.abs(element - av), 2);
        }
        return Math.sqrt(sd / array.length);
    }

    public static final int mod(int x, int y) {
        if (isPowerOfTwo(y)) {
            return x & (y - 1);
        }
        return x % y;
    }

    public static final int unsignedmod(int x, int y) {
        if (isPowerOfTwo(y)) {
            return x & (y - 1);
        }
        return x % y;
    }

    public static final boolean isPowerOfTwo(int x) {
        return (x & (x - 1)) == 0;
    }
}

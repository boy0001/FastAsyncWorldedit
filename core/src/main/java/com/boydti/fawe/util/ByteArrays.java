package com.boydti.fawe.util;

public class ByteArrays {

    public static final byte[] EMPTY_ARRAY = new byte[0];

    public static void ensureOffsetLength(byte[] a, int offset, int length)
    {
        ensureOffsetLength(a.length, offset, length);
    }

    public static void ensureOffsetLength(int arrayLength, int offset, int length)
    {
        if (offset < 0) {
            throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length (" + length + ") is negative");
        }
        if (offset + length > arrayLength) {
            throw new ArrayIndexOutOfBoundsException("Last index (" + (offset + length) + ") is greater than array length (" + arrayLength + ")");
        }
    }

    public static byte[] grow(byte[] array, int length, int preserve)
    {
        if (length > array.length)
        {
            int newLength = (int)Math.max(
                    Math.min(2L * array.length, 2147483639L), length);
            byte[] t = new byte[newLength];
            System.arraycopy(array, 0, t, 0, preserve);
            return t;
        }
        return array;
    }

    public static byte[] trim(byte[] array, int length)
    {
        if (length >= array.length) {
            return array;
        }
        byte[] t = length == 0 ? EMPTY_ARRAY : new byte[length];
        System.arraycopy(array, 0, t, 0, length);
        return t;
    }
}

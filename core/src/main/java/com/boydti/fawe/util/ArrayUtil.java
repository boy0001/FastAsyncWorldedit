package com.boydti.fawe.util;

public class ArrayUtil {
    public static final void fill(byte[] a, int fromIndex, int toIndex, byte val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static final void fill(char[] a, int fromIndex, int toIndex, char val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }
}

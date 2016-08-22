package com.boydti.fawe.object.io;

import com.boydti.fawe.util.ByteArrays;
import java.io.IOException;
import java.io.OutputStream;

public class FastByteArrayOutputStream extends OutputStream {

    /**
     * The array backing the output stream.
     */
    public final static int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The array backing the output stream.
     */
    public byte[] array;

    /**
     * The number of valid bytes in {@link #array}.
     */
    public int length;

    /**
     * The current writing position.
     */
    private int position;

    /**
     * Creates a new array output stream with an initial capacity of {@link #DEFAULT_INITIAL_CAPACITY} bytes.
     */
    public FastByteArrayOutputStream() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new array output stream with a given initial capacity.
     *
     * @param initialCapacity the initial length of the backing array.
     */
    public FastByteArrayOutputStream(final int initialCapacity) {
        array = new byte[initialCapacity];
    }

    /**
     * Creates a new array output stream wrapping a given byte array.
     *
     * @param a the byte array to wrap.
     */
    public FastByteArrayOutputStream(final byte[] a) {
        array = a;
    }

    /**
     * Marks this array output stream as empty.
     */
    public void reset() {
        length = 0;
        position = 0;
    }

    public void trim() {
        this.array = ByteArrays.trim(this.array, this.length);
    }

    public byte[] toByteArray() {
        trim();
        return array;
    }

    public void write(final int b) {
        if (position >= array.length) array = ByteArrays.grow(array, position + 1, length);
        array[position++] = (byte) b;
        if (length < position) length = position;
    }

    public void write(final byte[] b, final int off, final int len) throws IOException {
        ByteArrays.ensureOffsetLength(b, off, len);
        if (position + len > array.length) array = ByteArrays.grow(array, position + len, position);
        System.arraycopy(b, off, array, position, len);
        if (position + len > length) length = position += len;
    }

    public void position(long newPosition) {
        if (position > Integer.MAX_VALUE) throw new IllegalArgumentException("Position too large: " + newPosition);
        position = (int) newPosition;
    }

    public long position() {
        return position;
    }

    public long length() throws IOException {
        return length;
    }
}
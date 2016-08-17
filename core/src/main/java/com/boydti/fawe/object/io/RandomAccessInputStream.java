package com.boydti.fawe.object.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessInputStream extends InputStream {
    private final RandomAccessFile raf;

    public RandomAccessInputStream(RandomAccessFile raf) {
        this.raf = raf;
    }

    @Override
    public int read() throws IOException {
        return raf.read();
    }

    @Override
    public int available() throws IOException {
        return (int) (raf.length() - raf.getFilePointer());
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}

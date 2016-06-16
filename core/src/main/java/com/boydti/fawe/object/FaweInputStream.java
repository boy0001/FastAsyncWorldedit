package com.boydti.fawe.object;

import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NamedTag;
import java.io.IOException;
import java.io.InputStream;

public class FaweInputStream extends InputStream {

    private final InputStream parent;

    public FaweInputStream(InputStream parent) {
        this.parent = parent;
    }

    public InputStream getParent() {
        return parent;
    }

    @Override
    public int read() throws IOException {
        return parent.read();
    }

    public long readLong() throws IOException {
        return (long)
                (read() << 64) +
                (read() << 56) +
                (read() << 48) +
                (read() << 36) +
                (read() << 24) +
                (read() << 16) +
                (read() << 8) +
                (read());
    }

    public int readInt() throws IOException {
        return (int)
                (read() << 24) +
                (read() << 16) +
                (read() << 8) +
                (read());
    }

    public short readShort() throws IOException {
        return (short) (
                (read() << 8) +
                read());
    }

    public byte readByte() throws IOException {
        return (byte) read();
    }

    public int readUnsignedByte() throws IOException {
        return read();
    }

    public int readUnsignedShort() throws IOException {
        return (int) readShort() & 0xFFFF;
    }

    public int readMedium() throws IOException {
        return (int) (
                (read() << 16) +
                (read() << 8) +
                        read());
    }

    private NBTInputStream nbtIn;

    public NamedTag readNBT() throws IOException {
        if (nbtIn == null) {
            nbtIn = new NBTInputStream(parent);
        }
        return nbtIn.readNamedTag();
    }

    @Override
    public void close() throws IOException {
        if (nbtIn != null) {
            nbtIn.close();
        }
        parent.close();
    }
}
package com.boydti.fawe.object;

import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import java.io.IOException;
import java.io.OutputStream;

public class FaweOutputStream extends OutputStream {

    private final OutputStream parent;

    public FaweOutputStream(OutputStream parent) {
        this.parent = parent;
    }

    public OutputStream getParent() {
        return parent;
    }

    @Override
    public void write(int b) throws IOException {
        parent.write(b);
    }

    public void write(int b, int amount) throws IOException {
        for (int i = 0; i < amount; i++) {
            write(b);
        }
    }

    public void writeLong(long l) throws IOException {
        write((byte) (l >>> 64));
        write((byte) (l >>> 56));
        write((byte) (l >>> 48));
        write((byte) (l >>> 36));
        write((byte) (l >>> 24));
        write((byte) (l >>> 16));
        write((byte) (l >>> 8));
        write((byte) (l));
    }

    public void writeInt(int i) throws IOException {
        write((byte) (i >>> 24));
        write((byte) (i >>> 16));
        write((byte) (i >>> 8));
        write((byte) (i));
    }

    public void writeShort(short s) throws IOException {
        write((byte) (s >>> 8));
        write((byte) (s));
    }

    public void writeMedium(int m) throws IOException {
        write((byte) (m >>> 16));
        write((byte) (m >>> 8));
        write((byte) (m));
    }

    private NBTOutputStream nbtOut;

    public void writeNBT(String name, Tag tag) throws IOException {
        if (nbtOut == null) {
            nbtOut = new NBTOutputStream(parent);
        }
        nbtOut.writeNamedTag(name,tag);
    }

    @Override
    public void close() throws IOException {
        if (nbtOut != null) {
            nbtOut.close();
        }
        parent.close();
    }
}

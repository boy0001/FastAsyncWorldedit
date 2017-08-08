package com.boydti.fawe.object;

import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FaweOutputStream extends DataOutputStream {

    private final OutputStream parent;

    public FaweOutputStream(OutputStream parent) {
        super(parent);
        this.parent = parent;
    }

    public OutputStream getParent() {
        return parent;
    }

    public void write(int b, int amount) throws IOException {
        for (int i = 0; i < amount; i++) {
            write(b);
        }
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

    public void writeVarInt(int i) throws IOException {
        while((i & -128) != 0) {
            this.writeByte(i & 127 | 128);
            i >>>= 7;
        }
        this.writeByte(i);
    }

    public void write(long[] data) throws IOException {
        this.writeVarInt(data.length);

        for(int j = 0; j < data.length; ++j) {
            this.writeLong(data[j]);
        }
    }

    private NBTOutputStream nbtOut;

    public void writeNBT(String name, Tag tag) throws IOException {
        if (nbtOut == null) {
            nbtOut = new NBTOutputStream(parent);
        }
        nbtOut.writeNamedTag(name, tag);
    }

    @Override
    public void close() throws IOException {
        if (nbtOut != null) {
            nbtOut.close();
        }
        parent.close();
    }
}

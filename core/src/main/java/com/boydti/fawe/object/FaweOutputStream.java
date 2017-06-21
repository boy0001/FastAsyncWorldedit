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

package com.boydti.fawe.object;

import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NamedTag;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FaweInputStream extends DataInputStream {

    private final InputStream parent;

    public FaweInputStream(InputStream parent) {
        super(parent);
        this.parent = parent;
    }

    public InputStream getParent() {
        return parent;
    }

    public int readMedium() throws IOException {
        return (int) (
                (read() << 16) +
                        (read() << 8) +
                        read());
    }

    private NBTInputStream nbtIn;

    public void skipFully(int num) throws IOException {
        long skipped = skip(num);
        while (skipped != num) {
            skipped += skip(num - skipped);
        }
    }

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
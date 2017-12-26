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

    public Object readPrimitive(Class<?> clazz) throws IOException {
        if (clazz == long.class || clazz == Long.class) {
            return readLong();
        } else if (clazz == double.class || clazz == Double.class) {
            return readDouble();
        } else if (clazz == float.class || clazz == Float.class) {
            return readFloat();
        } else if (clazz == int.class || clazz == Integer.class) {
            return readInt();
        } else if (clazz == short.class || clazz == Short.class) {
            return readShort();
        } else if (clazz == char.class || clazz == Character.class) {
            return readChar();
        } else if (clazz == byte.class || clazz == Byte.class) {
            return readByte();
        } else if (clazz == boolean.class || clazz == Boolean.class) {
            return readBoolean();
        } else {
            throw new UnsupportedOperationException("Unknown class " + clazz);
        }
    }

    public int readVarInt() throws IOException {
        int i = 0;
        int offset = 0;
        int b;
        while ((b = read()) > 127) {
            i |= (b - 128) << offset;
            offset += 7;
        }
        i |= b << offset;
        return i;
    }

    @Override
    public void close() throws IOException {
        if (nbtIn != null) {
            nbtIn.close();
        }
        parent.close();
    }
}
package com.boydti.fawe.bukkit.v1_11;

import com.boydti.fawe.object.FaweQueue;

public class BukkitChunk_1_11_Copy extends BukkitChunk_1_11 {
    public final byte[][] idsBytes;
    public final byte[][] datasBytes;

    public BukkitChunk_1_11_Copy(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        idsBytes = new byte[16][];
        datasBytes = new byte[16][];
    }

    public void set(int i, byte[] ids, byte[] data) {
        this.idsBytes[i] = ids;
        this.datasBytes[i] = data;
    }

    @Override
    public char[][] getCombinedIdArrays() {
        for (int i = 0; i < ids.length; i++) {
            getIdArray(i);
        }
        return super.getCombinedIdArrays();
    }

    @Override
    public char[] getIdArray(int i) {
        char[] combined = this.ids[i];
        if (combined != null) {
            return combined;
        }
        byte[] idsBytesArray = idsBytes[i];
        if (idsBytesArray == null) {
            return null;
        }
        byte[] datasBytesArray = datasBytes[i];

        idsBytes[i] = null;
        datasBytes[i] = null;

        this.ids[i] = combined = new char[4096];
        for (int j = 0, k = 0; j < 2048; j++, k += 2) {
            combined[k] = (char) (((idsBytesArray[k] & 0xFF) << 4) + (datasBytesArray[j] & 15));
        }
        for (int j = 0, k = 1; j < 2048; j++, k += 2) {
            combined[k] = (char) (((idsBytesArray[k] & 0xFF) << 4) + ((datasBytesArray[j] >> 4) & 15));
        }
        return combined;
    }

    @Override
    public void setBlock(int x, int y, int z, int id) {
        throw new UnsupportedOperationException("This chunk is an immutable copy");
    }

    @Override
    public void setBlock(int x, int y, int z, int id, int data) {
        throw new UnsupportedOperationException("This chunk is an immutable copy");
    }
}

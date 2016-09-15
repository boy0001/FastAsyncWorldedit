package com.boydti.fawe.object.changeset;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArraysInputStream;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.world.World;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ChangeSet optimized for low memory usage
 *  - No disk usage
 *  - High CPU usage
 *  - Low memory usage
 */
public class MemoryOptimizedHistory extends FaweStreamChangeSet {

    private int size = 0;
    private byte[][] ids;
    private FastByteArrayOutputStream idsStream;
    private FaweOutputStream idsStreamZip;

    private byte[][] entC;
    private FastByteArrayOutputStream entCStream;
    private NBTOutputStream entCStreamZip;

    private byte[][] entR;
    private FastByteArrayOutputStream entRStream;
    private NBTOutputStream entRStreamZip;

    private byte[][] tileC;
    private FastByteArrayOutputStream tileCStream;
    private NBTOutputStream tileCStreamZip;

    private byte[][] tileR;
    private FastByteArrayOutputStream tileRStream;
    private NBTOutputStream tileRStreamZip;

    public MemoryOptimizedHistory(World world) {
        super(world);
    }

    @Override
    public boolean flush() {
        super.flush();
        try {
            if (idsStream != null) {
                idsStreamZip.close();
                size = idsStream.getSize();
                ids = idsStream.toByteArrays();
                idsStream = null;
                idsStreamZip = null;
            }
            if (entCStream != null) {
                entCStreamZip.close();
                entC = entCStream.toByteArrays();
                entCStream = null;
                entCStreamZip = null;
            }
            if (entRStream != null) {
                entRStreamZip.close();
                entR = entRStream.toByteArrays();
                entRStream = null;
                entRStreamZip = null;
            }
            if (tileCStream != null) {
                tileCStreamZip.close();
                tileC = tileCStream.toByteArrays();
                tileCStream = null;
                tileCStreamZip = null;
            }
            if (tileRStream != null) {
                tileRStreamZip.close();
                tileR = tileRStream.toByteArrays();
                tileRStream = null;
                tileRStreamZip = null;
            }
            return true;
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
        return false;
    }

    @Override
    public int getCompressedSize() {
        return ids == null ? 0 : ids.length;
    }

    @Override
    public long getSizeInMemory() {
        return 92 + getCompressedSize();
    }

    @Override
    public OutputStream getBlockOS(int x, int y, int z) throws IOException {
        if (idsStreamZip != null) {
            return idsStreamZip;
        }
        setOrigin(x, z);
        idsStream = new FastByteArrayOutputStream(Settings.HISTORY.BUFFER_SIZE);
        idsStreamZip = getCompressedOS(idsStream);
        idsStreamZip.write(FaweStreamChangeSet.MODE);
        idsStreamZip.writeInt(x);
        idsStreamZip.writeInt(z);
        return idsStreamZip;
    }

    @Override
    public InputStream getBlockIS() throws IOException {
        if (ids == null) {
            return null;
        }
        FaweInputStream result = MainUtil.getCompressedIS(new FastByteArraysInputStream(ids));
        result.skip(FaweStreamChangeSet.HEADER_SIZE);
        return result;
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        if (entCStreamZip != null) {
            return entCStreamZip;
        }
        entCStream = new FastByteArrayOutputStream(Settings.HISTORY.BUFFER_SIZE);
        return entCStreamZip = new NBTOutputStream(getCompressedOS(entCStream));
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        if (entRStreamZip != null) {
            return entRStreamZip;
        }
        entRStream = new FastByteArrayOutputStream(Settings.HISTORY.BUFFER_SIZE);
        return entRStreamZip = new NBTOutputStream(getCompressedOS(entRStream));
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        if (tileCStreamZip != null) {
            return tileCStreamZip;
        }
        tileCStream = new FastByteArrayOutputStream(Settings.HISTORY.BUFFER_SIZE);
        return tileCStreamZip = new NBTOutputStream(getCompressedOS(tileCStream));
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
        if (tileRStreamZip != null) {
            return tileRStreamZip;
        }
        tileRStream = new FastByteArrayOutputStream(Settings.HISTORY.BUFFER_SIZE);
        return tileRStreamZip = new NBTOutputStream(getCompressedOS(tileRStream));
    }

    @Override
    public NBTInputStream getEntityCreateIS() throws IOException {
        return entC == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArraysInputStream(entC)));
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        return entR == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArraysInputStream(entR)));
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        return tileC == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArraysInputStream(tileC)));
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        return tileR == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArraysInputStream(tileR)));
    }
}

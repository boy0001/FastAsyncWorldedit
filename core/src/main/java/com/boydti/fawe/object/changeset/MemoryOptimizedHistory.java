package com.boydti.fawe.object.changeset;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.world.World;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private byte[] ids;
    private ByteArrayOutputStream idsStream;
    private FaweOutputStream idsStreamZip;

    private byte[] entC;
    private ByteArrayOutputStream entCStream;
    private NBTOutputStream entCStreamZip;

    private byte[] entR;
    private ByteArrayOutputStream entRStream;
    private NBTOutputStream entRStreamZip;

    private byte[] tileC;
    private ByteArrayOutputStream tileCStream;
    private NBTOutputStream tileCStreamZip;

    private byte[] tileR;
    private ByteArrayOutputStream tileRStream;
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
                ids = idsStream.toByteArray();
                idsStream = null;
                idsStreamZip = null;
            }
            if (entCStream != null) {
                entCStreamZip.close();
                entC = entCStream.toByteArray();
                entCStream = null;
                entCStreamZip = null;
            }
            if (entRStream != null) {
                entRStreamZip.close();
                entR = entRStream.toByteArray();
                entRStream = null;
                entRStreamZip = null;
            }
            if (tileCStream != null) {
                tileCStreamZip.close();
                tileC = tileCStream.toByteArray();
                tileCStream = null;
                tileCStreamZip = null;
            }
            if (tileRStream != null) {
                tileRStreamZip.close();
                tileR = tileRStream.toByteArray();
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
    public OutputStream getBlockOS(int x, int y, int z) throws IOException {
        if (idsStreamZip != null) {
            return idsStreamZip;
        }
        setOrigin(x, z);
        idsStream = new ByteArrayOutputStream(Settings.BUFFER_SIZE);
        idsStreamZip = getCompressedOS(idsStream);
        idsStreamZip.write(FaweStreamChangeSet.MODE);
        idsStreamZip.writeInt(x);
        idsStreamZip.writeInt(z);
        return idsStreamZip;
    }

    @Override
    public InputStream getBlockIS() throws IOException {
        FaweInputStream result = ids == null ? null : MainUtil.getCompressedIS(new ByteArrayInputStream(ids));
        result.skip(FaweStreamChangeSet.HEADER_SIZE);
        return result;
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        if (entCStreamZip != null) {
            return entCStreamZip;
        }
        entCStream = new ByteArrayOutputStream(Settings.BUFFER_SIZE);
        return entCStreamZip = new NBTOutputStream(getCompressedOS(entCStream));
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        if (entRStreamZip != null) {
            return entRStreamZip;
        }
        entRStream = new ByteArrayOutputStream(Settings.BUFFER_SIZE);
        return entRStreamZip = new NBTOutputStream(getCompressedOS(entRStream));
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        if (tileCStreamZip != null) {
            return tileCStreamZip;
        }
        tileCStream = new ByteArrayOutputStream(Settings.BUFFER_SIZE);
        return tileCStreamZip = new NBTOutputStream(getCompressedOS(tileCStream));
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
        if (tileRStreamZip != null) {
            return tileRStreamZip;
        }
        tileRStream = new ByteArrayOutputStream(Settings.BUFFER_SIZE);
        return tileRStreamZip = new NBTOutputStream(getCompressedOS(tileRStream));
    }

    @Override
    public NBTInputStream getEntityCreateIS() throws IOException {
        return entC == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new ByteArrayInputStream(entC)));
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        return entR == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new ByteArrayInputStream(entR)));
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        return tileC == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new ByteArrayInputStream(tileC)));
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        return tileR == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new ByteArrayInputStream(tileR)));
    }
}

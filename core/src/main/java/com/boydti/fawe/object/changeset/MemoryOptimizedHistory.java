package com.boydti.fawe.object.changeset;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.extension.platform.Actor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4InputStream;
import net.jpountz.lz4.LZ4OutputStream;

/**
 * ChangeSet optimized for low memory usage
 *  - No disk usage
 *  - High CPU usage
 *  - Low memory usage
 */
public class MemoryOptimizedHistory extends FaweStreamChangeSet {

    private final Actor actor;

    public MemoryOptimizedHistory(Actor actor) {
        this.actor = actor;
    }

    @Override
    public boolean flush() {
        if (idsStreamZip != null) {
            try {
                idsStream.flush();
                idsStreamZip.flush();
                idsStreamZip.close();
                ids = idsStream.toByteArray(true);
                idsStream = null;
                idsStreamZip = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    @Override
    public int getCompressedSize() {
        return ids == null ? 0 : ids.length;
    }

    private byte[] ids;
    private FastByteArrayOutputStream idsStream;
    private OutputStream idsStreamZip;

    @Override
    public OutputStream getBlockOS(int x, int y, int z) throws IOException {
        if (idsStreamZip != null) {
            return idsStreamZip;
        }
        LZ4Factory factory = LZ4Factory.fastestInstance();
        idsStream = new FastByteArrayOutputStream(Settings.BUFFER_SIZE);
        idsStreamZip = new LZ4OutputStream(idsStream, Settings.BUFFER_SIZE, factory.fastCompressor());
        if (Settings.COMPRESSION_LEVEL > 0) {
            idsStreamZip = new LZ4OutputStream(idsStreamZip, Settings.BUFFER_SIZE, factory.highCompressor());
        }
        setOrigin(x, z);
        return idsStreamZip;
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        return null;
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        return null;
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        return null;
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
        return null;
    }

    @Override
    public InputStream getBlockIS() {
        if (ids == null) {
            return null;
        }
        InputStream is = new ByteArrayInputStream(ids);
        is = new LZ4InputStream(is);
        if (Settings.COMPRESSION_LEVEL > 0) {
            is = new LZ4InputStream(is);
        }
        return is;
    }

    @Override
    public NBTInputStream getEntityCreateIS() throws IOException {
        return null;
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        return null;
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        return null;
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        return null;
    }
}

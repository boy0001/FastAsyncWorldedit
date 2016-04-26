package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.BufferedRandomAccessFile;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * A clipboard with disk backed storage. (lower memory + loads on crash)
 *  - Uses an auto closable RandomAccessFile for getting / setting id / data
 *  - I don't know how to reduce nbt / entities to O(1) complexity, so it is stored in memory.
 *
 *  TODO load on join
 */
public class DiskOptimizedClipboard extends FaweClipboard {

    private static int HEADER_SIZE = 10;

    protected int length;
    protected int height;
    protected int width;
    protected int area;

    private final HashMap<IntegerTrio, CompoundTag> nbtMap;
    private final HashSet<ClipboardEntity> entities;
    private final File file;
    private final byte[] buffer;

    private volatile BufferedRandomAccessFile raf;
    private long lastAccessed;
    private int last;

    public DiskOptimizedClipboard(int width, int height, int length, UUID uuid) {
        this(width, height, length, new File(Fawe.imp().getDirectory(), "clipboard" + File.separator + uuid));
    }

    public DiskOptimizedClipboard(File file) {
        nbtMap = new HashMap<>();
        entities = new HashSet<>();this.buffer = new byte[2];
        this.file = file;
        this.lastAccessed = System.currentTimeMillis();
        try {
            this.raf = new BufferedRandomAccessFile(file, "rw", Settings.BUFFER_SIZE);
            raf.setLength(file.length());
            long size = (raf.length() - HEADER_SIZE) >> 1;
            raf.seek(0);
            last = -1;
            raf.read(buffer);
            width = (((buffer[1] & 0xFF) << 8) + ((buffer[0] & 0xFF)));
            raf.read(buffer);
            length = (((buffer[1] & 0xFF) << 8) + ((buffer[0] & 0xFF)));
            height = (int) (size / (width * length));
            area = width * length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        autoCloseTask();
    }

    public BlockArrayClipboard toClipboard() {
        try {
            CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(width - 1, height - 1, length - 1)) {
                @Override
                public boolean contains(Vector position) {
                    return true;
                }
            };
            if (raf == null) {
                open();
            }
            raf.seek(4);
            last = -1;
            int ox = (((byte) raf.read() << 8) | ((byte) raf.read()) & 0xFF);
            int oy = (((byte) raf.read() << 8) | ((byte) raf.read()) & 0xFF);
            int oz = (((byte) raf.read() << 8) | ((byte) raf.read()) & 0xFF);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region, this);
            clipboard.setOrigin(new Vector(ox, oy, oz));
            return clipboard;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DiskOptimizedClipboard(int width, int height, int length, File file) {
        nbtMap = new HashMap<>();
        entities = new HashSet<>();
        this.file = file;
        this.buffer = new byte[2];
        this.lastAccessed = System.currentTimeMillis();
        this.width = width;
        this.height = height;
        this.length = length;
        this.area = width * length;
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setOrigin(Vector offset) {
        try {
            if (raf == null) {
                open();
            }
            raf.seek(4);
            last = -1;
            raf.write((byte) (offset.getBlockX() >> 8));
            raf.write((byte) (offset.getBlockX()));

            raf.write((byte) (offset.getBlockY() >> 8));
            raf.write((byte) (offset.getBlockY()));

            raf.write((byte) (offset.getBlockZ() >> 8));
            raf.write((byte) (offset.getBlockZ()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flush() {
        try {
            raf.close();
            raf = null;
            file.setWritable(true);
            System.gc();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DiskOptimizedClipboard(int width, int height, int length) {
        this(width, height, length, new File(Fawe.imp().getDirectory(), "clipboard" + File.separator + UUID.randomUUID()));
    }

    public void close() {
        try {
            RandomAccessFile tmp = raf;
            raf = null;
            tmp.close();
            tmp = null;
            System.gc();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void open() throws IOException {
        if (raf != null) {
            close();
        }
        this.raf = new BufferedRandomAccessFile(file, "rw", Settings.BUFFER_SIZE);
        long size = width * height * length * 2l;
        if (raf.length() != size) {
            raf.setLength(size + HEADER_SIZE);
            // write length etc
            raf.seek(0);
            last = 0;
            raf.write((width) & 0xff);
            raf.write(((width) >> 8) & 0xff);
            raf.write((length) & 0xff);
            raf.write(((length) >> 8) & 0xff);
        }
        autoCloseTask();
    }

    private void autoCloseTask() {
        TaskManager.IMP.laterAsync(new Runnable() {
            @Override
            public void run() {
                if (raf != null && System.currentTimeMillis() - lastAccessed > 10000) {
                    close();
                } else if (raf == null) {
                    return;
                } else {
                    TaskManager.IMP.laterAsync(this, 200);
                }
            }
        }, 200);
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        try {
            if (raf == null) {
                open();
            }
            lastAccessed = System.currentTimeMillis();
            int i = x + z * width + y * area;
            if (i != last + 1) {
                raf.seek((HEADER_SIZE) + (i << 1));
            }
            raf.read(buffer);
            last = i;
            int id = ((((int) buffer[1] & 0xFF) << 4) + (((int) buffer[0] & 0xFF) >> 4));
            BaseBlock block;
            if (!FaweCache.hasData(id)) {
                block = FaweCache.CACHE_BLOCK[id << 4];
            } else {
                block = FaweCache.CACHE_BLOCK[(id << 4) + (buffer[0] & 0xF)];
            }
            if (FaweCache.hasNBT(id)) {
                CompoundTag nbt = nbtMap.get(new IntegerTrio(x, y, z));
                if (nbt != null) {
                    block = new BaseBlock(block.getId(), block.getData());
                    block.setNbtData(nbt);
                }
            }
            return block;
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return EditSession.nullBlock;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        try {
            if (raf == null) {
                open();
            }
            lastAccessed = System.currentTimeMillis();
            int i = x + z * width + y * area;
            if (i != last + 1) {
                raf.seek((HEADER_SIZE) + (i << 1));
            }
            last = i;
            final int id = block.getId();
            final int data = block.getData();
            int combined = (id << 4) + data;
            buffer[0] = (byte) ((combined) & 0xff);
            buffer[1] = (byte) (((combined) >> 8) & 0xFF);
            raf.write(buffer);
            if (FaweCache.hasNBT(id)) {
                nbtMap.put(new IntegerTrio(x, y, z), block.getNbtData());
            }
            return true;
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
        FaweClipboard.ClipboardEntity ret = new ClipboardEntity(world, x, y, z, yaw, pitch, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public boolean remove(ClipboardEntity clipboardEntity) {
        return entities.remove(clipboardEntity);
    }
}

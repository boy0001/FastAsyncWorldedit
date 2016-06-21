package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.BufferedRandomAccessFile;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector;
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

    public static int COMPRESSION = 0;
    public static int MODE = 0;
    public static int HEADER_SIZE = 14;

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
        this(width, height, length, new File(Fawe.imp().getDirectory(), "clipboard" + File.separator + uuid + ".bd"));
    }

    public DiskOptimizedClipboard(File file) throws IOException {
        nbtMap = new HashMap<>();
        entities = new HashSet<>();this.buffer = new byte[2];
        this.file = file;
        this.lastAccessed = System.currentTimeMillis();
        this.raf = new BufferedRandomAccessFile(file, "rw", Settings.HISTORY.BUFFER_SIZE);
        raf.setLength(file.length());
        long size = (raf.length() - HEADER_SIZE) >> 1;
        raf.seek(2);
        last = -1;
        raf.read(buffer);
        width = (((buffer[1] & 0xFF) << 8) + ((buffer[0] & 0xFF)));
        raf.read(buffer);
        height = (((buffer[1] & 0xFF) << 8) + ((buffer[0] & 0xFF)));
        raf.read(buffer);
        length = (((buffer[1] & 0xFF) << 8) + ((buffer[0] & 0xFF)));
        area = width * length;
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
            raf.seek(8);
            last = -1;
            int ox = (((byte) raf.read() << 8) | ((byte) raf.read()) & 0xFF);
            int oy = (((byte) raf.read() << 8) | ((byte) raf.read()) & 0xFF);
            int oz = (((byte) raf.read() << 8) | ((byte) raf.read()) & 0xFF);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region, this);
            clipboard.setOrigin(new Vector(ox, oy, oz));
            return clipboard;
        } catch (IOException e) {
            MainUtil.handleError(e);
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
            MainUtil.handleError(e);
        }
    }

    @Override
    public void setOrigin(Vector offset) {
        try {
            if (raf == null) {
                open();
            }
            raf.seek(8);
            last = -1;
            raf.write((byte) (offset.getBlockX() >> 8));
            raf.write((byte) (offset.getBlockX()));

            raf.write((byte) (offset.getBlockY() >> 8));
            raf.write((byte) (offset.getBlockY()));

            raf.write((byte) (offset.getBlockZ() >> 8));
            raf.write((byte) (offset.getBlockZ()));
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void flush() {
        try {
            raf.close();
            raf = null;
            file.setWritable(true);
            System.gc();
        } catch (IOException e) {
            MainUtil.handleError(e);
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
            MainUtil.handleError(e);
        }
    }

    public void open() throws IOException {
        if (raf != null) {
            close();
        }
        lastAccessed = System.currentTimeMillis();
        this.raf = new BufferedRandomAccessFile(file, "rw", Settings.HISTORY.BUFFER_SIZE);
        long size = width * height * length * 2l + HEADER_SIZE;
        if (raf.length() != size) {
            raf.setLength(size);
            // write length etc
            raf.seek(1);
            last = 0;
            raf.write((width) & 0xff);
            raf.write(((width) >> 8) & 0xff);
            raf.write((height) & 0xff);
            raf.write(((height) >> 8) & 0xff);
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

    private int ylast;
    private int ylasti;
    private int zlast;
    private int zlasti;

    @Override
    public void forEach(final RunnableVal2<Vector,BaseBlock> task, boolean air) {
        try {
            if (raf == null) {
                open();
            }
            raf.seek(HEADER_SIZE);
            BlockVector pos = new BlockVector(0, 0, 0);
            int x = 0;
            int y = 0;
            int z = 0;
            long len = (raf.length());
            for (long i = HEADER_SIZE; i < len; i+=2) {
                pos.x = x;
                pos.y = y;
                pos.z = z;
                if (++x >= width) {
                    x = 0;
                    if (++z >= length) {
                        z = 0;
                        ++y;
                    }
                }
                raf.seek(i);
                raf.read(buffer);
                int id = ((((int) buffer[1] & 0xFF) << 4) + (((int) buffer[0] & 0xFF) >> 4));
                if (id == 0 && !air) {
                    continue;
                }
                BaseBlock block;
                if (!FaweCache.hasData(id)) {
                    block = FaweCache.CACHE_BLOCK[id << 4];
                } else {
                    block = FaweCache.CACHE_BLOCK[(id << 4) + (buffer[0] & 0xF)];
                }
                if (FaweCache.hasNBT(id)) {
                    CompoundTag nbt = nbtMap.get(new IntegerTrio((int) pos.x, (int) pos.y, (int) pos.z));
                    if (nbt != null) {
                        block = new BaseBlock(block.getId(), block.getData());
                        block.setNbtData(nbt);
                    }
                }
                task.run(pos, block);
            }
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        try {
            if (raf == null) {
                open();
            }
            int i = x + ((ylast == y) ? ylasti : (ylasti = ((ylast = y)) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
            if (i != last + 1) {
                raf.seek((HEADER_SIZE) + (i << 1));
                lastAccessed = System.currentTimeMillis();
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
            MainUtil.handleError(e);
        }
        return EditSession.nullBlock;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        nbtMap.put(new IntegerTrio(x, y, z), tag);
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        try {
            if (raf == null) {
                open();
            }
            int i = x + ((ylast == y) ? ylasti : (ylasti = ((ylast = y)) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
            if (i != last + 1) {
                raf.seek((HEADER_SIZE) + (i << 1));
                lastAccessed = System.currentTimeMillis();
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
            MainUtil.handleError(e);
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

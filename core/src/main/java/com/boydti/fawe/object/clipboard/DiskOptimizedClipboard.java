package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A clipboard with disk backed storage. (lower memory + loads on crash)
 * - Uses an auto closable RandomAccessFile for getting / setting id / data
 * - I don't know how to reduce nbt / entities to O(2) complexity, so it is stored in memory.
 */
public class DiskOptimizedClipboard extends FaweClipboard implements Closeable {

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

    private RandomAccessFile braf;
    private MappedByteBuffer mbb;

    private int last;
    private FileChannel fc;

    public DiskOptimizedClipboard(int width, int height, int length, UUID uuid) {
        this(width, height, length, MainUtil.getFile(Fawe.get() != null ? Fawe.imp().getDirectory() : new File("."), Settings.IMP.PATHS.CLIPBOARD + File.separator + uuid + ".bd"));
    }

    public DiskOptimizedClipboard(File file) {
        try {
            nbtMap = new HashMap<>();
            entities = new HashSet<>();
            this.file = file;
            this.braf = new RandomAccessFile(file, "rw");
            braf.setLength(file.length());
            init();
            long size = (braf.length() - HEADER_SIZE) >> 1;

            mbb.position(2);
            last = Integer.MIN_VALUE;
            width = (int) mbb.getChar();
            height = (int) mbb.getChar();
            length = (int) mbb.getChar();
            area = width * length;
            autoCloseTask();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void init() throws IOException {
        if (this.fc == null) {
            this.fc = braf.getChannel();
            this.mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, file.length());
        }
    }

    @Override
    public Vector getDimensions() {
        return new Vector(width, height, length);
    }

    public BlockArrayClipboard toClipboard() {
        try {
            CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(width - 1, height - 1, length - 1)) {
                @Override
                public boolean contains(Vector position) {
                    return true;
                }
            };
            mbb.position(8);
            last = Integer.MIN_VALUE;
            int ox = mbb.getShort();
            int oy = mbb.getShort();
            int oz = mbb.getShort();
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region, this);
            clipboard.setOrigin(new Vector(ox, oy, oz));
            return clipboard;
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return null;
    }

    public DiskOptimizedClipboard(int width, int height, int length, File file) {
        try {
            nbtMap = new HashMap<>();
            entities = new HashSet<>();
            this.file = file;
            this.width = width;
            this.height = height;
            this.length = length;
            this.area = width * length;
            try {
                if (!file.exists()) {
                    File parent = file.getParentFile();
                    if (parent != null) {
                        file.getParentFile().mkdirs();
                    }
                    file.createNewFile();
                }
            } catch (Exception e) {
                MainUtil.handleError(e);
            }
            this.braf = new RandomAccessFile(file, "rw");
            long volume = (long) width * (long) height * (long) length * 2l + (long) HEADER_SIZE;
            braf.setLength(0);
            braf.setLength(volume);
            if (width * height * length != 0) {
                init();
                // write length etc
                mbb.position(2);
                last = Integer.MIN_VALUE;
                mbb.putChar((char) width);
                mbb.putChar((char) height);
                mbb.putChar((char) length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setOrigin(Vector offset) {
        try {
            mbb.position(8);
            last = Integer.MIN_VALUE;
            mbb.putShort((short) offset.getBlockX());
            mbb.putShort((short) offset.getBlockY());
            mbb.putShort((short) offset.getBlockZ());
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void setDimensions(Vector dimensions) {
        try {
            width = dimensions.getBlockX();
            height = dimensions.getBlockY();
            length = dimensions.getBlockZ();
            area = width * length;
            long size = width * height * length * 2l + HEADER_SIZE;
            braf.setLength(size);
            init();
            mbb.position(2);
            last = Integer.MIN_VALUE;
            mbb.putChar((char) width);
            mbb.putChar((char) height);
            mbb.putChar((char) length);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void flush() {
        mbb.force();
    }

    public DiskOptimizedClipboard(int width, int height, int length) {
        this(width, height, length, MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.CLIPBOARD + File.separator + UUID.randomUUID() + ".bd"));
    }

    private void closeDirectBuffer(ByteBuffer cb) {
        if (cb == null || !cb.isDirect()) return;

        // we could use this type cast and call functions without reflection code,
        // but static import from sun.* package is risky for non-SUN virtual machine.
        //try { ((sun.nio.ch.DirectBuffer)cb).cleaner().clean(); } catch (Exception ex) { }
        try {
            Method cleaner = cb.getClass().getMethod("cleaner");
            cleaner.setAccessible(true);
            Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
            clean.setAccessible(true);
            clean.invoke(cleaner.invoke(cb));
        } catch (Exception ex) {
        }
        cb = null;
    }

    @Override
    public void close() {
        try {
            mbb.force();
            fc.close();
            braf.close();
            file.setWritable(true);
            closeDirectBuffer(mbb);
            mbb = null;
            fc = null;
            braf = null;
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    private void autoCloseTask() {
//        TaskManager.IMP.laterAsync(new Runnable() {
//            @Override
//            public void run() {
//                if (raf != null && System.currentTimeMillis() - lastAccessed > 10000) {
//                    close();
//                } else if (raf == null) {
//                    return;
//                } else {
//                    TaskManager.IMP.laterAsync(this, 200);
//                }
//            }
//        }, 200);
    }

    private int ylast;
    private int ylasti;
    private int zlast;
    private int zlasti;

    @Override
    public void streamIds(NBTStreamer.ByteReader task) {
        try {
            mbb.force();
            mbb.position(HEADER_SIZE);
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int combinedId = mbb.getChar();
                        task.run(index++, FaweCache.getId(combinedId));
                    }
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void streamDatas(NBTStreamer.ByteReader task) {
        try {
            mbb.force();
            mbb.position(HEADER_SIZE);
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int combinedId = mbb.getChar();
                        task.run(index++, FaweCache.getData(combinedId));
                    }
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        return new ArrayList<>(nbtMap.values());
    }

    @Override
    public void forEach(final BlockReader task, boolean air) {
        try {
            mbb.force();
            mbb.position(HEADER_SIZE);
            IntegerTrio trio = new IntegerTrio();
            final boolean hasTile = !nbtMap.isEmpty();
            if (air) {
                if (hasTile) {
                    for (int y = 0; y < height; y++) {
                        for (int z = 0; z < length; z++) {
                            for (int x = 0; x < width; x++) {
                                char combinedId = mbb.getChar();
                                BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
                                if (block.canStoreNBTData()) {
                                    trio.set(x, y, z);
                                    CompoundTag nbt = nbtMap.get(trio);
                                    if (nbt != null) {
                                        block = new BaseBlock(block.getId(), block.getData());
                                        block.setNbtData(nbt);
                                    }
                                }
                                task.run(x, y, z, block);
                            }
                        }
                    }
                } else {
                    for (int y = 0; y < height; y++) {
                        for (int z = 0; z < length; z++) {
                            for (int x = 0; x < width; x++) {
                                char combinedId = mbb.getChar();
                                BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
                                task.run(x, y, z, block);
                            }
                        }
                    }
                }
            } else {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            int combinedId = mbb.getChar();
                            if (combinedId != 0) {
                                BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
                                if (block.canStoreNBTData()) {
                                    trio.set(x, y, z);
                                    CompoundTag nbt = nbtMap.get(trio);
                                    if (nbt != null) {
                                        block = new BaseBlock(block.getId(), block.getData());
                                        block.setNbtData(nbt);
                                    }
                                }
                                task.run(x, y, z, block);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    public int getIndex(int x, int y, int z) {
        return x + ((ylast == y) ? ylasti : (ylasti = (ylast = y) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        try {
            int i = getIndex(x, y, z);
            if (i != last + 1) {
                mbb.position((HEADER_SIZE) + (i << 1));
            }
            last = i;
            int combinedId = mbb.getChar();
            BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
            if (block.canStoreNBTData()) {
                CompoundTag nbt = nbtMap.get(new IntegerTrio(x, y, z));
                if (nbt != null) {
                    block = new BaseBlock(block.getId(), block.getData());
                    block.setNbtData(nbt);
                }
            }
            return block;
        } catch (Exception e) {
            MainUtil.handleError(e);
        }
        return EditSession.nullBlock;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        nbtMap.put(new IntegerTrio(x, y, z), tag);
        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
        values.put("x", new IntTag(x));
        values.put("y", new IntTag(y));
        values.put("z", new IntTag(z));
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        try {
            int i = x + ((ylast == y) ? ylasti : (ylasti = ((ylast = y)) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
            if (i != last + 1) {
                mbb.position((HEADER_SIZE) + (i << 1));
            }
            last = i;
            final int id = block.getId();
            final int data = block.getData();
            int combined = (id << 4) + data;
            mbb.putChar((char) combined);
            CompoundTag tile = block.getNbtData();
            if (tile != null) {
                setTile(x, y, z, tile);
            }
            return true;
        } catch (Exception e) {
            MainUtil.handleError(e);
        }
        return false;
    }

    @Override
    public void setId(int i, int id) {
        int index;
        if (i != last + 1) {
            index = (HEADER_SIZE) + (i << 1);
        } else {
            index = mbb.position();
        }
        last = i;
        mbb.position(index + 1);
        // 00000000 00000000
        // [    id     ]data
        byte id2 = mbb.get();
        mbb.position(index);
        mbb.put((byte) (id >> 4));
        mbb.put((byte) (((id & 0xFF) << 4) + (id2 & 0xFF)));
    }

    public void setCombined(int i, int combined) {
        if (i != last + 1) {
            mbb.position((HEADER_SIZE) + (i << 1));
        }
        last = i;
        mbb.putChar((char) combined);
    }

    @Override
    public void setAdd(int i, int add) {
        last = i;
        int index = (HEADER_SIZE) + (i << 1);
        mbb.position(index);
        // 00000000 00000000
        // [    id     ]data
        char combined = mbb.getChar();
        mbb.position(index);
        mbb.putChar((char) ((combined & 0xFFFF) + (add << 12)));
    }

    @Override
    public void setData(int i, int data) {
        int index;
        if (i != last + 1) {
            index = (HEADER_SIZE) + (i << 1) + 1;
        } else {
            index = mbb.position() + 1;
        }
        mbb.position(index);
        last = i;
        byte id = mbb.get();
        mbb.position(index);
        mbb.put((byte) ((id & 0xF0) + data));
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

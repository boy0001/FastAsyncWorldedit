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
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
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
    protected int volume;

    private final HashMap<IntegerTrio, CompoundTag> nbtMap;
    private final HashSet<ClipboardEntity> entities;
    private final File file;

    private RandomAccessFile braf;
    private MappedByteBuffer mbb;

    private FileChannel fc;
    private boolean hasBiomes;

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
            width = (int) mbb.getChar(2);
            height = (int) mbb.getChar(4);
            length = (int) mbb.getChar(6);
            area = width * length;
            this.volume = length * width * height;

            if ((braf.length() - HEADER_SIZE) == (volume << 1) + area) {
                hasBiomes = true;
            }
            autoCloseTask();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getFile() {
        return file;
    }

    private void init() throws IOException {
        if (this.fc == null) {
            this.fc = braf.getChannel();
            this.mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, file.length());
        }
    }

    private boolean initBiome() {
        if (!hasBiomes) {
            try {
                hasBiomes = true;
                close();
                this.braf = new RandomAccessFile(file, "rw");
                this.braf.setLength(HEADER_SIZE + (volume << 1) + area);
                init();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasBiomes() {
        return hasBiomes;
    }

    @Override
    public boolean setBiome(int x, int z, int biome) {
        setBiome(getIndex(x, 0, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, int biome) {
        if (initBiome()) {
            mbb.put(HEADER_SIZE + (volume << 1) + index, (byte) biome);
        }
    }

    @Override
    public BaseBiome getBiome(int index) {
        if (!hasBiomes()) {
            return EditSession.nullBiome;
        }
        int biomeId = mbb.get(HEADER_SIZE + (volume << 1) + index) & 0xFF;
        return FaweCache.CACHE_BIOME[biomeId];
    }

    @Override
    public void streamBiomes(NBTStreamer.ByteReader task) {
        if (!hasBiomes()) return;
        int index = 0;
        int mbbIndex = HEADER_SIZE + (volume << 1);
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < width; x++, index++, mbbIndex++) {
                int biome = mbb.get(mbbIndex) & 0xFF;
                task.run(index, biome);
            }
        }
    }

    @Override
    public BaseBiome getBiome(int x, int z) {
        return getBiome(getIndex(x, 0, z));
    }

    @Override
    public Vector getDimensions() {
        return new Vector(width, height, length);
    }

    public BlockArrayClipboard toClipboard() {
        try {
            CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(width - 1, height - 1, length - 1));
            int ox = mbb.getShort(8);
            int oy = mbb.getShort(10);
            int oz = mbb.getShort(12);
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
            this.volume = width * length * height;
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
                mbb.putChar(2, (char) width);
                mbb.putChar(4, (char) height);
                mbb.putChar(6, (char) length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setOrigin(Vector offset) {
        try {
            mbb.putShort(8, (short) offset.getBlockX());
            mbb.putShort(10, (short) offset.getBlockY());
            mbb.putShort(12, (short) offset.getBlockZ());
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
            volume = width * length * height;
            long size = width * height * length * 2l + HEADER_SIZE + (hasBiomes() ? area : 0);
            if (braf.length() < size) {
                close();
                this.braf = new RandomAccessFile(file, "rw");
                braf.setLength(size);
                init();
            }
            mbb.putChar(2, (char) width);
            mbb.putChar(4, (char) height);
            mbb.putChar(6, (char) length);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void flush() {
        mbb.force();
    }

    public DiskOptimizedClipboard(int width, int height, int length) {
        this(width, height, length, MainUtil.getFile(Fawe.imp() != null ? Fawe.imp().getDirectory() : new File("."), Settings.IMP.PATHS.CLIPBOARD + File.separator + UUID.randomUUID() + ".bd"));
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
            try {
                final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                final Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                final Object theUnsafe = theUnsafeField.get(null);
                final Method invokeCleanerMethod = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
                invokeCleanerMethod.invoke(theUnsafe, cb);
            } catch (Exception e) {
                System.gc();
            }
        }
        cb = null;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    @Override
    public void close() {
        try {
            if (mbb != null) {
                mbb.force();
                fc.close();
                braf.close();
                file.setWritable(true);
                closeDirectBuffer(mbb);
                mbb = null;
                fc = null;
                braf = null;
            }
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
            int pos = HEADER_SIZE;
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, pos += 2) {
                        int combinedId = mbb.getChar(pos);
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
            int pos = HEADER_SIZE;
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, pos += 2) {
                        int combinedId = mbb.getChar(pos);
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
        mbb.force();
        int pos = HEADER_SIZE;
        IntegerTrio trio = new IntegerTrio();
        final boolean hasTile = !nbtMap.isEmpty();
        if (air) {
            if (hasTile) {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++, pos += 2) {
                            char combinedId = mbb.getChar(pos);
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
                        for (int x = 0; x < width; x++, pos += 2) {
                            char combinedId = mbb.getChar(pos);
                            BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
                            task.run(x, y, z, block);
                        }
                    }
                }
            }
        } else {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, pos += 2) {
                        int combinedId = mbb.getChar(pos);
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
    }

    public int getIndex(int x, int y, int z) {
        return x + ((ylast == y) ? ylasti : (ylasti = (ylast = y) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        try {
            int index = HEADER_SIZE + (getIndex(x, y, z) << 1);
            int combinedId = mbb.getChar(index);
            BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
            if (block.canStoreNBTData() && !nbtMap.isEmpty()) {
                CompoundTag nbt = nbtMap.get(new IntegerTrio(x, y, z));
                if (nbt != null) {
                    block = new BaseBlock(block.getId(), block.getData());
                    block.setNbtData(nbt);
                }
            }
            return block;
        } catch (IndexOutOfBoundsException ignore) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return EditSession.nullBlock;
    }

    @Override
    public BaseBlock getBlock(int i) {
        try {
            int diskIndex = (HEADER_SIZE) + (i << 1);
            int combinedId = mbb.getChar(diskIndex);
            BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
            if (block.canStoreNBTData() && !nbtMap.isEmpty()) {
                CompoundTag nbt;
                if (nbtMap.size() < 4) {
                    nbt = null;
                    for (Map.Entry<IntegerTrio, CompoundTag> entry : nbtMap.entrySet()) {
                        IntegerTrio key = entry.getKey();
                        int index = getIndex(key.x, key.y, key.z);
                        if (index == i) {
                            nbt = entry.getValue();
                            break;
                        }
                    }
                } else {
                    // x + z * width + y * area;
                    int y = i / area;
                    int newI = (i - (y * area));
                    int z = newI / width;
                    int x = newI - z * width;
                    nbt = nbtMap.get(new IntegerTrio(x, y, z));
                }
                if (nbt != null) {
                    block = new BaseBlock(block.getId(), block.getData());
                    block.setNbtData(nbt);
                }
            }
            return block;
        } catch (IndexOutOfBoundsException ignore) {
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            int index = (HEADER_SIZE) + (getIndex(x, y, z) << 1);
            final int id = block.getId();
            final int data = block.getData();
            int combined = (id << 4) + data;
            mbb.putChar(index, (char) combined);
            CompoundTag tile = block.getNbtData();
            if (tile != null) {
                setTile(x, y, z, tile);
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setId(int i, int id) {
        int index = (HEADER_SIZE) + (i << 1);
        // 00000000 00000000
        // [    id     ]data
        char combined = mbb.getChar(index);
        mbb.putChar(index, (char) ((combined & 0xF00F) + (id << 4)));
    }

    public void setCombined(int i, int combined) {
        mbb.putChar((HEADER_SIZE) + (i << 1), (char) combined);
    }

    @Override
    public void setAdd(int i, int add) {
        int index = (HEADER_SIZE) + (i << 1);
        // 00000000 00000000
        // [    id     ]data
        char combined = mbb.getChar(index);
        mbb.putChar(index, (char) ((combined & 0x0FFF) + (add << 12)));
    }

    @Override
    public void setData(int i, int data) {
        int index = (HEADER_SIZE) + (i << 1) + 1;
        byte id = mbb.get(index);
        mbb.put(index, (byte) ((id & 0xF0) + data));
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

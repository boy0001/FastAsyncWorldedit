package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.io.BufferedRandomAccessFile;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A clipboard with disk backed storage. (lower memory + loads on crash)
 *  - Uses an auto closable RandomAccessFile for getting / setting id / data
 *  - I don't know how to reduce nbt / entities to O(2) complexity, so it is stored in memory.
 *
 *  TODO load on join
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
    private final byte[] buffer;

    private final BufferedRandomAccessFile raf;
    private int last;

    public DiskOptimizedClipboard(int width, int height, int length, UUID uuid) {
        this(width, height, length, MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.CLIPBOARD + File.separator + uuid + ".bd"));
    }

    public DiskOptimizedClipboard(File file) {
        try {
            nbtMap = new HashMap<>();
            entities = new HashSet<>();
            this.buffer = new byte[2];
            this.file = file;
            this.raf = new BufferedRandomAccessFile(file, "rw", 16);
            raf.setLength(file.length());
            long size = (raf.length() - HEADER_SIZE) >> 1;
            raf.seek(2);
            last = Integer.MIN_VALUE;
            width = (int) raf.readChar();
            height = (int) raf.readChar();
            length = (int) raf.readChar();
            area = width * length;
            autoCloseTask();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            raf.seek(8);
            last = Integer.MIN_VALUE;
            int ox = raf.readShort();
            int oy = raf.readShort();
            int oz = raf.readShort();
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region, this);
            clipboard.setOrigin(new Vector(ox, oy, oz));
            return clipboard;
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
        return null;
    }

    public DiskOptimizedClipboard(int width, int height, int length, File file) {
        try {
            nbtMap = new HashMap<>();
            entities = new HashSet<>();
            this.file = file;
            this.buffer = new byte[2];
            this.width = width;
            this.height = height;
            this.length = length;
            this.area = width * length;
            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
            } catch (Exception e) {
                MainUtil.handleError(e);
            }
            this.raf = new BufferedRandomAccessFile(file, "rw", 16);
            long volume = width * height * length * 2l + HEADER_SIZE;
            raf.setLength(0);
            raf.setLength(volume);
            // write length etc
            raf.seek(2);
            last = Integer.MIN_VALUE;
            raf.writeChar(width);
            raf.writeChar(height);
            raf.writeChar(length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setOrigin(Vector offset) {
        try {
            raf.seek(8);
            last = Integer.MIN_VALUE;
            raf.writeShort(offset.getBlockX());
            raf.writeShort(offset.getBlockY());
            raf.writeShort(offset.getBlockZ());
        } catch (IOException e) {
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
            raf.setLength(size);
            raf.seek(2);
            last = Integer.MIN_VALUE;
            raf.writeChar(width);
            raf.writeChar(height);
            raf.writeChar(length);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void flush() {
        try {
            raf.flush();
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public DiskOptimizedClipboard(int width, int height, int length) {
        this(width, height, length, MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.CLIPBOARD + File.separator + UUID.randomUUID() + ".bd"));
    }

    public void close() {
        try {
            raf.close();
            file.setWritable(true);
            System.gc();
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
            raf.seek(HEADER_SIZE);
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int combinedId = raf.readChar();
                        task.run(index++, FaweCache.getId(combinedId));
                    }
                }
            }
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void streamDatas(NBTStreamer.ByteReader task) {
        try {
            raf.seek(HEADER_SIZE);
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int combinedId = raf.readChar();
                        task.run(index++, FaweCache.getData(combinedId));
                    }
                }
            }
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        return new ArrayList<>(nbtMap.values());
    }

    @Override
    public void forEach(final RunnableVal2<Vector,BaseBlock> task, boolean air) {
        try {
            raf.seek(HEADER_SIZE);
            BlockVector pos = new BlockVector(0, 0, 0);
            IntegerTrio trio = new IntegerTrio();
            if (air) {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            int combinedId = raf.readChar();
                            BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
                            if (FaweCache.hasNBT(block.getId())) {
                                trio.set(x, y, z);
                                CompoundTag nbt = nbtMap.get(trio);
                                if (nbt != null) {
                                    block = new BaseBlock(block.getId(), block.getData());
                                    block.setNbtData(nbt);
                                }
                            }
                            pos.mutX(x);
                            pos.mutY(y);
                            pos.mutZ(z);
                            task.run(pos, block);
                        }
                    }
                }
            } else {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            int combinedId = raf.readChar();
                            if (combinedId != 0) {
                                BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
                                if (FaweCache.hasNBT(block.getId())) {
                                    trio.set(x, y, z);
                                    CompoundTag nbt = nbtMap.get(trio);
                                    if (nbt != null) {
                                        block = new BaseBlock(block.getId(), block.getData());
                                        block.setNbtData(nbt);
                                    }
                                }
                                pos.mutX(x);
                                pos.mutY(y);
                                pos.mutZ(z);
                                task.run(pos, block);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
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
                raf.seek((HEADER_SIZE) + (i << 1));
            }
            last = i;
            int combinedId = raf.readChar();
            BaseBlock block = FaweCache.CACHE_BLOCK[combinedId];
            if (FaweCache.hasNBT(block.getId())) {
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
                raf.seek((HEADER_SIZE) + (i << 1));
            }
            last = i;
            final int id = block.getId();
            final int data = block.getData();
            int combined = (id << 4) + data;
            raf.writeChar(combined);
            CompoundTag tile = block.getNbtData();
            if (tile != null) {
                setTile(x, y, z, tile);
            }
            return true;
        }  catch (Exception e) {
            MainUtil.handleError(e);
        }
        return false;
    }

    @Override
    public void setId(int i, int id) {
        try {
            if (i != last + 1) {
                raf.seek((HEADER_SIZE) + (i << 1));
            }
            last = i;
            // 00000000 00000000
            // [    id     ]data
            int id1 = raf.readCurrent();
            raf.writeUnsafe(id >> 4);
            int id2 = raf.readCurrent();
            raf.writeUnsafe(((id & 0xFF) << 4) + (id2 & 0xFF));
        }  catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    public void setCombined(int i, int combined) {
        try {
            if (i != last + 1) {
                raf.seek((HEADER_SIZE) + (i << 1));
            }
            last = i;
            raf.writeChar(combined);
        }  catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void setAdd(int i, int add) {
        try {
            if (i != last + 1) {
                raf.seek((HEADER_SIZE) + (i << 1));
            }
            last = i;
            // 00000000 00000000
            // [    id     ]data
            int id = (raf.readCurrent() & 0xFF);
            raf.writeUnsafe(id + (add >> 4));
            raf.read1();
        }  catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void setData(int i, int data) {
        try {
            if (i != last + 1) {
                raf.seek((HEADER_SIZE) + (i << 1) + 1);
            } else {
                raf.seek(raf.getFilePointer() + 1);
            }
            last = i;
            // 00000000 00000000
            // [    id     ]data
//            int skip = raf.read1();
            int id2 = raf.readCurrent();
            raf.writeUnsafe((id2 & 0xF0) + data);
        }  catch (Exception e) {
            MainUtil.handleError(e);
        }
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

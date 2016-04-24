package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
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

    private final HashMap<IntegerTrio, CompoundTag> nbtMap;
    private final HashSet<ClipboardEntity> entities;
    private final File file;
    private final byte[] buffer;

    private volatile RandomAccessFile raf;
    private long lastAccessed;
    private int last;

    public DiskOptimizedClipboard(int width, int height, int length, File file) {
        super(width, height, length);
        nbtMap = new HashMap<>();
        entities = new HashSet<>();
        this.file = file;
        this.buffer = new byte[2];
        this.lastAccessed = System.currentTimeMillis();
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DiskOptimizedClipboard(int width, int height, int length) {
        this(width, height, length, new File(Fawe.imp().getDirectory(), "clipboard" + File.separator + UUID.randomUUID()));
    }



    public void open() throws IOException {
        this.raf = new RandomAccessFile(file, "rw");
        long size = width * height * length * 2l;
        if (raf.length() != size) {
            raf.setLength(size);
        }
        TaskManager.IMP.laterAsync(new Runnable() {
            @Override
            public void run() {
                if (raf != null && System.currentTimeMillis() - lastAccessed > 10000) {
                    try {
                        RandomAccessFile tmp = raf;
                        raf = null;
                        tmp.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                raf.seek(i << 1);
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
                raf.seek(i << 1);
            }
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

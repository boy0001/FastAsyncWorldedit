package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.object.RunnableVal2;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CPUOptimizedClipboard extends FaweClipboard {
    private int length;
    private int height;
    private int width;
    private int area;
    private int volume;
    private byte[] ids;
    private byte[] datas;
    private byte[] add;

    private final HashMap<IntegerTrio, CompoundTag> nbtMapLoc;
    private final HashMap<Integer, CompoundTag> nbtMapIndex;

    private final HashSet<ClipboardEntity> entities;

    public CPUOptimizedClipboard(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.area = width * length;
        this.volume = area * height;
        ids = new byte[volume];
        datas = new byte[volume];
        nbtMapLoc = new HashMap<>();
        nbtMapIndex = new HashMap<>();
        entities = new HashSet<>();
    }


    public void convertTilesToIndex() {
        if (nbtMapLoc.isEmpty()) {
            return;
        }
        for (Map.Entry<IntegerTrio, CompoundTag> entry : nbtMapLoc.entrySet()) {
            IntegerTrio key = entry.getKey();
            nbtMapIndex.put(getIndex(key.x, key.y, key.z), entry.getValue());
        }
        nbtMapLoc.clear();
    }

    private CompoundTag getTag(int index) {
        convertTilesToIndex();
        return nbtMapIndex.get(index);
    }

    public int getId(int index) {
        if (add != null) {
            return ids[index] & 0xFF + add[index] & 0xFF;
        }
        return ids[index] & 0xFF;
    }

    public int getData(int index) {
        return datas[index];
    }

    @Override
    public void setDimensions(Vector dimensions) {
        width = dimensions.getBlockX();
        height = dimensions.getBlockY();
        length = dimensions.getBlockZ();
        area = width * length;
        int newVolume = area * height;
        if (newVolume != volume) {
            volume = newVolume;
            ids = new byte[volume];
            datas = new byte[volume];
        }
    }

    @Override
    public Vector getDimensions() {
        return new Vector(width, height, length);
    }

    @Override
    public void setAdd(int index, int value) {
        if (value == 0) {
            return;
        }
        if (this.add == null) {
            add = new byte[volume];
        }
        add[index] = (byte) value;
    }

    @Override
    public void setId(int index, int value) {
        ids[index] = (byte) value;
    }

    @Override
    public void setData(int index, int value) {
        datas[index] = (byte) value;
    }

    private int ylast;
    private int ylasti;
    private int zlast;
    private int zlasti;

    public int getIndex(int x, int y, int z) {
        return x + ((ylast == y) ? ylasti : (ylasti = (ylast = y) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        int index = getIndex(x, y, z);
        return getBlock(index);
    }

    public BaseBlock getBlock(int index) {
        int id = getId(index);
        if (id == 0) {
            return FaweCache.CACHE_BLOCK[0];
        }
        BaseBlock block;
        if (FaweCache.hasData(id)) {
            block = FaweCache.getBlock(id, getData(index));
        } else {
            block = FaweCache.getBlock(id, 0);
        }
        if (FaweCache.hasNBT(id)) {
            CompoundTag nbt = getTag(index);
            if (nbt != null) {
                block = new BaseBlock(block.getId(), block.getData());
                block.setNbtData(nbt);
            }
        }
        return block;
    }

    @Override
    public void forEach(final RunnableVal2<Vector,BaseBlock> task, boolean air) {
        task.value1 = new Vector(0, 0, 0);
        for (int y = 0, index = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++, index++) {
                    task.value2 = getBlock(index);
                    if (!air && task.value2.getId() == 0) {
                        continue;
                    }
                    task.value1.x = x;
                    task.value1.y = y;
                    task.value1.z = z;
                    task.run();
                }
            }
        }
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        nbtMapLoc.put(new IntegerTrio(x, y, z), tag);
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        return setBlock(getIndex(x, y, z), block);
    }

    public boolean setBlock(int index, BaseBlock block) {
        setId(index, (byte) block.getId());
        setData(index, (byte) block.getData());
        CompoundTag tile = block.getNbtData();
        if (tile != null) {
            nbtMapIndex.put(index, tile);
        }
        return true;
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

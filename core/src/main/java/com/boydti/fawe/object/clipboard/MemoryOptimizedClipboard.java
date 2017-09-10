package com.boydti.fawe.object.clipboard;

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
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MemoryOptimizedClipboard extends FaweClipboard {

    public static final int BLOCK_SIZE = 1048576;
    public static final int BLOCK_MASK = 1048575;
    public static final int BLOCK_SHIFT = 20;

    private int length;
    private int height;
    private int width;
    private int area;
    private int volume;

    private byte[][] ids;
    private byte[][] datas;
    private byte[][] add;

    private byte[] buffer = new byte[MainUtil.getMaxCompressedLength(BLOCK_SIZE)];
    private byte[] biomes = null;

    private final HashMap<IntegerTrio, CompoundTag> nbtMapLoc;
    private final HashMap<Integer, CompoundTag> nbtMapIndex;

    private final HashSet<ClipboardEntity> entities;

    private int lastIdsI = -1;
    private int lastDatasI = -1;
    private int lastAddI = -1;

    private byte[] lastIds;
    private byte[] lastDatas;
    private byte[] lastAdd;

    private boolean saveIds = false;
    private boolean saveDatas = false;
    private boolean saveAdd = false;

    private int compressionLevel;

    public MemoryOptimizedClipboard(int width, int height, int length) {
        this(width, height, length, Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL);
    }

    public MemoryOptimizedClipboard(int width, int height, int length, int compressionLevel) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.area = width * length;
        this.volume = area * height;
        ids = new byte[1 + (volume >> BLOCK_SHIFT)][];
        datas = new byte[1 + (volume >> BLOCK_SHIFT)][];
        nbtMapLoc = new HashMap<>();
        nbtMapIndex = new HashMap<>();
        entities = new HashSet<>();
        this.compressionLevel = compressionLevel;
    }

    public void convertTilesToIndex() {
        if (nbtMapLoc.isEmpty()) {
            return;
        }
        for (Map.Entry<IntegerTrio, CompoundTag> entry : nbtMapLoc.entrySet()) {
            IntegerTrio key = entry.getKey();
            setTile(getIndex(key.x, key.y, key.z), entry.getValue());
        }
        nbtMapLoc.clear();
    }

    @Override
    public boolean hasBiomes() {
        return biomes != null;
    }

    @Override
    public boolean setBiome(int x, int z, int biome) {
        setBiome(getIndex(x, 0, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, int biome) {
        if (biomes == null) {
            biomes = new byte[area];
        }
        biomes[index] = (byte) biome;
    }

    @Override
    public void streamBiomes(NBTStreamer.ByteReader task) {
        if (!hasBiomes()) return;
        int index = 0;
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < width; x++, index++) {
                task.run(index, biomes[index] & 0xFF);
            }
        }
    }

    @Override
    public BaseBiome getBiome(int index) {
        if (!hasBiomes()) {
            return EditSession.nullBiome;
        }
        return FaweCache.CACHE_BIOME[biomes[index] & 0xFF];
    }

    @Override
    public BaseBiome getBiome(int x, int z) {
        return getBiome(getIndex(x, 0, z));
    }

    private CompoundTag getTag(int index) {
        convertTilesToIndex();
        return nbtMapIndex.get(index);
    }

    public int getId(int index) {
        int i = index >> BLOCK_SHIFT;
        if (i == lastIdsI) {
            if (lastIds == null) {
                return 0;
            }
            return lastIds[index & BLOCK_MASK] & 0xFF;
        }
        saveIds();
        byte[] compressed = ids[lastIdsI = i];
        if (compressed == null) {
            lastIds = null;
            return 0;
        }
        lastIds = MainUtil.decompress(compressed, lastIds, BLOCK_SIZE, compressionLevel);
        return lastIds[index & BLOCK_MASK] & 0xFF;
    }

    int saves = 0;

    private void saveIds() {
        if (saveIds && lastIds != null) {
            ids[lastIdsI] = MainUtil.compress(lastIds, buffer, compressionLevel);
        }
        saveIds = false;
    }

    private void saveDatas() {
        if (saveDatas && lastDatas != null) {
            datas[lastDatasI] = MainUtil.compress(lastDatas, buffer, compressionLevel);
        }
        saveDatas = false;
    }

    private void saveAdd() {
        if (saveAdd && lastAdd != null) {
            add[lastAddI] = MainUtil.compress(lastAdd, buffer, compressionLevel);
        }
        saveAdd = false;
    }

    public int getData(int index) {
        int i = index >> BLOCK_SHIFT;
        if (i == lastDatasI) {
            if (lastDatas == null) {
                return 0;
            }
            return lastDatas[index & BLOCK_MASK];
        }
        saveDatas();
        byte[] compressed = datas[lastDatasI = i];
        if (compressed == null) {
            lastDatas = null;
            return 0;
        }
        lastDatas = MainUtil.decompress(compressed, lastDatas, BLOCK_SIZE, compressionLevel);
        return lastDatas[index & BLOCK_MASK];
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
            ids = new byte[1 + (volume >> BLOCK_SHIFT)][];
            datas = new byte[1 + (volume >> BLOCK_SHIFT)][];
            lastAddI = -1;
            lastIdsI = -1;
            lastDatasI = -1;
            saveIds = false;
            saveAdd = false;
            saveDatas = false;
        }
    }

    @Override
    public Vector getDimensions() {
        return new Vector(width, height, length);
    }

    public int getAdd(int index) {
        int i = index >> BLOCK_SHIFT;
        if (i == lastAddI) {
            if (lastAdd == null) {
                return 0;
            }
            return lastAdd[index & BLOCK_MASK] & 0xFF;
        }
        saveAdd();
        byte[] compressed = add[lastAddI = i];
        if (compressed == null) {
            lastAdd = null;
            return 0;
        }
        lastAdd = MainUtil.decompress(compressed, lastAdd, BLOCK_SIZE, compressionLevel);
        return lastAdd[index & BLOCK_MASK] & 0xFF;
    }


    private int lastI;
    private int lastIMin;
    private int lastIMax;

    public int getLocalIndex(int index) {
        if (index < lastIMin || index > lastIMax) {
            lastI = index >> BLOCK_SHIFT;
            lastIMin = lastI << BLOCK_SHIFT;
            lastIMax = lastIMin + BLOCK_MASK;
        }
        return lastI;
    }

    @Override
    public void setId(int index, int value) {
        int i = getLocalIndex(index);
        if (i != lastIdsI) {
            saveIds();
            byte[] compressed = ids[lastIdsI = i];
            if (compressed != null) {
                lastIds = MainUtil.decompress(compressed, lastIds, BLOCK_SIZE, compressionLevel);
            } else {
                lastIds = null;
            }
        }
        if (lastIds == null) {
            if (value == 0) {
                return;
            }
            lastIds = new byte[BLOCK_SIZE];
        }
        lastIds[index & BLOCK_MASK] = (byte) value;
        saveIds = true;
    }

    @Override
    public void setData(int index, int value) {
        int i = getLocalIndex(index);
        if (i != lastDatasI) {
            saveDatas();
            byte[] compressed = datas[lastDatasI = i];
            if (compressed != null) {
                lastDatas = MainUtil.decompress(compressed, lastDatas, BLOCK_SIZE, compressionLevel);
            } else {
                lastDatas = null;
            }
        }
        if (lastDatas == null) {
            if (value == 0) {
                return;
            }
            lastDatas = new byte[BLOCK_SIZE];
        }
        lastDatas[index & BLOCK_MASK] = (byte) value;
        saveDatas = true;
    }

    @Override
    public void setAdd(int index, int value) {
        if (value == 0) {
            return;
        }
        if (add == null) {
            add = new byte[1 + (volume >> BLOCK_SHIFT)][];
        }
        int i = index >> BLOCK_SHIFT;
        if (i != lastAddI) {
            saveAdd();
            byte[] compressed = add[lastAddI = i];
            if (compressed != null) {
                lastAdd = MainUtil.decompress(compressed, lastAdd, BLOCK_SIZE, compressionLevel);
            } else {
                lastAdd = null;
            }
        }
        if (lastAdd == null) {
            if (value == 0) {
                return;
            }
            lastAdd = new byte[BLOCK_SIZE];
        }
        lastAdd[index & BLOCK_MASK] = (byte) value;
        saveAdd = true;
    }

    @Override
    public void streamIds(NBTStreamer.ByteReader task) {
        int index = 0;
        if (add != null) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int id = getId(index) + (getAdd(index) << 8);
                        task.run(index++, id);
                    }
                }
            }
        } else {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int id = getId(index);
                        task.run(index++, id);
                    }
                }
            }
        }
    }

    @Override
    public void streamDatas(NBTStreamer.ByteReader task) {
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int data = getData(index);
                    task.run(index++, data);
                }
            }
        }
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        convertTilesToIndex();
        for (Map.Entry<Integer, CompoundTag> entry : nbtMapIndex.entrySet()) {
            int index = entry.getKey();
            CompoundTag tag = entry.getValue();
            Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
            if (!values.containsKey("x")) {
                int y = index / area;
                index -= y * area;
                int z = index / width;
                int x = index - (z * width);
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));
            }
        }
        return new ArrayList<>(nbtMapIndex.values());
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

    @Override
    public BaseBlock getBlock(int index) {
        int id = getId(index);
        if (add != null) {
            id += getAdd(index) << 8;
        }
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
    public void forEach(final BlockReader task, final boolean air) {
        if (air) {
            for (int y = 0, index = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, index++) {
                        BaseBlock block = getBlock(index);
                        task.run(x, y, z, block);
                    }
                }
            }
        } else {
            for (int y = 0, index = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, index++) {
                        BaseBlock block = getBlock(index);
                        if (block.getId() != 0) {
                            task.run(x, y, z, block);
                        }
                    }
                }
            }
        }
    }

    public int size() {
        saveIds();
        saveDatas();
        int total = 0;
        for (byte[] array : ids) {
            if (array != null) {
                total += array.length;
            }
        }
        for (byte[] array : datas) {
            if (array != null) {
                total += array.length;
            }
        }
        return total;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        nbtMapLoc.put(new IntegerTrio(x, y, z), tag);
        return true;
    }

    public boolean setTile(int index, CompoundTag tag) {
        nbtMapIndex.put(index, tag);
        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
        values.remove("x");
        values.remove("y");
        values.remove("z");
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        return setBlock(getIndex(x, y, z), block);
    }

    public boolean setBlock(int index, BaseBlock block) {
        int id = block.getId();
        setId(index, id);
        if (id >= 256) {
            setAdd(index, (id >> 8));
        }
        setData(index, block.getData());
        CompoundTag tile = block.getNbtData();
        if (tile != null) {
            setTile(index, tile);
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

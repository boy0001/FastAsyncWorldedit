package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


import static com.google.common.base.Preconditions.checkNotNull;

public class MCAChunk extends FaweChunk<Void> {

//    ids: byte[16][4096]
//    data: byte[16][2048]
//    skylight: byte[16][2048]
//    blocklight: byte[16][2048]
//    entities: Map<Short, CompoundTag>
//    tiles: List<CompoundTag>
//    biomes: byte[256]
//    compressedSize: int
//    modified: boolean
//    deleted: boolean

    public byte[][] ids;
    public byte[][] data;
    public byte[][] skyLight;
    public byte[][] blockLight;
    public byte[] biomes;
    public Map<Short, CompoundTag> tiles = new HashMap<>();
    public Map<UUID, CompoundTag> entities = new HashMap<>();
    private long inhabitedTime;
    private long lastUpdate;
    private int[] heightMap;

    public int compressedSize;
    private boolean modified;
    private boolean deleted;

    public byte[] toBytes(byte[] buffer) throws IOException {
        checkNotNull(buffer);
        if (buffer == null) {
            buffer = new byte[8192];
        }
        FastByteArrayOutputStream buffered = new FastByteArrayOutputStream(buffer);
        DataOutputStream dataOut = new DataOutputStream(buffered);
        NBTOutputStream nbtOut = new NBTOutputStream(dataOut);
        nbtOut.writeNamedTagName("", NBTConstants.TYPE_COMPOUND);
        nbtOut.writeLazyCompoundTag("Level", new NBTOutputStream.LazyWrite() {
            @Override
            public void write(NBTOutputStream out) throws IOException {
                out.writeNamedTag("V", (byte) 1);
                out.writeNamedTag("xPos", getX());
                out.writeNamedTag("zPos", getZ());
                out.writeNamedTag("LightPopulated", (byte) 0);
                out.writeNamedTag("TerrainPopulated", (byte) 1);
                if (entities.isEmpty()) {
                    out.writeNamedEmptyList("Entities");
                } else {
                    out.writeNamedTag("Entities", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>(entities.values())));
                }
                if (tiles.isEmpty()) {
                    out.writeNamedEmptyList("TileEntities");
                } else {
                    out.writeNamedTag("TileEntities", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>(tiles.values())));
                }
                out.writeNamedTag("InhabitedTime", inhabitedTime);
                out.writeNamedTag("LastUpdate", lastUpdate);
                if (biomes != null) {
                    out.writeNamedTag("Biomes", biomes);
                }
                out.writeNamedTag("HeightMap", heightMap);
                out.writeNamedTagName("Sections", NBTConstants.TYPE_LIST);
                dataOut.writeByte(NBTConstants.TYPE_COMPOUND);
                int len = 0;
                for (int layer = 0; layer < ids.length; layer++) {
                    if (ids[layer] != null) len++;
                }
                dataOut.writeInt(len);
                for (int layer = 0; layer < ids.length; layer++) {
                    byte[] idLayer = ids[layer];
                    if (idLayer == null) {
                        continue;
                    }
                    out.writeNamedTag("Y", (byte) layer);
                    out.writeNamedTag("BlockLight", blockLight[layer]);
                    out.writeNamedTag("SkyLight", skyLight[layer]);
                    out.writeNamedTag("Blocks", idLayer);
                    out.writeNamedTag("Data", data[layer]);
                    out.writeEndTag();
                }
            }
        });
        nbtOut.writeEndTag();
        nbtOut.close();
        return buffered.toByteArray();
    }

    public CompoundTag toTag() {
        if (deleted) {
            return null;
        }
        // TODO optimize this as it's slow
        // e.g. by precalculating the length
        HashMap<String, Object> level = new HashMap<String, Object>();
        level.put("Entities", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>(entities.values())));
        level.put("TileEntities", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>(tiles.values())));
        level.put("InhabitedTime", inhabitedTime);
        level.put("LastUpdate", lastUpdate);
        level.put("LightPopulated", (byte) 0);
        level.put("TerrainPopulated", (byte) 1);
        level.put("V", (byte) 1);
        level.put("xPos", getX());
        level.put("zPos", getZ());
        if (biomes != null) {
            level.put("Biomes", biomes);
        }
        level.put("HeightMap", heightMap);
        ArrayList<HashMap<String, Object>> sections = new ArrayList<>();
        for (int layer = 0; layer < ids.length; layer++) {
            byte[] idLayer = ids[layer];
            if (idLayer == null) {
                continue;
            }
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("Y", (byte) layer);
            map.put("BlockLight", blockLight[layer]);
            map.put("SkyLight", skyLight[layer]);
            map.put("Blocks", idLayer);
            map.put("Data", data[layer]);
            sections.add(map);
        }
        level.put("Sections", sections);
        HashMap<String, Object> root = new HashMap<>();
        root.put("Level", level);
        return FaweCache.asTag(root);
    }

    public MCAChunk(FaweQueue queue, int x, int z) {
        super(queue, x, z);
        this.ids = new byte[16][];
        this.data = new byte[16][];
        this.skyLight = new byte[16][];
        this.blockLight = new byte[16][];
        this.biomes = new byte[256];
        this.tiles = new HashMap<>();
        this.entities = new HashMap<>();
        this.lastUpdate = System.currentTimeMillis();
        this.heightMap = new int[256];
        this.modified = true;
    }

    public MCAChunk(MCAChunk parent, boolean shallow) {
        super(parent.getParent(), parent.getX(), parent.getZ());
        if (shallow) {
            this.ids = parent.ids;
            this.data = parent.data;
            this.skyLight = parent.skyLight;
            this.blockLight = parent.blockLight;
            this.biomes = parent.biomes;
            this.tiles = parent.tiles;
            this.entities = parent.entities;
            this.inhabitedTime = parent.inhabitedTime;
            this.lastUpdate = parent.lastUpdate;
            this.heightMap = parent.heightMap;
            this.compressedSize = parent.compressedSize;
            this.modified = parent.modified;
            this.deleted = parent.deleted;
        } else {
            this.ids = (byte[][]) MainUtil.copyNd(parent.ids);
            this.data = (byte[][]) MainUtil.copyNd(parent.data);
            this.skyLight = (byte[][]) MainUtil.copyNd(parent.skyLight);
            this.blockLight = (byte[][]) MainUtil.copyNd(parent.blockLight);
            this.biomes = parent.biomes.clone();
            this.tiles = new HashMap<>(parent.tiles);
            this.entities = new HashMap<>(parent.entities);
            this.inhabitedTime = parent.inhabitedTime;
            this.lastUpdate = parent.lastUpdate;
            this.heightMap = parent.heightMap.clone();
            this.compressedSize = parent.compressedSize;
            this.modified = parent.modified;
            this.deleted = parent.deleted;
        }
    }

    public MCAChunk(NBTInputStream nis, FaweQueue parent, int x, int z, int compressedSize) throws IOException {
        super(parent, x, z);
        ids = new byte[16][];
        data = new byte[16][];
        skyLight = new byte[16][];
        blockLight = new byte[16][];
        this.compressedSize = compressedSize;
        NBTStreamer streamer = new NBTStreamer(nis);
        streamer.addReader(".Level.InhabitedTime", new RunnableVal2<Integer, Long>() {
            @Override
            public void run(Integer index, Long value) {
                inhabitedTime = value;
            }
        });
        streamer.addReader(".Level.LastUpdate", new RunnableVal2<Integer, Long>() {
            @Override
            public void run(Integer index, Long value) {
                lastUpdate = value;
            }
        });
        streamer.addReader(".Level.Sections.#", new RunnableVal2<Integer, CompoundTag>() {
            @Override
            public void run(Integer index, CompoundTag tag) {
                int layer = tag.getByte("Y");
                ids[layer] = tag.getByteArray("Blocks");
                data[layer] = tag.getByteArray("Data");
                skyLight[layer] = tag.getByteArray("SkyLight");
                blockLight[layer] = tag.getByteArray("BlockLight");
            }
        });
        streamer.addReader(".Level.TileEntities.#", new RunnableVal2<Integer, CompoundTag>() {
            @Override
            public void run(Integer index, CompoundTag tile) {
                int x = tile.getInt("x") & 15;
                int y = tile.getInt("y");
                int z = tile.getInt("z") & 15;
                short pair = MathMan.tripleBlockCoord(x, y, z);
                tiles.put(pair, tile);
            }
        });
        streamer.addReader(".Level.Entities.#", new RunnableVal2<Integer, CompoundTag>() {
            @Override
            public void run(Integer index, CompoundTag entityTag) {
                if (entities == null) {
                    entities = new HashMap<UUID, CompoundTag>();
                }

                long least = entityTag.getLong("UUIDLeast");
                long most = entityTag.getLong("UUIDMost");
                entities.put(new UUID(most, least), entityTag);
            }
        });
        streamer.addReader(".Level.Biomes", new RunnableVal2<Integer, byte[]>() {
            @Override
            public void run(Integer index, byte[] value) {
                biomes = value;
            }
        });
        streamer.addReader(".Level.HeightMap", new RunnableVal2<Integer, int[]>() {
            @Override
            public void run(Integer index, int[] value) {
                heightMap = value;
            }
        });
        streamer.readFully();
    }

    public int[] getHeightMapArray() {
        return heightMap;
    }

    public void setDeleted(boolean deleted) {
        setModified();
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isModified() {
        return modified;
    }

    @Deprecated
    public final void setModified() {
        this.modified = true;
    }

    @Override
    public int getBitMask() {
        int bitMask = 0;
        for (int section = 0; section < ids.length; section++) {
            if (ids[section] != null) {
                bitMask += 1 << section;
            }
        }
        return bitMask;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {
        modified = true;
        short pair = MathMan.tripleBlockCoord(x, y, z);
        if (tile != null) {
            tiles.put(pair, tile);
        } else {
            tiles.remove(pair);
        }
    }

    @Override
    public void setEntity(CompoundTag entityTag) {
        modified = true;
        long least = entityTag.getLong("UUIDLeast");
        long most = entityTag.getLong("UUIDMost");
        entities.put(new UUID(most, least), entityTag);
    }

    @Override
    public void setBiome(int x, int z, byte biome) {
        modified = true;
        biomes[x + (z << 4)] = biome;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return new HashSet<>(entities.values());
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return tiles == null ? new HashMap<Short, CompoundTag>() : tiles;
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        if (tiles == null || tiles.isEmpty()) {
            return null;
        }
        short pair = MathMan.tripleBlockCoord(x, y, z);
        return tiles.get(pair);
    }

    public boolean doesSectionExist(int cy) {
        return ids[cy] != null;
    }

    @Override
    public FaweChunk<Void> copy(boolean shallow) {
        return new MCAChunk(this, shallow);
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        int layer = y >> 4;
        byte[] idLayer = ids[layer];
        if (idLayer == null) {
            return 0;
        }
        int j = FaweCache.CACHE_J[y][z & 15][x & 15];
        int id = idLayer[j] & 0xFF;
        if (FaweCache.hasData(id)) {
            byte[] dataLayer = data[layer];
            if (dataLayer != null) {
                return (id << 4) + getNibble(j, dataLayer);
            }
        }
        return id << 4;
    }

    @Override
    public byte[] getBiomeArray() {
        return this.biomes;
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return new HashSet<>();
    }

    public void setSkyLight(int x, int y, int z, int value) {
        modified = true;
        int layer = y >> 4;
        byte[] skyLayer = skyLight[layer];
        if (skyLayer == null) {
            return;
        }
        int index = FaweCache.CACHE_J[y][z & 15][x & 15];
        setNibble(index, skyLayer, value);
    }

    public void setBlockLight(int x, int y, int z, int value) {
        modified = true;
        int layer = y >> 4;
        byte[] blockLayer = blockLight[layer];
        if (blockLayer == null) {
            return;
        }
        int index = FaweCache.CACHE_J[y][z & 15][x & 15];
        setNibble(index, blockLayer, value);
    }

    public int getSkyLight(int x, int y, int z) {
        int layer = y >> 4;
        byte[] skyLayer = skyLight[layer];
        if (skyLayer == null) {
            return 0;
        }
        int index = FaweCache.CACHE_J[y][z & 15][x & 15];
        return getNibble(index, skyLayer);
    }

    public int getBlockLight(int x, int y, int z) {
        int layer = y >> 4;
        byte[] blockLayer = blockLight[layer];
        if (blockLayer == null) {
            return 0;
        }
        int index = FaweCache.CACHE_J[y][z & 15][x & 15];
        return getNibble(index, blockLayer);
    }

    public void setFullbright() {
        modified = true;
        for (byte[] array : skyLight) {
            if (array != null) {
                Arrays.fill(array, (byte) 255);
            }
        }
    }

    public void removeLight() {
        for (int i = 0; i < skyLight.length; i++) {
            byte[] array1 = skyLight[i];
            if (array1 == null) {
                continue;
            }
            byte[] array2 = blockLight[i];
            Arrays.fill(array1, (byte) 0);
            Arrays.fill(array2, (byte) 0);
        }
    }

    public int getNibble(int index, byte[] array) {
        int indexShift = index >> 1;
        if((index & 1) == 0) {
            return array[indexShift] & 15;
        } else {
            return array[indexShift] >> 4 & 15;
        }
    }

    public void setNibble(int index, byte[] array, int value) {
        int indexShift = index >> 1;
        byte existing = array[indexShift];
        int valueShift = value << 4;
        if (existing == value + valueShift) {
            return;
        }
        if((index & 1) == 0) {
            array[indexShift] = (byte)(existing & 240 | value);
        } else {
            array[indexShift] = (byte)(existing & 15 | valueShift);
        }
    }

    public void setIdUnsafe(byte[] idsLayer, int index, byte id) {
        idsLayer[index] = id;
    }

    public void setBlockUnsafe(byte[] idsLayer, byte[] dataLayer, int index, byte id, int data) {
        idsLayer[index] = id;
        setNibble(index, dataLayer, data);
    }

    @Override
    public void setBlock(int x, int y, int z, int id, int data) {
        modified = true;
        int layer = y >> 4;
        byte[] idsLayer = ids[layer];
        if (idsLayer == null) {
            idsLayer = this.ids[layer] = new byte[4096];
            this.data[layer] = new byte[2048];
            this.skyLight[layer] = new byte[2048];
            this.blockLight[layer] = new byte[2048];
        }
        int j = FaweCache.CACHE_J[y][z & 15][x & 15];
        idsLayer[j] = (byte) id;
        byte[] dataLayer = this.data[layer];
        setNibble(j, dataLayer, data);
    }

    @Override
    public void setBiome(byte biome) {
        Arrays.fill(biomes, biome);
    }

    @Override
    public void removeEntity(UUID uuid) {
        modified = true;
        entities.remove(uuid);
    }

    @Override
    public Void getChunk() {
        throw new UnsupportedOperationException("Not applicable for this");
    }

    @Override
    public FaweChunk call() {
        throw new UnsupportedOperationException("Not supported");
    }
}

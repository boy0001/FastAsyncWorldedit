package com.boydti.fawe.example;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;

import java.util.*;

public abstract class CharFaweChunk<T, V extends FaweQueue> extends FaweChunk<T> {

    public final char[][] ids;
    public final short[] count;
    public final short[] air;
    public final byte[] heightMap;

    public byte[] biomes;
    public HashMap<Short, CompoundTag> tiles;
    public HashSet<CompoundTag> entities;
    public HashSet<UUID> entityRemoves;

    public T chunk;

    public CharFaweChunk(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z);
        this.ids = ids;
        this.count = count;
        this.air = air;
        this.heightMap = heightMap;
    }

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public CharFaweChunk(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        this.ids = new char[HEIGHT >> 4][];
        this.count = new short[HEIGHT >> 4];
        this.air = new short[HEIGHT >> 4];
        this.heightMap = new byte[256];
    }

    @Override
    public V getParent() {
        return (V) super.getParent();
    }

    @Override
    public T getChunk() {
        if (this.chunk == null) {
            this.chunk = getNewChunk();
        }
        return this.chunk;
    }

    public abstract T getNewChunk();

    @Override
    public void setLoc(final FaweQueue parent, int x, int z) {
        super.setLoc(parent, x, z);
        this.chunk = null;
    }

    /**
     * Get the number of block changes in a specified section
     *
     * @param i
     * @return
     */
    public int getCount(final int i) {
        return this.count[i];
    }

    public int getAir(final int i) {
        return this.air[i];
    }

    public void setCount(final int i, final short value) {
        this.count[i] = value;
    }

    public int getTotalCount() {
        int total = 0;
        for (int i = 0; i < count.length; i++) {
            total += Math.min(4096, this.count[i]);
        }
        return total;
    }

    public int getTotalAir() {
        int total = 0;
        for (int i = 0; i < air.length; i++) {
            total += Math.min(4096, this.air[i]);
        }
        return total;
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

    /**
     * Get the raw data for a section
     *
     * @param i
     * @return
     */
    @Override
    public char[] getIdArray(final int i) {
        return this.ids[i];
    }

    @Override
    public char[][] getCombinedIdArrays() {
        return this.ids;
    }

    @Override
    public byte[] getBiomeArray() {
        return this.biomes;
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        short i = FaweCache.CACHE_I[y][z][x];
        char[] array = getIdArray(i);
        if (array == null) {
            return 0;
        }
        return array[FaweCache.CACHE_J[y][z][x]];
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {
        if (tiles == null) {
            tiles = new HashMap<>();
        }
        short pair = MathMan.tripleBlockCoord(x, y, z);
        tiles.put(pair, tile);
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        if (tiles == null) {
            return null;
        }
        short pair = MathMan.tripleBlockCoord(x, y, z);
        return tiles.get(pair);
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return tiles == null ? new HashMap<Short, CompoundTag>() : tiles;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return entities == null ? Collections.emptySet() : entities;
    }

    @Override
    public void setEntity(CompoundTag tag) {
        if (entities == null) {
            entities = new HashSet<>();
        }
        entities.add(tag);
    }

    @Override
    public void removeEntity(UUID uuid) {
        if (entityRemoves == null) {
            entityRemoves = new HashSet<>();
        }
        entityRemoves.add(uuid);
    }

    @Override
    public HashSet<UUID> getEntityRemoves() {
        return entityRemoves == null ? new HashSet<UUID>() : entityRemoves;
    }

    @Override
    public void setBlock(int x, int y, int z, int id) {
        final int i = FaweCache.CACHE_I[y][z][x];
        final int j = FaweCache.CACHE_J[y][z][x];
        char[] vs = this.ids[i];
        if (vs == null) {
            vs = this.ids[i] = new char[4096];
            this.count[i]++;
        } else {
            switch (vs[j]) {
                case 0:
                    this.count[i]++;
                    break;
                case 1:
                    this.air[i]--;
                    break;
            }
        }
        switch (id) {
            case 0:
                this.air[i]++;
                vs[j] = (char) 1;
                return;
            case 11:
            case 39:
            case 40:
            case 51:
            case 74:
            case 89:
            case 122:
            case 124:
            case 138:
            case 169:
            case 213:
            case 130:
            case 76:
            case 62:
            case 50:
            case 10:
            default:
                vs[j] = (char) (id << 4);
                heightMap[z << 4 | x] = (byte) y;
                return;
        }
    }

    @Override
    public void setBlock(final int x, final int y, final int z, final int id, int data) {
        final int i = FaweCache.CACHE_I[y][z][x];
        final int j = FaweCache.CACHE_J[y][z][x];
        char[] vs = this.ids[i];
        if (vs == null) {
            vs = this.ids[i] = new char[4096];
            this.count[i]++;
        } else {
            switch (vs[j]) {
                case 0:
                    this.count[i]++;
                    break;
                case 1:
                    this.air[i]--;
                    break;
            }
        }
        switch (id) {
            case 0:
                this.air[i]++;
                vs[j] = (char) 1;
                return;
            case 39:
            case 40:
            case 51:
            case 74:
            case 89:
            case 122:
            case 124:
            case 138:
            case 169:
            case 213:
            case 2:
            case 4:
            case 13:
            case 14:
            case 15:
            case 20:
            case 21:
            case 22:
            case 30:
            case 32:
            case 37:
            case 41:
            case 42:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 56:
            case 57:
            case 58:
            case 7:
            case 73:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 85:
            case 87:
            case 88:
            case 101:
            case 102:
            case 103:
            case 110:
            case 112:
            case 113:
            case 121:
            case 129:
            case 133:
            case 165:
            case 166:
            case 172:
            case 173:
            case 174:
            case 190:
            case 191:
            case 192:
                vs[j] = (char) (id << 4);
                heightMap[z << 4 | x] = (byte) y;
                return;
            case 130:
            case 76:
            case 62:
            case 50:
            case 10:
            case 54:
            case 146:
            case 137:
            case 188:
            case 189:
            case 61:
            case 65:
            case 68: // removed
            default:
                vs[j] = (char) ((id << 4) + data);
                heightMap[z << 4 | x] = (byte) y;
                return;
        }
    }

    @Deprecated
    public void setBitMask(int ignore) {
        // Remove
    }

    @Override
    public void setBiome(final int x, final int z, byte biome) {
        if (this.biomes == null) {
            this.biomes = new byte[256];
        }
        if (biome == 0) biome = -1;
        biomes[((z & 15) << 4) + (x & 15)] = biome;
    }

    @Override
    public abstract CharFaweChunk<T, V> copy(boolean shallow);
}

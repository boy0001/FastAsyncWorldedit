package com.boydti.fawe.example;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.BytePair;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class CharFaweChunk<T, V extends FaweQueue> extends FaweChunk<T> {

    public char[][] ids;
    public short[] count;
    public short[] air;
    public short[] relight;
    public int[][] biomes;
    private int bitMask = -1;

    public HashMap<BytePair, CompoundTag> tiles;

    public HashSet<CompoundTag> entities;

    public HashSet<UUID> entityRemoves;

    public T chunk;

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
        this.relight = new short[HEIGHT >> 4];
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

    /**
     * Get the number of block changes in a specified section
     * @param i
     * @return
     */
    public int getRelight(final int i) {
        return this.relight[i];
    }

    public int getTotalCount() {
        int total = 0;
        for (int i = 0; i < count.length; i++) {
            total += Math.min(4096, this.count[i]);
        }
        return total;
    }

    public int getTotalRelight() {
        if ((this.getTotalCount() == 0) && (this.biomes == null)) {
            Arrays.fill(this.count, (short) 1);
            Arrays.fill(this.relight, Short.MAX_VALUE);
            return Short.MAX_VALUE;
        }
        int total = 0;
        for (int i = 0; i < relight.length; i++) {
            total += this.relight[i];
        }
        return total;
    }

    @Override
    public int getBitMask() {
        if (bitMask == -1) {
            this.bitMask = 0;
            for (int section = 0; section < ids.length; section++) {
                if (ids[section] != null) {
                    bitMask += 1 << section;
                }
            }
        }
        return bitMask;
    }

    public void setBitMask(int value) {
        this.bitMask = value;
    }

    /**
     * Get the raw data for a section
     * @param i
     * @return
     */
    public char[] getIdArray(final int i) {
        return this.ids[i];
    }

    public char[][] getCombinedIdArrays() {
        return this.ids;
    }

    public int[][] getBiomeArray() {
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
        byte i = MathMan.pair16((byte) x, (byte) z);
        byte j = (byte) y;
        BytePair pair = new BytePair(i, j);
        tiles.put(pair, tile);
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        if (tiles == null) {
            return null;
        }
        byte i = MathMan.pair16((byte) x, (byte) z);
        byte j = (byte) y;
        BytePair pair = new BytePair(i, j);
        return tiles.get(pair);
    }

    @Override
    public Map<BytePair, CompoundTag> getTiles() {
        return tiles == null ? new HashMap<BytePair, CompoundTag>() : tiles;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return entities == null ? new HashSet<CompoundTag>() : entities;
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
        } else if (vs[j] == 0) {
            this.count[i]++;
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
                this.relight[i]++;
            default:
                vs[j] = (char) (id << 4);
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
        } else if (vs[j] == 0) {
            this.count[i]++;
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
                this.relight[i]++;
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
            case 60:
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
            case 170:
            case 172:
            case 173:
            case 174:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
                vs[j] = (char) (id << 4);
                return;
            case 130:
            case 76:
            case 62:
            case 50:
            case 10:
                this.relight[i]++;
            case 54:
            case 146:
            case 61:
            case 65:
            case 68: // removed
            default:
                vs[j] = (char) ((id << 4) + data);
                return;
        }
    }

    @Override
    public void setBiome(final int x, final int z, final BaseBiome biome) {
        if (this.biomes == null) {
            this.biomes = new int[16][];
        }
        int[] index = this.biomes[x];
        if (index == null) {
            index = this.biomes[x] = new int[16];
        }
        index[z] = biome.getId();
    }

    @Override
    public CharFaweChunk<T, V> copy(boolean shallow) {
        CharFaweChunk<T, V> copy = (CharFaweChunk<T, V>) getParent().getFaweChunk(getX(), getZ());
        if (shallow) {
            copy.ids = ids;
            copy.air = air;
            copy.biomes = biomes;
            copy.chunk = chunk;
            copy.count = count;
            copy.relight = relight;
        } else {
            copy.ids = (char[][]) MainUtil.copyNd(ids);
            copy.air = air.clone();
            copy.biomes = biomes.clone();
            copy.chunk = chunk;
            copy.count = count.clone();
            copy.relight = relight.clone();
        }
        return copy;
    }
}

package com.boydti.fawe.bukkit.v1_9;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

public class BukkitChunk_1_9 extends FaweChunk<Chunk> {

    private int[][] ids;

    private short[] count;
    private short[] air;
    private short[] relight;
    public int[][] biomes;

    public Chunk chunk;

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     */
    protected BukkitChunk_1_9(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        this.ids = new int[16][];
        this.count = new short[16];
        this.air = new short[16];
        this.relight = new short[16];
    }

    @Override
    public Chunk getChunk() {
        if (this.chunk == null) {
            this.chunk = Bukkit.getWorld(getParent().world).getChunkAt(getX(), getZ());
        }
        return this.chunk;
    }

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
        for (int i = 0; i < 16; i++) {
            total += this.count[i];
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
        for (int i = 0; i < 16; i++) {
            total += this.relight[i];
        }
        return total;
    }

    /**
     * Get the raw data for a section
     * @param i
     * @return
     */
    public int[] getIdArray(final int i) {
        return this.ids[i];
    }

    public int[][] getIdArrays() {
        return this.ids;
    }

    public int[][] getBiomeArray() {
        return this.biomes;
    }

    @Override
    public void setBlock(final int x, final int y, final int z, final int id, byte data) {
        final int i = FaweCache.CACHE_I[y][x][z];
        final int j = FaweCache.CACHE_J[y][x][z];
        int[] vs = this.ids[i];
        if (vs == null) {
            vs = this.ids[i] = new int[4096];
            this.count[i]++;
        } else if (vs[j] == 0) {
            this.count[i]++;
        }
        switch (id) {
            case 0:
                this.air[i]++;
                vs[j] = -1;
                return;
            case 10:
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
            case 8:
            case 9:
            case 73:
            case 78:
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
            case 181:
            case 182:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
                vs[j] = (id);
                return;
            case 130:
            case 76:
            case 62:
                this.relight[i]++;
            case 54:
            case 146:
            case 61:
            case 65:
            case 68:
                if (data < 2) {
                    data = 2;
                }
            default:
                vs[j] = id + (data << 12);
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
    public FaweChunk<Chunk> copy(boolean shallow) {
        BukkitChunk_1_9 copy = new BukkitChunk_1_9(getParent(), getX(), getZ());
        if (shallow) {
            copy.ids = ids;
            copy.air = air;
            copy.biomes = biomes;
            copy.chunk = chunk;
            copy.count = count;
            copy.relight = relight;
        } else {
            copy.ids = (int[][]) MainUtil.copyNd(ids);
            copy.air = air.clone();
            copy.biomes = biomes.clone();
            copy.chunk = chunk;
            copy.count = count.clone();
            copy.relight = relight.clone();
        }
        return copy;
    }
}

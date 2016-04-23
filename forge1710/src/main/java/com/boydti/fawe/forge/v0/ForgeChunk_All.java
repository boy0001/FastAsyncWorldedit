package com.boydti.fawe.forge.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.FaweQueue;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.Arrays;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;

public class ForgeChunk_All extends FaweChunk<Chunk> {

    public byte[][] ids;
    public NibbleArray[] datas;

    public short[] count;
    public short[] air;
    public short[] relight;
    public byte[][] biomes;
    public Chunk chunk;

    public ForgeChunk_All(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        this.ids = new byte[16][];
        this.datas = new NibbleArray[16];
        this.count = new short[16];
        this.air = new short[16];
        this.relight = new short[16];
    }


    @Override
    public Chunk getChunk() {
        if (this.chunk == null) {
            World world = ((ForgeQueue_All) getParent()).getWorld();
            this.chunk = world.getChunkProvider().provideChunk(getX(), getZ());
        }
        return this.chunk;
    }

    @Override
    public void setLoc(final FaweQueue parent, int x, int z) {
        super.setLoc(parent, x, z);
        this.chunk = null;
    }

    /**
     * Get the number of block changes in a specified section.
     * @param i
     * @return
     */
    public int getCount(int i) {
        return this.count[i];
    }

    public int getAir(int i) {
        return this.air[i];
    }

    public void setCount(int i, short value) {
        this.count[i] = value;
    }

    /**
     * Get the number of block changes in a specified section.
     * @param i
     * @return
     */
    public int getRelight(int i) {
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
        if (getTotalCount() == 0) {
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
     * Get the raw data for a section.
     * @param i
     * @return
     */
    public byte[] getIdArray(int i) {
        return this.ids[i];
    }

    public NibbleArray getDataArray(int i) {
        return datas[i];
    }

    @Override
    public void setBlock(int x, int y, int z, int id, byte data) {
        int i = FaweCache.CACHE_I[y][x][z];
        int j = FaweCache.CACHE_J[y][x][z];
        byte[] vs = this.ids[i];
        if (vs == null) {
            vs = this.ids[i] = new byte[4096];
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
            case 55:
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
                vs[j] = (byte) (id);
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
            case 50:
                if (data < 2) {
                    data = 2;
                }
            default:
                vs[j] = (byte) id;
                NibbleArray dataArray = datas[i];
                if (dataArray == null) {
                    datas[i] = dataArray = new NibbleArray(4096, 4);
                }
                dataArray.set(x, y & 15, z, data);
                return;
        }
    }

    @Override
    public void setBiome(int x, int z, BaseBiome biome) {
        if (this.biomes == null) {
            this.biomes = new byte[16][];
        }
        byte[] index = this.biomes[x];
        if (index == null) {
            index = this.biomes[x] = new byte[16];
        }
        index[z] = (byte) biome.getId();
    }

    @Override
    public FaweChunk clone() {
        ForgeChunk_All toReturn = new ForgeChunk_All(getParent(), getX(), getZ());
        toReturn.air = this.air.clone();
        toReturn.count = this.count.clone();
        toReturn.relight = this.relight.clone();
        toReturn.ids = new byte[this.ids.length][];
        for (int i = 0; i < this.ids.length; i++) {
            byte[] matrix = this.ids[i];
            if (matrix != null) {
                toReturn.ids[i] = new byte[matrix.length];
                System.arraycopy(matrix, 0, toReturn.ids[i], 0, matrix.length);
            }
        }
        toReturn.datas = new NibbleArray[16];
        for (int i = 0; i < this.datas.length; i++) {
            if (datas[i] != null) {
                toReturn.datas[i] = datas[i];
            }
        }
        return toReturn;
    }
}

package com.boydti.fawe.forge.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;

public class ForgeChunk_All extends CharFaweChunk<Chunk> {

    public byte[][] byteIds;
    public NibbleArray[] datas;

    public ForgeChunk_All(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        this.byteIds = new byte[16][];
        this.datas = new NibbleArray[16];
    }

    @Override
    public Chunk getNewChunk() {
        World world = ((ForgeQueue_All) getParent()).getWorld();
        return world.getChunkProvider().provideChunk(getX(), getZ());
    }

    public byte[] getByteIdArray(int i) {
        return this.byteIds[i];
    }

    public NibbleArray getDataArray(int i) {
        return datas[i];
    }

    @Override
    public void setBlock(int x, int y, int z, int id, byte data) {
        int i = FaweCache.CACHE_I[y][x][z];
        int j = FaweCache.CACHE_J[y][x][z];
        byte[] vs = this.byteIds[i];
        char[] vs2 = this.ids[i];
        if (vs2 == null) {
            vs2 = this.ids[i] = new char[4096];
        }
        if (vs == null) {
            vs = this.byteIds[i] = new byte[4096];
        }
        this.count[i]++;
        switch (id) {
            case 0:
                this.air[i]++;
                vs[j] = -1;
                vs2[j] = (char) 1;
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
            case 55:
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
                vs[j] = (byte) (id);
                vs2[j] = (char) (id << 4);
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
                vs2[j] = (char) ((id << 4) + data);
                vs[j] = (byte) id;
                if (data != 0) {
                    NibbleArray dataArray = datas[i];
                    if (dataArray == null) {
                        datas[i] = dataArray = new NibbleArray(4096, 4);
                    }
                    dataArray.set(x, y & 15, z, data);
                }
                return;
        }
    }

    @Override
    public CharFaweChunk<Chunk> copy(boolean shallow) {
        ForgeChunk_All copy = new ForgeChunk_All(getParent(), getX(), getZ());
        if (shallow) {
            copy.byteIds = byteIds;
            copy.datas = datas;
            copy.air = air;
            copy.biomes = biomes;
            copy.chunk = chunk;
            copy.count = count;
            copy.relight = relight;
        } else {
            copy.byteIds = (byte[][]) MainUtil.copyNd(byteIds);
            copy.datas = datas.clone();
            copy.air = air.clone();
            copy.biomes = biomes.clone();
            copy.chunk = chunk;
            copy.count = count.clone();
            copy.relight = relight.clone();
        }
        return copy;
    }
}

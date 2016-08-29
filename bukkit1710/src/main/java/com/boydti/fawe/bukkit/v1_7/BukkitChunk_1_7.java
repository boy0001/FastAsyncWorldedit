package com.boydti.fawe.bukkit.v1_7;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import net.minecraft.server.v1_7_R4.NibbleArray;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

public class BukkitChunk_1_7 extends CharFaweChunk<Chunk> {

    @Override
    public Chunk getNewChunk() {
        return Bukkit.getWorld(getParent().getWorldName()).getChunkAt(getX(), getZ());
    }

    public byte[][] byteIds;
    public NibbleArray[] datas;

    public BukkitChunk_1_7(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        this.byteIds = new byte[16][];
        this.datas = new NibbleArray[16];
    }

    public byte[] getByteIdArray(int i) {
        return this.byteIds[i];
    }

    public NibbleArray getDataArray(int i) {
        return datas[i];
    }

    @Override
    public void setBlock(int x, int y, int z, int id) {
        this.setBlock(x, y, z, id, 0);
    }

    @Override
    public void setBlock(int x, int y, int z, int id, int data) {
        int i = FaweCache.CACHE_I[y][z][x];
        int j = FaweCache.CACHE_J[y][z][x];
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
                vs[j] = 0;
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
            case 130:
            case 76:
            case 62:
            case 50:
            case 10:
                this.relight[i]++;
            default:
                vs2[j] = (char) ((id << 4) + data);
                vs[j] = (byte) id;
                if (data != 0) {
                    NibbleArray dataArray = datas[i];
                    if (dataArray == null) {
                        datas[i] = dataArray = new NibbleArray(new byte[2048], 4);
                    }
                    dataArray.a(x, y & 15, z, data);
                }
                return;
        }
    }

    @Override
    public CharFaweChunk<Chunk> copy(boolean shallow) {
        BukkitChunk_1_7 copy = new BukkitChunk_1_7(getParent(), getX(), getZ());
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

package com.boydti.fawe.bukkit.v1_11;

import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.Chunk;
import net.minecraft.server.v1_11_R1.ChunkSection;
import net.minecraft.server.v1_11_R1.Entity;
import net.minecraft.server.v1_11_R1.ExceptionWorldConflict;
import net.minecraft.server.v1_11_R1.IAsyncChunkSaver;
import net.minecraft.server.v1_11_R1.IChunkLoader;
import net.minecraft.server.v1_11_R1.MinecraftKey;
import net.minecraft.server.v1_11_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_11_R1.NBTReadLimiter;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.NBTTagList;
import net.minecraft.server.v1_11_R1.NextTickListEntry;
import net.minecraft.server.v1_11_R1.NibbleArray;
import net.minecraft.server.v1_11_R1.TileEntity;
import net.minecraft.server.v1_11_R1.World;

public class FaweChunkLoader implements IChunkLoader, IAsyncChunkSaver {

    private final File folder;
    private Long2ObjectMap<Long> hashes = new Long2ObjectOpenHashMap<>();

    public FaweChunkLoader(File folder) {
        this.folder = folder;
        System.out.println(folder);
    }

    // writeNextIO (save)
    @Override
    public boolean c() {
        return false;
    }

    // loadChunk
    @Nullable
    @Override
    public Chunk a(World world, int x, int z) throws IOException {
        long pair = MathMan.pairInt(x, z);
        Long hash = hashes.get(pair);
        if (hash == null) {
            return null;
        }
        File file = new File(folder, hash.toString());
        int length = (int) file.length();
        try (FaweInputStream in = MainUtil.getCompressedIS(new FileInputStream(file), Math.min(length, 8192))) {
            NBTTagCompound nbttagcompound = NBTCompressedStreamTools.a(in, NBTReadLimiter.a);
            return readChunkFromNBT(world, nbttagcompound);
        }
    }

    private Chunk readChunkFromNBT(World world, NBTTagCompound nbttagcompound) {
        int i = nbttagcompound.getInt("xPos");
        int j = nbttagcompound.getInt("zPos");
        Chunk chunk = new Chunk(world, i, j);
        chunk.a(nbttagcompound.getIntArray("HeightMap"));
        chunk.d(nbttagcompound.getBoolean("TerrainPopulated"));
        chunk.e(nbttagcompound.getBoolean("LightPopulated"));
        chunk.c(nbttagcompound.getLong("InhabitedTime"));
        NBTTagList nbttaglist = nbttagcompound.getList("Sections", 10);
        ChunkSection[] achunksection = new ChunkSection[16];
        boolean flag1 = world.worldProvider.m();

        for(int k = 0; k < nbttaglist.size(); ++k) {
            NBTTagCompound nbttagcompound1 = nbttaglist.get(k);
            byte b0 = nbttagcompound1.getByte("Y");
            ChunkSection chunksection = new ChunkSection(b0 << 4, flag1);
            byte[] abyte = nbttagcompound1.getByteArray("Blocks");
            NibbleArray nibblearray = new NibbleArray(nbttagcompound1.getByteArray("Data"));
            NibbleArray nibblearray1 = nbttagcompound1.hasKeyOfType("Add", 7) ? new NibbleArray(nbttagcompound1.getByteArray("Add")):null;
            chunksection.getBlocks().a(abyte, nibblearray, nibblearray1);
            chunksection.a(new NibbleArray(nbttagcompound1.getByteArray("BlockLight")));
            if(flag1) {
                chunksection.b(new NibbleArray(nbttagcompound1.getByteArray("SkyLight")));
            }

            chunksection.recalcBlockCounts();
            achunksection[b0] = chunksection;
        }

        chunk.a(achunksection);
        if(nbttagcompound.hasKeyOfType("Biomes", 7)) {
            chunk.a(nbttagcompound.getByteArray("Biomes"));
        }

        return chunk;
    }


    // saveChunk
    @Override
    public void a(World world, Chunk chunk) throws IOException, ExceptionWorldConflict {
        try {
            NBTTagCompound exception = new NBTTagCompound();
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            exception.set("Level", nbttagcompound1);
            exception.setInt("DataVersion", 819);
            this.writeChunkToNBT(chunk, world, nbttagcompound1);
//            this.a(chunk.k(), exception);
        } catch (Exception var5) {
        }
    }

    private void writeChunkToNBT(Chunk chunk, World world, NBTTagCompound nbttagcompound) {
        nbttagcompound.setInt("xPos", chunk.locX);
        nbttagcompound.setInt("zPos", chunk.locZ);
        nbttagcompound.setLong("LastUpdate", world.getTime());
        nbttagcompound.setIntArray("HeightMap", chunk.r());
        nbttagcompound.setBoolean("TerrainPopulated", chunk.isDone());
        nbttagcompound.setBoolean("LightPopulated", chunk.v());
        nbttagcompound.setLong("InhabitedTime", chunk.x());
        ChunkSection[] achunksection = chunk.getSections();
        NBTTagList nbttaglist = new NBTTagList();
        boolean flag = world.worldProvider.m();
        ChunkSection[] achunksection1 = achunksection;
        int i = achunksection.length;

        NBTTagCompound nbttagcompound1;
        for(int nbttaglist1 = 0; nbttaglist1 < i; ++nbttaglist1) {
            ChunkSection iterator = achunksection1[nbttaglist1];
            if(iterator != Chunk.a) {
                nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Y", (byte)(iterator.getYPosition() >> 4 & 255));
                byte[] nbttaglist2 = new byte[4096];
                NibbleArray list = new NibbleArray();
                NibbleArray nibblearray1 = iterator.getBlocks().exportData(nbttaglist2, list);
                nbttagcompound1.setByteArray("Blocks", nbttaglist2);
                nbttagcompound1.setByteArray("Data", list.asBytes());
                if(nibblearray1 != null) {
                    nbttagcompound1.setByteArray("Add", nibblearray1.asBytes());
                }

                nbttagcompound1.setByteArray("BlockLight", iterator.getEmittedLightArray().asBytes());
                if(flag) {
                    nbttagcompound1.setByteArray("SkyLight", iterator.getSkyLightArray().asBytes());
                } else {
                    nbttagcompound1.setByteArray("SkyLight", new byte[iterator.getEmittedLightArray().asBytes().length]);
                }

                nbttaglist.add(nbttagcompound1);
            }
        }

        nbttagcompound.set("Sections", nbttaglist);
        nbttagcompound.setByteArray("Biomes", chunk.getBiomeIndex());
        chunk.g(false);
        NBTTagList var22 = new NBTTagList();

        Iterator var23;
        for(i = 0; i < chunk.getEntitySlices().length; ++i) {
            var23 = chunk.getEntitySlices()[i].iterator();

            while(var23.hasNext()) {
                Entity var24 = (Entity)var23.next();
                nbttagcompound1 = new NBTTagCompound();
                if(var24.d(nbttagcompound1)) {
                    chunk.g(true);
                    var22.add(nbttagcompound1);
                }
            }
        }

        nbttagcompound.set("Entities", var22);
        NBTTagList var25 = new NBTTagList();
        var23 = chunk.getTileEntities().values().iterator();

        while(var23.hasNext()) {
            TileEntity var26 = (TileEntity)var23.next();
            nbttagcompound1 = var26.save(new NBTTagCompound());
            var25.add(nbttagcompound1);
        }

        nbttagcompound.set("TileEntities", var25);
        List var27 = world.a(chunk, false);
        if(var27 != null) {
            long k = world.getTime();
            NBTTagList nbttaglist3 = new NBTTagList();
            Iterator iterator1 = var27.iterator();

            while(iterator1.hasNext()) {
                NextTickListEntry nextticklistentry = (NextTickListEntry)iterator1.next();
                NBTTagCompound nbttagcompound2 = new NBTTagCompound();
                MinecraftKey minecraftkey = (MinecraftKey) Block.REGISTRY.b(nextticklistentry.a());
                nbttagcompound2.setString("i", minecraftkey == null?"":minecraftkey.toString());
                nbttagcompound2.setInt("x", nextticklistentry.a.getX());
                nbttagcompound2.setInt("y", nextticklistentry.a.getY());
                nbttagcompound2.setInt("z", nextticklistentry.a.getZ());
                nbttagcompound2.setInt("t", (int)(nextticklistentry.b - k));
                nbttagcompound2.setInt("p", nextticklistentry.c);
                nbttaglist3.add(nbttagcompound2);
            }

            nbttagcompound.set("TileTicks", nbttaglist3);
        }

    }

    // saveExtraChunkData
    @Override
    public void b(World world, Chunk chunk) throws IOException {

    }

    // chunkTick
    @Override
    public void a() {

    }

    // saveExtraData
    @Override
    public void b() {
//        try {
//            this.savingExtraData = true;
//
//            while(true) {
//                if(this.writeNextIO()) {
//                    continue;
//                }
//            }
//        } finally {
//            this.savingExtraData = false;
//        }
    }

    // isChunkGeneratedAt
    @Override
    public boolean a(int x, int z) {
        return hashes.containsKey(MathMan.pairInt(x, z));
    }
}

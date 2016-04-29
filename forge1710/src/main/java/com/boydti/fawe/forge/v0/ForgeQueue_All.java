package com.boydti.fawe.forge.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.forge.ForgePlayer;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.RunnableVal;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;

public class ForgeQueue_All extends NMSMappedFaweQueue<World, Chunk, Chunk, ExtendedBlockStorage> {

    public ForgeQueue_All(final String world) {
        super(world);
    }

    @Override
    public boolean loadChunk(World world, int x, int z, boolean generate) {
        return getCachedChunk(world, x, z) != null;
    }

    @Override
    public Chunk getCachedChunk(World world, int cx, int cz) {
        Chunk chunk = world.getChunkProvider().provideChunk(cx, cz);
        if (chunk != null && !chunk.isChunkLoaded) {
            chunk.onChunkLoad();
        }
        return chunk;
    }

    @Override
    public int getCombinedId4Data(ExtendedBlockStorage ls, int x, int y, int z) {
        byte[] ids = ls.getBlockLSBArray();
        NibbleArray datasNibble = ls.getBlockMSBArray();
        int i = FaweCache.CACHE_J[y & 15][x & 15][z & 15];
        int combined = (ids[i] << 4) + (datasNibble == null ? 0 : datasNibble.get(x & 15, y & 15, z & 15));
        return combined;
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.getChunkProvider().chunkExists(x, z);
    }

    @Override
    public World getWorld(String world) {
        WorldServer[] worlds = MinecraftServer.getServer().worldServers;
        for (WorldServer ws : worlds) {
            if (ws.provider.getDimensionName().equals(world)) {
                return ws;
            }
        }
        return null;
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z) {
        try {
            IChunkProvider provider = world.getChunkProvider();
            if (!(provider instanceof ChunkProviderServer)) {
                return false;
            }
            ChunkProviderServer chunkServer = (ChunkProviderServer) provider;

            Chunk mcChunk;
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                mcChunk.onChunkUnload();
            }
            Field u;
            try {
                u = ChunkProviderServer.class.getDeclaredField("field_73248_b"); // chunksToUnload
            } catch(NoSuchFieldException e) {
                u = ChunkProviderServer.class.getDeclaredField("chunksToUnload");
            }
            u.setAccessible(true);
            Set unloadQueue = (Set) u.get(chunkServer);
            Field m;
            try {
                m = ChunkProviderServer.class.getDeclaredField("field_73244_f"); // loadedChunkHashMap
            } catch(NoSuchFieldException e) {
                m = ChunkProviderServer.class.getDeclaredField("loadedChunkHashMap");
            }
            m.setAccessible(true);
            LongHashMap loadedMap = (LongHashMap) m.get(chunkServer);
            Field lc;
            try {
                lc = ChunkProviderServer.class.getDeclaredField("field_73245_g"); // loadedChunkHashMap
            } catch(NoSuchFieldException e) {
                lc = ChunkProviderServer.class.getDeclaredField("loadedChunks");
            }
            lc.setAccessible(true);
            @SuppressWarnings("unchecked") List loaded = (List) lc.get(chunkServer);
            Field p;
            try {
                p = ChunkProviderServer.class.getDeclaredField("field_73246_d"); // currentChunkProvider
            } catch(NoSuchFieldException e) {
                p = ChunkProviderServer.class.getDeclaredField("currentChunkProvider");
            }
            p.setAccessible(true);
            IChunkProvider chunkProvider = (IChunkProvider) p.get(chunkServer);
            long pos = ChunkCoordIntPair.chunkXZ2Int(x, z);
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                mcChunk.onChunkUnload();
            }
            unloadQueue.remove(pos);
            loadedMap.remove(pos);
            mcChunk = chunkProvider.provideChunk(x, z);
            loadedMap.add(pos, mcChunk);
            loaded.add(mcChunk);
            if (mcChunk != null) {
                mcChunk.onChunkLoad();
                mcChunk.populateChunk(chunkProvider, chunkProvider, x, z);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
        return true;
    }

    private final RunnableVal<IntegerPair> loadChunk = new RunnableVal<IntegerPair>() {
        @Override
        public void run(IntegerPair loc) {
            Chunk chunk = getWorld().getChunkProvider().provideChunk(loc.x, loc.z);
            if (chunk != null && !chunk.isChunkLoaded) {
                chunk.onChunkLoad();
            }

        }
    };

    @Override
    public void refreshChunk(World world, Chunk chunk) {
        if (!chunk.isChunkLoaded) {
            return;
        }
        ChunkCoordIntPair pos = chunk.getChunkCoordIntPair();
        int cx = pos.chunkXPos;
        int cz = pos.chunkZPos;
        for (FawePlayer fp : Fawe.get().getCachedPlayers()) {
            ForgePlayer forgePlayer = (ForgePlayer) fp;
            EntityPlayerMP player = forgePlayer.parent;
            if (!player.worldObj.equals(world)) {
                continue;
            }
            int view = MinecraftServer.getServer().getConfigurationManager().getViewDistance();
            EntityPlayerMP nmsPlayer = (EntityPlayerMP) player;
            ChunkCoordinates loc = player.getPlayerCoordinates();
            int px = loc.posX >> 4;
            int pz = loc.posZ >> 4;
            int dx = Math.abs(cx - (loc.posX >> 4));
            int dz = Math.abs(cz - (loc.posZ >> 4));
            if ((dx > view) || (dz > view)) {
                continue;
            }
            NetHandlerPlayServer con = nmsPlayer.playerNetServerHandler;
            con.sendPacket(new S21PacketChunkData(chunk, false, 65535));
            // Try sending true, 0 first
            // Try bulk chunk packet
        }
    }

    public boolean setComponents(FaweChunk fc) {
        ForgeChunk_All fs = (ForgeChunk_All) fc;
        Chunk forgeChunk = fs.getChunk();
        net.minecraft.world.World nmsWorld = forgeChunk.worldObj;
        try {
            boolean flag = !nmsWorld.provider.hasNoSky;
            // Sections
            ExtendedBlockStorage[] sections = forgeChunk.getBlockStorageArray();
            Map<ChunkPosition, TileEntity> tiles = forgeChunk.chunkTileEntityMap;
            List[] entities = forgeChunk.entityLists;
            // Trim tiles
            Set<Map.Entry<ChunkPosition, TileEntity>> entryset = tiles.entrySet();
            Iterator<Map.Entry<ChunkPosition, TileEntity>> iterator = entryset.iterator();
            while (iterator.hasNext()) {
                Map.Entry<ChunkPosition, TileEntity> tile = iterator.next();
                ChunkPosition pos = tile.getKey();
                int lx = pos.chunkPosX & 15;
                int ly = pos.chunkPosY;
                int lz = pos.chunkPosZ & 15;
                int j = FaweCache.CACHE_I[ly][lx][lz];
                int k = FaweCache.CACHE_J[ly][lx][lz];
                byte[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                if (array[k] != 0) {
                    iterator.remove();
                }
            }
            // Efficiently merge sections
            for (int j = 0; j < sections.length; j++) {
                if (fs.getCount(j) == 0) {
                    continue;
                }
                byte[] newIdArray = fs.getIdArray(j);
                if (newIdArray == null) {
                    continue;
                }
                NibbleArray newDataArray = fs.getDataArray(j);
                ExtendedBlockStorage section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    sections[j] = section = new ExtendedBlockStorage(j << 4, !getWorld().provider.hasNoSky);
                    section.setBlockLSBArray(newIdArray);
                    section.setBlockMetadataArray(newDataArray);
                    continue;
                }
                // id + data << 8

                byte[] currentIdArray = section.getBlockLSBArray();
                NibbleArray currentDataArray = section.getMetadataArray();
                boolean data = currentDataArray != null;
                if (!data) {
                    section.setBlockMetadataArray(newDataArray);
                }
                boolean fill = true;
                for (int k = 0; k < newIdArray.length; k++) {
                    byte n = newIdArray[k];
                    switch (n) {
                        case 0:
                            fill = false;
                            continue;
                        case -1:
                            fill = false;
                            currentIdArray[k] = 0;
                            continue;
                        default:
                            currentIdArray[k] = n;
                            if (data) {
                                int x = FaweCache.CACHE_X[j][k];
                                int y = FaweCache.CACHE_Y[j][k];
                                int z = FaweCache.CACHE_Z[j][k];
                                int newData = newDataArray == null ? 0 : newDataArray.get(x, y, z);
                                int currentData = currentDataArray == null ? 0 : currentDataArray.get(x, y, z);
                                if (newData != currentData) {
                                    currentDataArray.set(x, y, z, newData);
                                }
                            }
                            continue;
                    }
                }
                if (fill) {
                    fs.setCount(j, Short.MAX_VALUE);
                }
            }
//            // Clear
        } catch (Throwable e) {
            e.printStackTrace();
        }
        byte[][] biomes = fs.biomes;
        if (biomes != null) {
            for (int x = 0; x < 16; x++) {
                byte[] array = biomes[x];
                if (array == null) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    byte biome = array[z];
                    if (biome == 0) {
                        continue;
                    }
                    forgeChunk.getBiomeArray()[((z & 0xF) << 4 | x & 0xF)] = biome;
                }
            }
        }
        sendChunk(fs);
        return true;
    }

    @Override
    public FaweChunk getChunk(int x, int z) {
        return new ForgeChunk_All(this, x, z);
    }

    @Override
    public boolean fixLighting(FaweChunk chunk, boolean fixAll) {
        try {
            ForgeChunk_All fc = (ForgeChunk_All) chunk;
            Chunk forgeChunk = fc.getChunk();
            if (!forgeChunk.isChunkLoaded) {
                forgeChunk.onChunkLoad();
            }
            forgeChunk.generateSkylightMap();
            if (fc.getTotalRelight() == 0 && !fixAll) {
                return true;
            }
            ExtendedBlockStorage[] sections = forgeChunk.getBlockStorageArray();
            net.minecraft.world.World nmsWorld = forgeChunk.worldObj;

            int X = fc.getX() << 4;
            int Z = fc.getZ() << 4;


            for (int j = 0; j < sections.length; j++) {
                ExtendedBlockStorage section = sections[j];
                if (section == null) {
                    continue;
                }
                if ((fc.getRelight(j) == 0 && !fixAll) || fc.getCount(j) == 0 || (fc.getCount(j) >= 4096 && fc.getAir(j) == 0)) {
                    continue;
                }
                byte[] array = section.getBlockLSBArray();
                int l = PseudoRandom.random.random(2);
                for (int k = 0; k < array.length; k++) {
                    int i = array[k];
                    if (i < 16) {
                        continue;
                    }
                    short id = (short) (i);
                    switch (id) { // Lighting
                        default:
                            if (!fixAll) {
                                continue;
                            }
                            if ((k & 1) == l) {
                                l = 1 - l;
                                continue;
                            }
                        case 10:
                        case 11:
                        case 39:
                        case 40:
                        case 50:
                        case 51:
                        case 62:
                        case 74:
                        case 76:
                        case 89:
                        case 122:
                        case 124:
                        case 130:
                        case 138:
                        case 169:
                            int x = FaweCache.CACHE_X[j][k];
                            int y = FaweCache.CACHE_Y[j][k];
                            int z = FaweCache.CACHE_Z[j][k];
                            if (isSurrounded(sections, x, y, z)) {
                                continue;
                            }
                            nmsWorld.func_147451_t(X + x, y, Z + z);
                    }
                }
            }
            return true;
        } catch (Throwable e) {
            if (Thread.currentThread() == Fawe.get().getMainThread()) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isSurrounded(ExtendedBlockStorage[] sections, int x, int y, int z) {
        return isSolid(getId(sections, x, y + 1, z))
                && isSolid(getId(sections, x + 1, y - 1, z))
                && isSolid(getId(sections, x - 1, y, z))
                && isSolid(getId(sections, x, y, z + 1))
                && isSolid(getId(sections, x, y, z - 1));
    }

    public boolean isSolid(int i) {
        return i != 0 && Block.getBlockById(i).isOpaqueCube();
    }

    public int getId(ExtendedBlockStorage[] sections, int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            return 1;
        }
        if (y < 0 || y > 255) {
            return 1;
        }
        int i = FaweCache.CACHE_I[y][x][z];
        ExtendedBlockStorage section = sections[i];
        if (section == null) {
            return 0;
        }
        byte[] array = section.getBlockLSBArray();
        int j = FaweCache.CACHE_J[y][x][z];
        return array[j];
    }
}

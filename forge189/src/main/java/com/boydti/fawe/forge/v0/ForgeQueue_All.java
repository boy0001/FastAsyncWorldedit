package com.boydti.fawe.forge.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.forge.ForgePlayer;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.PseudoRandom;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;

public class ForgeQueue_All extends NMSMappedFaweQueue<World, Chunk, Chunk, char[]> {

    public ForgeQueue_All(final String world) {
        super(world);
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return getWorld().getChunkProvider().chunkExists(x, z);
    }

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
        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return false;
        }
        ChunkProviderServer chunkServer = (ChunkProviderServer) provider;
        IChunkProvider chunkProvider = chunkServer.serverChunkGenerator;

        long pos = ChunkCoordIntPair.chunkXZ2Int(x, z);
        Chunk mcChunk;
        if (chunkServer.chunkExists(x, z)) {
            mcChunk = chunkServer.loadChunk(x, z);
            mcChunk.onChunkUnload();
        }
        try {
            Field droppedChunksSetField = chunkServer.getClass().getDeclaredField("field_73248_b");
            droppedChunksSetField.setAccessible(true);
            Set droppedChunksSet = (Set) droppedChunksSetField.get(chunkServer);
            droppedChunksSet.remove(pos);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        chunkServer.id2ChunkMap.remove(pos);
        mcChunk = chunkProvider.provideChunk(x, z);
        chunkServer.id2ChunkMap.add(pos, mcChunk);
        chunkServer.loadedChunks.add(mcChunk);
        if (mcChunk != null) {
            mcChunk.onChunkLoad();
            mcChunk.populateChunk(chunkProvider, chunkProvider, x, z);
        }
        return true;
    }

    @Override
    public boolean loadChunk(World world, int x, int z, boolean generate) {
        return getCachedChunk(world, x, z) != null;
    }

    @Override
    public Chunk getCachedChunk(World world, int x, int z) {
        Chunk chunk = world.getChunkProvider().provideChunk(x, z);
        if (chunk != null && !chunk.isLoaded()) {
            chunk.onChunkLoad();
        }
        return chunk;
    }

    @Override
    public int getCombinedId4Data(char[] ls, int x, int y, int z) {
        return ls[FaweCache.CACHE_J[y][x & 15][z & 15]];
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.getChunkProvider().chunkExists(x, z);
    }

    @Override
    public boolean setComponents(FaweChunk fc) {
        ForgeChunk_All fs = (ForgeChunk_All) fc;
        Chunk forgeChunk = fs.getChunk();
        net.minecraft.world.World nmsWorld = forgeChunk.getWorld();
        try {
            boolean flag = !nmsWorld.provider.getHasNoSky();
            // Sections
            ExtendedBlockStorage[] sections = forgeChunk.getBlockStorageArray();
            Map<BlockPos, TileEntity> tiles = forgeChunk.getTileEntityMap();
            ClassInheritanceMultiMap<Entity>[] entities = forgeChunk.getEntityLists();
            // Trim tiles
            Set<Map.Entry<BlockPos, TileEntity>> entryset = tiles.entrySet();
            Iterator<Map.Entry<BlockPos, TileEntity>> iterator = entryset.iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, TileEntity> tile = iterator.next();
                BlockPos pos = tile.getKey();
                int lx = pos.getX() & 15;
                int ly = pos.getY();
                int lz = pos.getZ() & 15;
                int j = FaweCache.CACHE_I[ly][lx][lz];
                int k = FaweCache.CACHE_J[ly][lx][lz];
                char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                if (array[k] != 0) {
                    iterator.remove();
                }
            }
            // Trim entities
            for (int i = 0; i < 16; i++) {
                if ((entities[i] != null) && (fs.getCount(i) >= 4096)) {
                    entities[i] = new ClassInheritanceMultiMap<>(Entity.class);
                }
            }
            // Efficiently merge sections
            for (int j = 0; j < sections.length; j++) {
                if (fs.getCount(j) == 0) {
                    continue;
                }
                char[] newArray = fs.getIdArray(j);
                if (newArray == null) {
                    continue;
                }
                ExtendedBlockStorage section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    section = new ExtendedBlockStorage(j << 4, flag);
                    section.setData(newArray);
                    sections[j] = section;
                    continue;
                }
                char[] currentArray = section.getData();
                boolean fill = true;
                for (int k = 0; k < newArray.length; k++) {
                    char n = newArray[k];
                    switch (n) {
                        case 0:
                            fill = false;
                            continue;
                        case 1:
                            fill = false;
                            currentArray[k] = 0;
                            continue;
                        default:
                            currentArray[k] = n;
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
        int[][] biomes = fs.biomes;
        if (biomes != null) {
            for (int x = 0; x < 16; x++) {
                int[] array = biomes[x];
                if (array == null) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    int biome = array[z];
                    if (biome == 0) {
                        continue;
                    }
                    forgeChunk.getBiomeArray()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome;
                }
            }
        }
        sendChunk(fs);
        return true;
    }

    @Override
    public void refreshChunk(World world, Chunk chunk) {
        if (!chunk.isLoaded()) {
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
            BlockPos loc = player.getPosition();
            int px = loc.getX() >> 4;
            int pz = loc.getZ() >> 4;
            int dx = Math.abs(cx - (loc.getX() >> 4));
            int dz = Math.abs(cz - (loc.getZ() >> 4));
            if ((dx > view) || (dz > view)) {
                continue;
            }
            NetHandlerPlayServer con = nmsPlayer.playerNetServerHandler;
            con.sendPacket(new S21PacketChunkData(chunk, false, 65535));
            // Try sending true, 0 first
            // Try bulk chunk packet
        }
    }

    @Override
    public FaweChunk<Chunk> getChunk(int x, int z) {
        return new ForgeChunk_All(this, x, z);
    }

    @Override
    public boolean fixLighting(FaweChunk chunk, boolean fixAll) {
        try {
            ForgeChunk_All fc = (ForgeChunk_All) chunk;
            Chunk forgeChunk = fc.getChunk();
            if (!forgeChunk.isLoaded()) {
                forgeChunk.onChunkLoad();
            }
            forgeChunk.generateSkylightMap();
            if (fc.getTotalRelight() == 0 && !fixAll) {
                return true;
            }
            ExtendedBlockStorage[] sections = forgeChunk.getBlockStorageArray();
            net.minecraft.world.World nmsWorld = forgeChunk.getWorld();

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
                char[] array = section.getData();
                int l = PseudoRandom.random.random(2);
                for (int k = 0; k < array.length; k++) {
                    int i = array[k];
                    if (i < 16) {
                        continue;
                    }
                    short id = (short) (i >> 4);
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
                            BlockPos pos = new BlockPos(X + x, y, Z + z);
                            nmsWorld.checkLight(pos);
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
        char[] array = section.getData();
        int j = FaweCache.CACHE_J[y][x][z];
        return array[j] >> 4;
    }
}

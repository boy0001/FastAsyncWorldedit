package com.boydti.fawe.sponge.v1_8;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.PseudoRandom;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SpongeQueue_1_8 extends NMSMappedFaweQueue<World, net.minecraft.world.chunk.Chunk, ExtendedBlockStorage[], char[]> {
    public SpongeQueue_1_8(String world) {
        super(world);
    }

    @Override
    public void refreshChunk(World world, net.minecraft.world.chunk.Chunk chunk) {
        if (!chunk.isLoaded()) {
            return;
        }
        int cx = chunk.xPosition;
        int cz = chunk.zPosition;
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            int view = player.getViewDistance();
            EntityPlayerMP nmsPlayer = (EntityPlayerMP) player;
            Location<World> loc = player.getLocation();
            int px = loc.getBlockX() >> 4;
            int pz = loc.getBlockZ() >> 4;
            int dx = Math.abs(cx - (loc.getBlockX() >> 4));
            int dz = Math.abs(cz - (loc.getBlockZ() >> 4));
            if ((dx > view) || (dz > view)) {
                continue;
            }
            NetHandlerPlayServer con = nmsPlayer.playerNetServerHandler;
            net.minecraft.world.chunk.Chunk  nmsChunk = (net.minecraft.world.chunk.Chunk) chunk;
            con.sendPacket(new S21PacketChunkData(chunk, false, 65535));
        }
    }

    @Override
    public char[] getCachedSection(ExtendedBlockStorage[] chunk, int cy) {
        ExtendedBlockStorage value = chunk[cy];
        return value == null ? null : value.getData();
    }

    @Override
    public World getWorld(String world) {
        return Sponge.getServer().getWorld(this.world).get();
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        Chunk chunk = world.getChunk(x << 4, 0, z << 4).orElse(null);
        return chunk != null && chunk.isLoaded();
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z) {
        try {
            net.minecraft.world.World nmsWorld = (net.minecraft.world.World) world;
            IChunkProvider provider = nmsWorld.getChunkProvider();
            if (!(provider instanceof ChunkProviderServer)) {
                return false;
            }
            ChunkProviderServer chunkServer = (ChunkProviderServer) provider;
            Field chunkProviderField = chunkServer.getClass().getDeclaredField("field_73246_d");
            chunkProviderField.setAccessible(true);
            IChunkProvider chunkProvider = (IChunkProvider) chunkProviderField.get(chunkServer);
            long pos = ChunkCoordIntPair.chunkXZ2Int(x, z);
            net.minecraft.world.chunk.Chunk mcChunk;
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                mcChunk.onChunkUnload();
            }
            Field droppedChunksSetField = chunkServer.getClass().getDeclaredField("field_73248_b");
            droppedChunksSetField.setAccessible(true);
            Set droppedChunksSet = (Set) droppedChunksSetField.get(chunkServer);
            droppedChunksSet.remove(pos);
            Field id2ChunkMapField = chunkServer.getClass().getDeclaredField("field_73244_f");
            id2ChunkMapField.setAccessible(true);
            LongHashMap<net.minecraft.world.chunk.Chunk> id2ChunkMap = (LongHashMap<net.minecraft.world.chunk.Chunk>) id2ChunkMapField.get(chunkServer);
            id2ChunkMap.remove(pos);
            mcChunk = chunkProvider.provideChunk(x, z);
            id2ChunkMap.add(pos, mcChunk);
            List<net.minecraft.world.chunk.Chunk> loadedChunks = chunkServer.func_152380_a();
            loadedChunks.add(mcChunk);
            if (mcChunk != null) {
                mcChunk.onChunkLoad();
                mcChunk.populateChunk(chunkProvider, chunkProvider, x, z);
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean setComponents(FaweChunk fc) {
        SpongeChunk_1_8 fs = (SpongeChunk_1_8) fc;
        net.minecraft.world.chunk.Chunk nmsChunk = fs.getChunk();
        net.minecraft.world.World nmsWorld = nmsChunk.getWorld();
        try {
            boolean flag = !nmsWorld.provider.getHasNoSky();
            // Sections
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            Map<BlockPos, TileEntity> tiles = nmsChunk.getTileEntityMap();
            ClassInheritanceMultiMap<Entity>[] entities = nmsChunk.getEntityLists();
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
                int count = fs.getCount(j);
                if (count == 0) {
                    continue;
                }
                char[] newArray = fs.getIdArray(j);
                if (newArray == null) {
                    continue;
                }
                ExtendedBlockStorage section = sections[j];

                if ((section == null)) {
                    section = new ExtendedBlockStorage(j << 4, flag);
                    section.setData(newArray);
                    sections[j] = section;
                    continue;
                } else if (count >= 4096){
                    section.setData(newArray);
                    setCount(0, count - fs.getAir(j), section);
                    continue;
                }
                char[] currentArray = section.getData();
                boolean fill = true;
                int solid = 0;
                for (int k = 0; k < newArray.length; k++) {
                    char n = newArray[k];
                    switch (n) {
                        case 0:
                            fill = false;
                            continue;
                        case 1:
                            fill = false;
                            if (currentArray[k] > 1) {
                                solid++;
                            }
                            currentArray[k] = 0;
                            continue;
                        default:
                            solid++;
                            currentArray[k] = n;
                            continue;
                    }
                }
                setCount(0, solid, section);
                if (fill) {
                    fs.setCount(j, Short.MAX_VALUE);
                }
            }
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
                    nmsChunk.getBiomeArray()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome;
                }
            }
        }
        sendChunk(fs);
        return true;
    }

    public void setCount(int tickingBlockCount, int nonEmptyBlockCount, ExtendedBlockStorage section) throws NoSuchFieldException, IllegalAccessException {
        Class<? extends ExtendedBlockStorage> clazz = section.getClass();
        Field fieldTickingBlockCount = clazz.getDeclaredField("field_76683_c");
        Field fieldNonEmptyBlockCount = clazz.getDeclaredField("field_76682_b");
        fieldTickingBlockCount.setAccessible(true);
        fieldNonEmptyBlockCount.setAccessible(true);
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    @Override
    public FaweChunk<net.minecraft.world.chunk.Chunk> getChunk(int x, int z) {
        return new SpongeChunk_1_8(this, x, z);
    }


    @Override
    public boolean fixLighting(FaweChunk fc, boolean fixAll) {
        try {
            SpongeChunk_1_8 bc = (SpongeChunk_1_8) fc;
            net.minecraft.world.chunk.Chunk nmsChunk = bc.getChunk();
            if (!nmsChunk.isLoaded()) {
                if (!((Chunk) nmsChunk).loadChunk(false)) {
                    return false;
                }
            }
            nmsChunk.generateSkylightMap();
            if (bc.getTotalRelight() == 0 && !fixAll) {
                return true;
            }
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            net.minecraft.world.World nmsWorld = nmsChunk.getWorld();

            int X = bc.getX() << 4;
            int Z = bc.getZ() << 4;


            for (int j = 0; j < sections.length; j++) {
                ExtendedBlockStorage section = sections[j];
                if (section == null) {
                    continue;
                }
                if ((bc.getRelight(j) == 0 && !fixAll) || bc.getCount(j) == 0 || (bc.getCount(j) >= 4096 && bc.getAir(j) == 0)) {
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

    @Override
    public boolean loadChunk(World world, int x, int z, boolean generate) {
        return getCachedChunk(world, x, z) != null;
    }

    @Override
    public ExtendedBlockStorage[] getCachedChunk(World world, int cx, int cz) {
        Chunk chunk = world.loadChunk(cx, 0, cz, true).orElse(null);
        return ((net.minecraft.world.chunk.Chunk) chunk).getBlockStorageArray();
    }

    @Override
    public int getCombinedId4Data(char[] chars, int x, int y, int z) {
        return chars[FaweCache.CACHE_J[y][x & 15][z & 15]];
    }
}

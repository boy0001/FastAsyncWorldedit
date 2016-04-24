package com.boydti.fawe.forge.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.forge.ForgePlayer;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
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

public class ForgeQueue_All extends FaweQueue {

    private World forgeWorld;

    private ConcurrentHashMap<Long, FaweChunk<Chunk>> blocks = new ConcurrentHashMap<>();
    private LinkedBlockingDeque<FaweChunk<Chunk>> chunks = new LinkedBlockingDeque<>();

    public ForgeQueue_All(final String world) {
        super(world);
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return getWorld().getChunkProvider().chunkExists(x, z);
    }

    public World getWorld() {
        if (forgeWorld != null) {
            return forgeWorld;
        }
        WorldServer[] worlds = MinecraftServer.getServer().worldServers;
        for (WorldServer ws : worlds) {
            if (ws.provider.getDimensionName().equals(world)) {
                return forgeWorld = ws;
            }
        }
        return null;
    }

    @Override
    public boolean regenerateChunk(int x, int z) {
        IChunkProvider provider = getWorld().getChunkProvider();
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
    public void addTask(int x, int z, Runnable runnable) {
        long pair = (long) (x) << 32 | (z) & 0xFFFFFFFFL;
        FaweChunk<Chunk> result = this.blocks.get(pair);
        if (result == null) {
            result = this.getChunk(x, z);
            result.addTask(runnable);
            FaweChunk<Chunk> previous = this.blocks.put(pair, result);
            if (previous == null) {
                chunks.add(result);
                return;
            }
            this.blocks.put(pair, previous);
            result = previous;
        }
        result.addTask(runnable);
    }

    private int lcx = Integer.MIN_VALUE;
    private int lcz = Integer.MIN_VALUE;
    private int lcy = Integer.MIN_VALUE;
    private net.minecraft.world.chunk.Chunk lc;
    private char[] ls;

    private final RunnableVal<IntegerPair> loadChunk = new RunnableVal<IntegerPair>() {
        @Override
        public void run(IntegerPair loc) {
            getWorld().getChunkProvider().provideChunk(loc.x, loc.z);
        }
    };

    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        if (y < 0 || y > 255) {
            return 0;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lcx || cz != lcz) {
            World world = getWorld();
            lcx = cx;
            lcz = cz;
            IChunkProvider provider = world.getChunkProvider();
            Chunk chunk;
            if (!provider.chunkExists(lcx, lcz)) {
                boolean sync = Thread.currentThread() == Fawe.get().getMainThread();
                if (sync) {
                    chunk = provider.provideChunk(cx, cz);
                } else if (Settings.CHUNK_WAIT > 0) {
                    loadChunk.value = new IntegerPair(cx, cz);
                    TaskManager.IMP.sync(loadChunk, Settings.CHUNK_WAIT);
                    if (!provider.chunkExists(cx, cz)) {
                        throw new FaweException.FaweChunkLoadException();
                    }
                    chunk = provider.provideChunk(cx, cz);
                } else {
                    return 0;
                }
            } else {
                chunk = provider.provideChunk(cx, cz);
            }
            lc = chunk;
        } else if (cy == lcy) {
            return ls != null ? ls[FaweCache.CACHE_J[y][x & 15][z & 15]] : 0;
        }
        ExtendedBlockStorage storage = lc.getBlockStorageArray()[cy];
        if (storage == null) {
            ls = null;
            return 0;
        }
        ls = storage.getData();
        return ls[FaweCache.CACHE_J[y][x & 15][z & 15]];
    }

    private FaweChunk lastChunk;
    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;

    @Override
    public boolean setBlock(int x, int y, int z, short id, byte data) {
        if ((y > 255) || (y < 0)) {
            return false;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        if (cx != lastX || cz != lastZ) {
            lastX = cx;
            lastZ = cz;
            long pair = (long) (cx) << 32 | (cz) & 0xFFFFFFFFL;
            lastChunk = this.blocks.get(pair);
            if (lastChunk == null) {
                lastChunk = this.getChunk(x >> 4, z >> 4);
                lastChunk.setBlock(x & 15, y, z & 15, id, data);
                FaweChunk<Chunk> previous = this.blocks.put(pair, lastChunk);
                if (previous == null) {
                    chunks.add(lastChunk);
                    return true;
                }
                this.blocks.put(pair, previous);
                lastChunk = previous;
            }
        }
        lastChunk.setBlock(x & 15, y, z & 15, id, data);
        return true;
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        long pair = (long) (x >> 4) << 32 | (z >> 4) & 0xFFFFFFFFL;
        FaweChunk<Chunk> result = this.blocks.get(pair);
        if (result == null) {
            result = this.getChunk(x >> 4, z >> 4);
            FaweChunk<Chunk> previous = this.blocks.put(pair, result);
            if (previous != null) {
                this.blocks.put(pair, previous);
                result = previous;
            } else {
                chunks.add(result);
            }
        }
        result.setBiome(x & 15, z & 15, biome);
        return true;
    }

    @Override
    public FaweChunk<Chunk> next() {
        lastX = Integer.MIN_VALUE;
        lastZ = Integer.MIN_VALUE;
        try {
            if (this.blocks.size() == 0) {
                return null;
            }
            synchronized (blocks) {
                FaweChunk<Chunk> chunk = chunks.poll();
                if (chunk != null) {
                    blocks.remove(chunk.longHash());
                    this.execute(chunk);
                    return chunk;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int size() {
        return chunks.size();
    }

    private LinkedBlockingDeque<FaweChunk<Chunk>> toUpdate = new LinkedBlockingDeque<>();

    public boolean execute(FaweChunk<Chunk> fc) {
        if (fc == null) {
            return false;
        }
        // Load chunk
        Chunk chunk = fc.getChunk();
        if (!chunk.isLoaded()) {
            chunk.onChunkLoad();
        }
        // Set blocks / entities / biome
        if (!this.setComponents(fc)) {
            return false;
        }
        fc.executeTasks();
        return true;
    }

    @Override
    public void clear() {
        this.blocks.clear();
        this.chunks.clear();
    }

    @Override
    public void setChunk(FaweChunk<?> chunk) {
        FaweChunk<Chunk> previous = this.blocks.put(chunk.longHash(), (FaweChunk<Chunk>) chunk);
        if (previous != null) {
            chunks.remove(previous);
        }
        chunks.add((FaweChunk<Chunk>) chunk);
    }

    public void sendChunk(FaweChunk<Chunk> fc) {
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                final boolean result = fixLighting(fc, Settings.FIX_ALL_LIGHTING) || !Settings.ASYNC_LIGHTING;
                TaskManager.IMP.task(new Runnable() {
                    @Override
                    public void run() {
                        if (!result) {
                            fixLighting(fc, Settings.FIX_ALL_LIGHTING);
                        }
                        Chunk chunk = fc.getChunk();
                        if (!chunk.isLoaded()) {
                            return;
                        }
                        World world = chunk.getWorld();
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
                }, false);
            }
        }, Settings.ASYNC_LIGHTING);
    }

    public boolean setComponents(FaweChunk<Chunk> fc) {
        ForgeChunk_All fs = (ForgeChunk_All) fc;
        Chunk forgeChunk = fc.getChunk();
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
    public FaweChunk<Chunk> getChunk(int x, int z) {
        return new ForgeChunk_All(this, x, z);
    }

    @Override
    public boolean fixLighting(FaweChunk<?> chunk, boolean fixAll) {
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

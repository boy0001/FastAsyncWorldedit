package com.boydti.fawe.bukkit.v1_7;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_7_R4.Block;
import net.minecraft.server.v1_7_R4.Chunk;
import net.minecraft.server.v1_7_R4.ChunkCoordIntPair;
import net.minecraft.server.v1_7_R4.ChunkPosition;
import net.minecraft.server.v1_7_R4.ChunkSection;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EntityTracker;
import net.minecraft.server.v1_7_R4.EntityTypes;
import net.minecraft.server.v1_7_R4.EnumDifficulty;
import net.minecraft.server.v1_7_R4.EnumGamemode;
import net.minecraft.server.v1_7_R4.EnumSkyBlock;
import net.minecraft.server.v1_7_R4.LongHashMap;
import net.minecraft.server.v1_7_R4.MinecraftServer;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NibbleArray;
import net.minecraft.server.v1_7_R4.PacketPlayOutMapChunk;
import net.minecraft.server.v1_7_R4.PlayerChunkMap;
import net.minecraft.server.v1_7_R4.ServerNBTManager;
import net.minecraft.server.v1_7_R4.TileEntity;
import net.minecraft.server.v1_7_R4.WorldManager;
import net.minecraft.server.v1_7_R4.WorldServer;
import net.minecraft.server.v1_7_R4.WorldSettings;
import net.minecraft.server.v1_7_R4.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_7_R4.CraftChunk;
import org.bukkit.craftbukkit.v1_7_R4.CraftServer;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.ChunkGenerator;

public class BukkitQueue17 extends BukkitQueue_0<net.minecraft.server.v1_7_R4.Chunk, ChunkSection[], ChunkSection> {

    protected static Field fieldData;
    protected static Field fieldIds;
    protected static Field fieldCompactId;
    protected static Field fieldCompactData;
    protected static Field fieldTickingBlockCount;
    protected static Field fieldNonEmptyBlockCount;
    protected static Field fieldBiomes;
    protected static Field fieldSeed;
    protected static Field fieldBiomeCache;
    protected static Field fieldBiomes2;
    protected static Field fieldGenLayer1;
    protected static Field fieldGenLayer2;
    protected static com.boydti.fawe.bukkit.v1_7.MutableGenLayer genLayer;
    protected static ChunkSection emptySection;


    static {
        try {
            emptySection = new ChunkSection(0, true);
            fieldData = ChunkSection.class.getDeclaredField("blockData");
            fieldData.setAccessible(true);
            fieldIds = ChunkSection.class.getDeclaredField("blockIds");
            fieldIds.setAccessible(true);
            fieldCompactId = ChunkSection.class.getDeclaredField("compactId");
            fieldCompactId.setAccessible(true);
            fieldCompactData = ChunkSection.class.getDeclaredField("compactData");
            fieldCompactData.setAccessible(true);
            fieldTickingBlockCount = ChunkSection.class.getDeclaredField("tickingBlockCount");
            fieldNonEmptyBlockCount = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount.setAccessible(true);

            fieldBiomes = net.minecraft.server.v1_7_R4.ChunkProviderGenerate.class.getDeclaredField("z");
            fieldBiomes.setAccessible(true);
            fieldSeed = net.minecraft.server.v1_7_R4.WorldData.class.getDeclaredField("seed");
            fieldSeed.setAccessible(true);
            fieldBiomeCache = net.minecraft.server.v1_7_R4.WorldChunkManager.class.getDeclaredField("e");
            fieldBiomeCache.setAccessible(true);
            fieldBiomes2 = net.minecraft.server.v1_7_R4.WorldChunkManager.class.getDeclaredField("f");
            fieldBiomes2.setAccessible(true);
            fieldGenLayer1 = net.minecraft.server.v1_7_R4.WorldChunkManager.class.getDeclaredField("c");
            fieldGenLayer2 = net.minecraft.server.v1_7_R4.WorldChunkManager.class.getDeclaredField("d");
            fieldGenLayer1.setAccessible(true);
            fieldGenLayer2.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public BukkitQueue17(final com.sk89q.worldedit.world.World world) {
        super(world);
        getImpWorld();
    }

    public BukkitQueue17(final String world) {
        super(world);
        getImpWorld();
    }

    @Override
    public void saveChunk(Chunk nmsChunk) {
        nmsChunk.e(); // Modified
        nmsChunk.mustSave = true;
    }

    @Override
    public boolean setMCA(final int mcaX, final int mcaZ, final RegionWrapper allowed, final Runnable whileLocked, boolean saveChunks, final boolean load) {
        throw new UnsupportedOperationException();
        /*TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                long start = System.currentTimeMillis();
                long last = start;
                synchronized (net.minecraft.server.v1_7_R4.RegionFileCache.class) {
                    World world = getWorld();
                    if (world.getKeepSpawnInMemory()) world.setKeepSpawnInMemory(false);
                    net.minecraft.server.v1_7_R4.ChunkProviderServer provider = nmsWorld.chunkProviderServer;

                    boolean mustSave = false;
                    boolean[][] chunksUnloaded = null;
                    { // Unload chunks
                        Iterator<net.minecraft.server.v1_7_R4.Chunk> iter = provider.a().iterator();
                        while (iter.hasNext()) {
                            net.minecraft.server.v1_7_R4.Chunk chunk = iter.next();
                            if (chunk.locX >> 5 == mcaX && chunk.locZ >> 5 == mcaZ) {
                                boolean isIn = allowed.isInChunk(chunk.locX, chunk.locZ);
                                if (isIn) {
                                    if (!load) {
                                        if (saveChunks && chunk.a(false)) {
                                            mustSave = true;
                                            provider.saveChunk(chunk);
                                            provider.saveChunkNOP(chunk);
                                        }
                                        continue;
                                    }
                                    iter.remove();
                                    boolean save = saveChunks && chunk.a(false);
                                    mustSave |= save;
                                    chunk.bukkitChunk.unload(save, false);
                                    if (chunksUnloaded == null) {
                                        chunksUnloaded = new boolean[32][];
                                    }
                                    int relX = chunk.locX & 31;
                                    boolean[] arr = chunksUnloaded[relX];
                                    if (arr == null) {
                                        arr = chunksUnloaded[relX] = new boolean[32];
                                    }
                                    arr[chunk.locZ & 31] = true;
                                }
                            }
                        }
                    }
                    if (mustSave) provider.c(); // TODO only the necessary chunks

                    File unloadedRegion = null;
                    if (load && !net.minecraft.server.v1_7_R4.RegionFileCache.a.isEmpty()) {
                        Map<File, net.minecraft.server.v1_7_R4.RegionFile> map = net.minecraft.server.v1_7_R4.RegionFileCache.a;
                        Iterator<Map.Entry<File, net.minecraft.server.v1_7_R4.RegionFile>> iter = map.entrySet().iterator();
                        String requiredPath = world.getName() + File.separator + "region";
                        while (iter.hasNext()) {
                            Map.Entry<File, net.minecraft.server.v1_7_R4.RegionFile> entry = iter.next();
                            File file = entry.getKey();
                            int[] regPos = MainUtil.regionNameToCoords(file.getPath());
                            if (regPos[0] == mcaX && regPos[1] == mcaZ && file.getPath().contains(requiredPath)) {
                                if (file.exists()) {
                                    unloadedRegion = file;
                                    net.minecraft.server.v1_7_R4.RegionFile regionFile = entry.getValue();
                                    iter.remove();
                                    try {
                                        regionFile.c();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            }
                        }
                    }

                    long now = System.currentTimeMillis();
                    if (whileLocked != null) whileLocked.run();
                    if (!load) return;

                    { // Load the region again
                        if (unloadedRegion != null && chunksUnloaded != null && unloadedRegion.exists()) {
                            final boolean[][] finalChunksUnloaded = chunksUnloaded;
                            TaskManager.IMP.async(() -> {
                                int bx = mcaX << 5;
                                int bz = mcaZ << 5;
                                for (int x = 0; x < finalChunksUnloaded.length; x++) {
                                    boolean[] arr = finalChunksUnloaded[x];
                                    if (arr != null) {
                                        for (int z = 0; z < arr.length; z++) {
                                            if (arr[z]) {
                                                int cx = bx + x;
                                                int cz = bz + z;
                                                TaskManager.IMP.sync(new RunnableVal<Object>() {
                                                    @Override
                                                    public void run(Object value1) {
                                                        net.minecraft.server.v1_7_R4.Chunk chunk = provider.getChunkAt(cx, cz, null);
                                                        if (chunk != null) {
                                                            if (nmsWorld.getPlayerChunkMap().isChunkInUse(cx, cz)) {
                                                                sendChunk(chunk, 0);
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
        return true;*/
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z, BaseBiome biome, Long seed) {
        if (biome != null) {
            try {
                if (seed == null) {
                    seed = world.getSeed();
                }
                nmsWorld.worldData.getSeed();
                boolean result;
                net.minecraft.server.v1_7_R4.ChunkProviderGenerate generator = new net.minecraft.server.v1_7_R4.ChunkProviderGenerate(nmsWorld, seed, false);
                Biome bukkitBiome = getAdapter().getBiome(biome.getId());
                net.minecraft.server.v1_7_R4.BiomeBase base = net.minecraft.server.v1_7_R4.BiomeBase.getBiome(biome.getId());
                fieldBiomes.set(generator, new net.minecraft.server.v1_7_R4.BiomeBase[]{base});
                net.minecraft.server.v1_7_R4.IChunkProvider existingGenerator = nmsWorld.chunkProviderServer.chunkProvider;
                long existingSeed = world.getSeed();
                {
                    if (genLayer == null) genLayer = new com.boydti.fawe.bukkit.v1_7.MutableGenLayer(seed);
                    genLayer.set(biome.getId());
                    Object existingGenLayer1 = fieldGenLayer1.get(nmsWorld.getWorldChunkManager());
                    Object existingGenLayer2 = fieldGenLayer2.get(nmsWorld.getWorldChunkManager());
                    fieldGenLayer1.set(nmsWorld.getWorldChunkManager(), genLayer);
                    fieldGenLayer2.set(nmsWorld.getWorldChunkManager(), genLayer);

                    fieldSeed.set(nmsWorld.worldData, seed);

                    ReflectionUtils.setFailsafeFieldValue(fieldBiomeCache, this.nmsWorld.getWorldChunkManager(), new net.minecraft.server.v1_7_R4.BiomeCache(this.nmsWorld.getWorldChunkManager()));

                    nmsWorld.chunkProviderServer.chunkProvider = generator;

                    keepLoaded.remove(MathMan.pairInt(x, z));
                    result = getWorld().regenerateChunk(x, z);

                    nmsWorld.chunkProviderServer.chunkProvider = existingGenerator;

                    fieldSeed.set(nmsWorld.worldData, existingSeed);

                    fieldGenLayer1.set(nmsWorld.getWorldChunkManager(), existingGenLayer1);
                    fieldGenLayer2.set(nmsWorld.getWorldChunkManager(), existingGenLayer2);
                }
                return result;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return super.regenerateChunk(world, x, z, biome, seed);
    }

    @Override
    public void setHeightMap(FaweChunk chunk, byte[] heightMap) {
        CraftChunk craftChunk = (CraftChunk) chunk.getChunk();
        if (craftChunk != null) {
            int[] otherMap = craftChunk.getHandle().heightMap;
            for (int i = 0; i < heightMap.length; i++) {
                int newHeight = heightMap[i] & 0xFF;
                int currentHeight = otherMap[i];
                if (newHeight > currentHeight) {
                    otherMap[i] = newHeight;
                }
            }
        }
    }

    public World getWorld(String world) {
        return Bukkit.getWorld(world);
    }

    @Override
    public int getBiome(net.minecraft.server.v1_7_R4.Chunk chunk, int x, int z) {
        return chunk.m()[((z & 15) << 4) + (x & 15)];
    }

    @Override
    public net.minecraft.server.v1_7_R4.ChunkSection[] getSections(net.minecraft.server.v1_7_R4.Chunk chunk) {
        return chunk.getSections();
    }

    @Override
    public net.minecraft.server.v1_7_R4.Chunk loadChunk(World world, int x, int z, boolean generate) {
        net.minecraft.server.v1_7_R4.Chunk chunk;
        net.minecraft.server.v1_7_R4.ChunkProviderServer provider = ((org.bukkit.craftbukkit.v1_7_R4.CraftWorld) world).getHandle().chunkProviderServer;
        if (generate) {
            return provider.originalGetChunkAt(x, z);
        } else {
            return provider.loadChunk(x, z);
        }
    }

    @Override
    public net.minecraft.server.v1_7_R4.ChunkSection[] getCachedSections(World world, int cx, int cz) {
        net.minecraft.server.v1_7_R4.Chunk chunk = ((org.bukkit.craftbukkit.v1_7_R4.CraftWorld) world).getHandle().chunkProviderServer.getChunkIfLoaded(cx, cz);
        if (chunk != null) {
            return chunk.getSections();
        }
        return null;
    }

    @Override
    public net.minecraft.server.v1_7_R4.Chunk getCachedChunk(World world, int cx, int cz) {
        return ((org.bukkit.craftbukkit.v1_7_R4.CraftWorld) world).getHandle().chunkProviderServer.getChunkIfLoaded(cx, cz);
    }

    @Override
    public net.minecraft.server.v1_7_R4.ChunkSection getCachedSection(net.minecraft.server.v1_7_R4.ChunkSection[] chunkSections, int cy) {
        return chunkSections[cy];
    }

    @Override
    public void setFullbright(ChunkSection[] sections) {
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section != null) {
                byte[] bytes = section.getSkyLightArray().a;
                Arrays.fill(bytes, Byte.MAX_VALUE);
            }
        }
    }

    @Override
    public int getCombinedId4Data(ChunkSection ls, int x, int y, int z) {
        byte[] ids = ls.getIdArray();
        NibbleArray datasNibble = ls.getDataArray();
        int i = FaweCache.CACHE_J[y & 15][z & 15][x & 15];
        int combined = ((ids[i] & 0xFF)  << 4) + (datasNibble == null ? 0 : datasNibble.a(x & 15, y & 15, z & 15));
        return combined;
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, ChunkSection[] sections, Map<?, ?> tilesGeneric, Collection<?>[] entitiesGeneric, Set<UUID> createdEntities, boolean all) throws Exception {
        Map<ChunkPosition, TileEntity> tiles = (Map<ChunkPosition, TileEntity>) tilesGeneric;
        Collection<Entity>[] entities = (Collection<Entity>[]) entitiesGeneric;
        CharFaweChunk previous = (CharFaweChunk) getFaweChunk(fs.getX(), fs.getZ());
        for (int layer = 0; layer < sections.length; layer++) {
            if (fs.getCount(layer) != 0 || all) {
                ChunkSection section = sections[layer];
                if (section != null) {
                    byte[] currentIdArray = section.getIdArray();
                    NibbleArray currentDataArray = section.getDataArray();
                    char[] array = new char[4096];
                    for (int j = 0; j < currentIdArray.length; j++) {
                        int x = FaweCache.CACHE_X[layer][j];
                        int y = FaweCache.CACHE_Y[layer][j];
                        int z = FaweCache.CACHE_Z[layer][j];
                        int id = currentIdArray[j] & 0xFF;
                        byte data = (byte) currentDataArray.a(x, y & 15, z);
                        previous.setBlock(x, y, z, id, data);
                    }
                }
            }
        }
        if (tiles != null) {
            for (Map.Entry<ChunkPosition, TileEntity> entry : tiles.entrySet()) {
                TileEntity tile = entry.getValue();
                NBTTagCompound tag = new NBTTagCompound();
                ChunkPosition pos = entry.getKey();
                CompoundTag nativeTag = getTag(tile);
                previous.setTile(pos.x & 15, pos.y, pos.z & 15, nativeTag);
            }
        }
        if (entities != null) {
            for (Collection<Entity> entityList : entities) {
                for (Entity ent : entityList) {
                    if (ent instanceof EntityPlayer || (!createdEntities.isEmpty() && createdEntities.contains(ent.getUniqueID()))) {
                        continue;
                    }
                    int x = (MathMan.roundInt(ent.locX) & 15);
                    int z = (MathMan.roundInt(ent.locZ) & 15);
                    int y = (MathMan.roundInt(ent.locY) & 0xFF);
                    int i = FaweCache.CACHE_I[y][z][x];
                    char[] array = fs.getIdArray(i);
                    if (array == null) {
                        continue;
                    }
                    int j = FaweCache.CACHE_J[y][z][x];
                    if (array[j] != 0) {
                        String id = EntityTypes.b(ent);
                        if (id != null) {
                            NBTTagCompound tag = new NBTTagCompound();
                            ent.e(tag); // readEntityIntoTag
                            CompoundTag nativeTag = (CompoundTag) toNative(tag);
                            Map<String, Tag> map = ReflectionUtils.getMap(nativeTag.getValue());
                            map.put("Id", new StringTag(id));
                            previous.setEntity(nativeTag);
                        }
                    }
                }
            }
        }
        return previous;
    }

    public CompoundTag getTag(TileEntity tile) {
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.b(tag); // readTagIntoEntity
            return (CompoundTag) toNative(tag);
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    @Override
    public CompoundTag getTileEntity(net.minecraft.server.v1_7_R4.Chunk chunk, int x, int y, int z) {
        Map<ChunkPosition, TileEntity> tiles = chunk.tileEntities;
        ChunkPosition pos = new ChunkPosition(x & 15, y, z & 15);
        TileEntity tile = tiles.get(pos);
        return tile != null ? getTag(tile) : null;
    }


    public void setCount(int tickingBlockCount, int nonEmptyBlockCount, ChunkSection section) throws NoSuchFieldException, IllegalAccessException {
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    public int getNonEmptyBlockCount(ChunkSection section) throws IllegalAccessException {
        return (int) fieldNonEmptyBlockCount.get(section);
    }

    @Override
    public void sendChunk(int x, int z, int bitMask) {
        net.minecraft.server.v1_7_R4.Chunk chunk = getCachedChunk(getWorld(), x, z);
        if (chunk != null) {
            sendChunk(chunk, bitMask);
        }
    }

    @Override
    public void refreshChunk(FaweChunk fc) {
        net.minecraft.server.v1_7_R4.Chunk chunk = getCachedChunk(getWorld(), fc.getX(), fc.getZ());
        if (chunk != null) {
            sendChunk(chunk, fc.getBitMask());
        }
    }

    public void sendChunk(net.minecraft.server.v1_7_R4.Chunk nmsChunk, final int finalMask) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                try {
                    int mask = finalMask;
                    ChunkCoordIntPair pos = nmsChunk.l(); // getPosition()
                    WorldServer w = (WorldServer) nmsChunk.world;
                    PlayerChunkMap chunkMap = w.getPlayerChunkMap();
                    int x = pos.x;
                    int z = pos.z;
                    if (!chunkMap.isChunkInUse(x, z)) {
                        return;
                    }
                    HashSet<EntityPlayer> set = new HashSet<EntityPlayer>();
                    EntityTracker tracker = w.getTracker();
                    // Get players

                    Field fieldChunkMap = chunkMap.getClass().getDeclaredField("d");
                    fieldChunkMap.setAccessible(true);
                    LongHashMap map = (LongHashMap) fieldChunkMap.get(chunkMap);
                    long pair = (long) x + 2147483647L | (long) z + 2147483647L << 32;
                    Object playerChunk = map.getEntry(pair);
                    Field fieldPlayers = playerChunk.getClass().getDeclaredField("b");
                    fieldPlayers.setAccessible(true);
                    final HashSet<EntityPlayer> players = new HashSet<>((Collection<EntityPlayer>)fieldPlayers.get(playerChunk));
                    if (players.size() == 0) {
                        return;
                    }
                    // Send chunks
                    boolean empty = false;
                    ChunkSection[] sections = nmsChunk.getSections();
                    for (int i = 0; i < sections.length; i++) {
                        if (sections[i] == null) {
                            sections[i] = emptySection;
                            empty = true;
                        }
                    }
                    for (EntityPlayer player : players) {
                        if (mask == 0 || mask == 65535 && hasEntities(nmsChunk)) {
                            PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, 65280);
                            player.playerConnection.sendPacket(packet);
                            mask = 255;
                        }
                        PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, mask);
                        player.playerConnection.sendPacket(packet);
                    }
                    if (empty) {
                        for (int i = 0; i < sections.length; i++) {
                            if (sections[i] == emptySection) {
                                sections[i] = null;
                            }
                        }
                    }
                } catch (Throwable e) {
                    MainUtil.handleError(e);
                }
            }
        });
    }

    public boolean hasEntities(net.minecraft.server.v1_7_R4.Chunk nmsChunk) {
        for (int i = 0; i < nmsChunk.entitySlices.length; i++) {
            List<Entity> slice = nmsChunk.entitySlices[i];
            if (slice != null && !slice.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeSectionLighting(ChunkSection section, int layer, boolean sky) {
        if (section != null) {
            section.setEmittedLightArray(null);
            if (sky) {
                section.setSkyLightArray(null);
            }
            return true;
        }
        return false;
    }

    @Override
    public World createWorld(final WorldCreator creator) {
        final String name = creator.name();
        ChunkGenerator generator = creator.generator();
        final CraftServer server = (CraftServer) Bukkit.getServer();
        final MinecraftServer console = server.getServer();
        final File folder = new File(server.getWorldContainer(), name);
        final World world = server.getWorld(name);
        final WorldType type = WorldType.getType(creator.type().getName());
        final boolean generateStructures = creator.generateStructures();
        if (world != null) {
            return world;
        }
        if (folder.exists() && !folder.isDirectory()) {
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
        }
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                try {
                    Field field = CraftServer.class.getDeclaredField("worlds");
                    field.setAccessible(true);
                    Map<Object, Object> existing = (Map<Object, Object>) field.get(server);
                    if (!existing.getClass().getName().contains("SynchronizedMap")) {
                        field.set(server, Collections.synchronizedMap(existing));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
        if (generator == null) {
            generator = server.getGenerator(name);
        }
        int dimension = 10 + console.worlds.size();
        boolean used = false;
        do {
            for (final WorldServer ws : console.worlds) {
                used = (ws.dimension == dimension);
                if (used) {
                    ++dimension;
                    break;
                }
            }
        } while (used);
        final boolean hardcore = false;
        ServerNBTManager sdm = new ServerNBTManager(server.getWorldContainer(), name, true);
        final WorldSettings worldSettings = new WorldSettings(creator.seed(), EnumGamemode.getById(server.getDefaultGameMode().getValue()), generateStructures, hardcore, type);
        startSet(true);
        disableChunkLoad = true;
        final WorldServer internal = new WorldServer(console, sdm, name, dimension, worldSettings, console.methodProfiler, creator.environment(), generator);
        disableChunkLoad = false;
        endSet(true);
        internal.scoreboard = server.getScoreboardManager().getMainScoreboard().getHandle();
        internal.tracker = new EntityTracker(internal);
        internal.addIWorldAccess(new WorldManager(console, internal));
        internal.difficulty = EnumDifficulty.EASY;
        internal.setSpawnFlags(true, true);
        if (generator != null) {
            internal.getWorld().getPopulators().addAll(generator.getDefaultPopulators(internal.getWorld()));
        }
        // Add the world
        return TaskManager.IMP.sync(new RunnableVal<World>() {
            @Override
            public void run(World value) {
                console.worlds.add(internal);
                server.getPluginManager().callEvent(new WorldInitEvent(internal.getWorld()));
                server.getPluginManager().callEvent(new WorldLoadEvent(internal.getWorld()));
                this.value = internal.getWorld();
            }
        });
    }

    @Override
    public void relight(int x, int y, int z) {
        nmsWorld.t(x, y, z);
    }

    protected WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        World world = super.getImpWorld();
        if (world != null) {
            this.nmsWorld = ((CraftWorld) world).getHandle();
            return super.getImpWorld();
        } else {
            return null;
        }
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return new BukkitChunk_1_7(this, x, z);
    }

    @Override
    public void setSkyLight(ChunkSection section, int x, int y, int z, int value) {
        section.setSkyLight(x & 15, y & 15, z & 15, value);
    }

    @Override
    public void setBlockLight(ChunkSection section, int x, int y, int z, int value) {
        section.setEmittedLight(x & 15, y & 15, z & 15, value);
    }

    @Override
    public int getSkyLight(ChunkSection section, int x, int y, int z) {
        return section.getSkyLight(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmmittedLight(ChunkSection section, int x, int y, int z) {
        return section.getEmittedLight(x & 15, y & 15, z & 15);
    }

    @Override
    public int getOpacity(ChunkSection section, int x, int y, int z) {
        return section.getTypeId(x & 15, y & 15, z & 15).k();
    }

    @Override
    public int getBrightness(ChunkSection section, int x, int y, int z) {
        return section.getTypeId(x & 15, y & 15, z & 15).m();
    }

    @Override
    public boolean hasBlock(ChunkSection section, int x, int y, int z) {
        int i = FaweCache.CACHE_J[y & 15][z & 15][x & 15];
        return section.getIdArray()[i] != 0;
    }

    @Override
    public int getOpacityBrightnessPair(ChunkSection section, int x, int y, int z) {
        Block block = section.getTypeId(x & 15, y & 15, z & 15);
        return MathMan.pair16(block.k(), block.m());
    }

    @Override
    public void relightBlock(int x, int y, int z) {
        nmsWorld.c(EnumSkyBlock.BLOCK, x, y, z);
    }

    @Override
    public void relightSky(int x, int y, int z) {
        nmsWorld.c(EnumSkyBlock.SKY, x, y, z);
    }
}

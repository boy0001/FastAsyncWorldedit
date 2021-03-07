package com.boydti.fawe.bukkit.v1_8;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.SetQueue;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntitySlice;
import net.minecraft.server.v1_8_R3.EntityTracker;
import net.minecraft.server.v1_8_R3.EntityTypes;
import net.minecraft.server.v1_8_R3.EnumDifficulty;
import net.minecraft.server.v1_8_R3.EnumSkyBlock;
import net.minecraft.server.v1_8_R3.IChunkProvider;
import net.minecraft.server.v1_8_R3.LongHashMap;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NibbleArray;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PlayerChunkMap;
import net.minecraft.server.v1_8_R3.ServerNBTManager;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.WorldData;
import net.minecraft.server.v1_8_R3.WorldManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.minecraft.server.v1_8_R3.WorldSettings;
import net.minecraft.server.v1_8_R3.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.ChunkGenerator;

public class BukkitQueue18R3 extends BukkitQueue_0<net.minecraft.server.v1_8_R3.Chunk, ChunkSection[], ChunkSection> {

    public static Field isDirty;

    protected static Field fieldTickingBlockCount;
    protected static Field fieldNonEmptyBlockCount;
    protected static Field fieldSection;
    protected static Field fieldChunkMap;
    protected static Field fieldBiomes;
    protected static Field fieldSeed;
    protected static Field fieldBiomeCache;
    protected static Field fieldBiomes2;
    protected static Field fieldGenLayer1;
    protected static Field fieldGenLayer2;
    protected static com.boydti.fawe.bukkit.v1_8.MutableGenLayer genLayer;
    protected static ChunkSection emptySection;

    static {
        try {
            emptySection = new ChunkSection(0, true);
            fieldSection = ChunkSection.class.getDeclaredField("blockIds");
            fieldTickingBlockCount = ChunkSection.class.getDeclaredField("tickingBlockCount");
            fieldNonEmptyBlockCount = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            fieldChunkMap = PlayerChunkMap.class.getDeclaredField("d");
            fieldSection.setAccessible(true);
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount.setAccessible(true);
            fieldChunkMap.setAccessible(true);

            fieldBiomes = net.minecraft.server.v1_8_R3.ChunkProviderGenerate.class.getDeclaredField("B");
            fieldBiomes.setAccessible(true);
            fieldSeed = net.minecraft.server.v1_8_R3.WorldData.class.getDeclaredField("b");
            fieldSeed.setAccessible(true);
            fieldBiomeCache = net.minecraft.server.v1_8_R3.WorldChunkManager.class.getDeclaredField("d");
            fieldBiomeCache.setAccessible(true);
            fieldBiomes2 = net.minecraft.server.v1_8_R3.WorldChunkManager.class.getDeclaredField("e");
            fieldBiomes2.setAccessible(true);
            fieldGenLayer1 = net.minecraft.server.v1_8_R3.WorldChunkManager.class.getDeclaredField("b") ;
            fieldGenLayer2 = net.minecraft.server.v1_8_R3.WorldChunkManager.class.getDeclaredField("c") ;
            fieldGenLayer1.setAccessible(true);
            fieldGenLayer2.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            isDirty = ChunkSection.class.getDeclaredField("isDirty");
            isDirty.setAccessible(true);
        } catch (Throwable ignore) {}
    }

    public BukkitQueue18R3(final com.sk89q.worldedit.world.World world) {
        super(world);
        getImpWorld();
    }

    public BukkitQueue18R3(final String world) {
        super(world);
        getImpWorld();
    }

    @Override
    public void saveChunk(Chunk nmsChunk) {
        nmsChunk.f(true); // Modified
        nmsChunk.mustSave = true;
    }

    @Override
    public boolean setMCA(final int mcaX, final int mcaZ, final RegionWrapper allowed, final Runnable whileLocked, final boolean saveChunks, final boolean load) {
        throw new UnsupportedOperationException();
        /*TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                long start = System.currentTimeMillis();
                long last = start;
                synchronized (net.minecraft.server.v1_8_R3.RegionFileCache.class) {
                    World world = getWorld();
                    if (world.getKeepSpawnInMemory()) world.setKeepSpawnInMemory(false);
                    net.minecraft.server.v1_8_R3.ChunkProviderServer provider = nmsWorld.chunkProviderServer;

                    boolean mustSave = false;
                    boolean[][] chunksUnloaded = null;
                    { // Unload chunks
                        Iterator<net.minecraft.server.v1_8_R3.Chunk> iter = provider.a().iterator();
                        while (iter.hasNext()) {
                            net.minecraft.server.v1_8_R3.Chunk chunk = iter.next();
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
                    if (load && !net.minecraft.server.v1_8_R3.RegionFileCache.a.isEmpty()) {
                        Map<File, net.minecraft.server.v1_8_R3.RegionFile> map = net.minecraft.server.v1_8_R3.RegionFileCache.a;
                        Iterator<Map.Entry<File, net.minecraft.server.v1_8_R3.RegionFile>> iter = map.entrySet().iterator();
                        String requiredPath = world.getName() + File.separator + "region";
                        while (iter.hasNext()) {
                            Map.Entry<File, net.minecraft.server.v1_8_R3.RegionFile> entry = iter.next();
                            File file = entry.getKey();
                            int[] regPos = MainUtil.regionNameToCoords(file.getPath());
                            if (regPos[0] == mcaX && regPos[1] == mcaZ && file.getPath().contains(requiredPath)) {
                                if (file.exists()) {
                                    unloadedRegion = file;
                                    net.minecraft.server.v1_8_R3.RegionFile regionFile = entry.getValue();
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
                                                SetQueue.IMP.addTask(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        net.minecraft.server.v1_8_R3.Chunk chunk = provider.getChunkAt(cx, cz, null);
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
                net.minecraft.server.v1_8_R3.ChunkProviderGenerate generator = new net.minecraft.server.v1_8_R3.ChunkProviderGenerate(nmsWorld, seed, false, "");
                Biome bukkitBiome = getAdapter().getBiome(biome.getId());
                net.minecraft.server.v1_8_R3.BiomeBase base = net.minecraft.server.v1_8_R3.BiomeBase.getBiome(biome.getId());
                fieldBiomes.set(generator, new net.minecraft.server.v1_8_R3.BiomeBase[]{base});
                IChunkProvider existingGenerator = nmsWorld.chunkProviderServer.chunkProvider;
                long existingSeed = world.getSeed();
                {
                    if (genLayer == null) genLayer = new MutableGenLayer(seed);
                    genLayer.set(biome.getId());
                    Object existingGenLayer1 = fieldGenLayer1.get(nmsWorld.getWorldChunkManager());
                    Object existingGenLayer2 = fieldGenLayer2.get(nmsWorld.getWorldChunkManager());
                    fieldGenLayer1.set(nmsWorld.getWorldChunkManager(), genLayer);
                    fieldGenLayer2.set(nmsWorld.getWorldChunkManager(), genLayer);

                    fieldSeed.set(nmsWorld.worldData, seed);

                    ReflectionUtils.setFailsafeFieldValue(fieldBiomeCache, this.nmsWorld.getWorldChunkManager(), new net.minecraft.server.v1_8_R3.BiomeCache(this.nmsWorld.getWorldChunkManager()));

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

    @Override
    public int getBiome(net.minecraft.server.v1_8_R3.Chunk chunk, int x, int z) {
        return chunk.getBiomeIndex()[((z & 15) << 4) + (x & 15)];
    }

    @Override
    public net.minecraft.server.v1_8_R3.ChunkSection[] getSections(net.minecraft.server.v1_8_R3.Chunk chunk) {
        return chunk.getSections();
    }

    @Override
    public net.minecraft.server.v1_8_R3.Chunk loadChunk(World world, int x, int z, boolean generate) {
        net.minecraft.server.v1_8_R3.Chunk chunk;
        net.minecraft.server.v1_8_R3.ChunkProviderServer provider = ((org.bukkit.craftbukkit.v1_8_R3.CraftWorld) world).getHandle().chunkProviderServer;
        if (generate) {
            return provider.originalGetChunkAt(x, z);
        } else {
            return provider.loadChunk(x, z);
        }
    }

    @Override
    public net.minecraft.server.v1_8_R3.ChunkSection[] getCachedSections(World world, int cx, int cz) {
        net.minecraft.server.v1_8_R3.Chunk chunk = ((CraftWorld) world).getHandle().chunkProviderServer.getChunkIfLoaded(cx, cz);
        if (chunk != null) {
            return chunk.getSections();
        }
        return null;
    }

    @Override
    public net.minecraft.server.v1_8_R3.Chunk getCachedChunk(World world, int cx, int cz) {
        return ((org.bukkit.craftbukkit.v1_8_R3.CraftWorld) world).getHandle().chunkProviderServer.getChunkIfLoaded(cx, cz);
    }

    @Override
    public net.minecraft.server.v1_8_R3.ChunkSection getCachedSection(net.minecraft.server.v1_8_R3.ChunkSection[] chunkSections, int cy) {
        return chunkSections[cy];
    }

    @Override
    public void setFullbright(ChunkSection[] sections) {
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section != null) {
                byte[] bytes = section.getSkyLightArray().a();
                Arrays.fill(bytes, Byte.MAX_VALUE);
            }
        }
    }

    @Override
    public int getCombinedId4Data(ChunkSection section, int x, int y, int z) {
        char[] ls = section.getIdArray();
        return ls[FaweCache.CACHE_J[y][z & 15][x & 15]];
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, ChunkSection[] sections, Map<?, ?> tilesGeneric, Collection<?>[] entitiesGeneric, Set<UUID> createdEntities, boolean all) throws Exception {
        Map<BlockPosition, TileEntity> tiles = (Map<BlockPosition, TileEntity>) tilesGeneric;
        Collection<Entity>[] entities = (Collection<Entity>[]) entitiesGeneric;
        CharFaweChunk previous = (CharFaweChunk) getFaweChunk(fs.getX(), fs.getZ());
        char[][] idPrevious = previous.getCombinedIdArrays();
        for (int layer = 0; layer < sections.length; layer++) {
            if (fs.getCount(layer) != 0 || all) {
                ChunkSection section = sections[layer];
                if (section != null) {
                    idPrevious[layer] = section.getIdArray().clone();
                    short solid = 0;
                    for (int combined : idPrevious[layer]) {
                        if (combined > 1) {
                            solid++;
                        }
                    }
                    previous.count[layer] = solid;
                    previous.air[layer] = (short) (4096 - solid);
                }
            }
        }
        if (tiles != null) {
            for (Map.Entry<BlockPosition, TileEntity> entry : tiles.entrySet()) {
                TileEntity tile = entry.getValue();
                NBTTagCompound tag = new NBTTagCompound();
                BlockPosition pos = entry.getKey();
                CompoundTag nativeTag = getTag(tile);
                previous.setTile(pos.getX() & 15, pos.getY(), pos.getZ() & 15, nativeTag);
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

    private BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition(0, 0, 0);

    @Override
    public CompoundTag getTileEntity(net.minecraft.server.v1_8_R3.Chunk chunk, int x, int y, int z) {
        Map<BlockPosition, TileEntity> tiles = chunk.getTileEntities();
        pos.c(x, y, z);
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
        net.minecraft.server.v1_8_R3.Chunk chunk = getCachedChunk(getWorld(), x, z);
        if (chunk != null) {
            sendChunk(chunk, bitMask);
        }
    }

    @Override
    public void refreshChunk(FaweChunk fc) {
        net.minecraft.server.v1_8_R3.Chunk chunk = getCachedChunk(getWorld(), fc.getX(), fc.getZ());
        if (chunk != null) {
            sendChunk(chunk, fc.getBitMask());
        }
    }

    public void sendChunk(net.minecraft.server.v1_8_R3.Chunk nmsChunk, int mask) {
        try {
            WorldServer w = (WorldServer) nmsChunk.getWorld();
            PlayerChunkMap chunkMap = w.getPlayerChunkMap();
            int x = nmsChunk.locX;
            int z = nmsChunk.locZ;
            if (!chunkMap.isChunkInUse(x, z)) {
                return;
            }

            LongHashMap<Object> map = (LongHashMap<Object>) fieldChunkMap.get(chunkMap);
            long pair = (long) x + 2147483647L | (long) z + 2147483647L << 32;
            Object playerChunk = map.getEntry(pair);
            Field fieldPlayers = playerChunk.getClass().getDeclaredField("b");
            fieldPlayers.setAccessible(true);
            Collection<EntityPlayer> players = (Collection<EntityPlayer>) fieldPlayers.get(playerChunk);
            if (players.isEmpty()) {
                return;
            }
            boolean empty = false;
            ChunkSection[] sections = nmsChunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                if (sections[i] == null) {
                    sections[i] = emptySection;
                    empty = true;
                }
            }
            // Send chunks
            if (mask == 0 || mask == 65535 && hasEntities(nmsChunk)) {
                PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, 65280);
                for (EntityPlayer player : players) {
                    player.playerConnection.sendPacket(packet);
                }
                mask = 255;
            }
            PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, mask);
            for (EntityPlayer player : players) {
                player.playerConnection.sendPacket(packet);
            }
            // Send tiles
            for (Map.Entry<BlockPosition, TileEntity> entry : nmsChunk.getTileEntities().entrySet()) {
                TileEntity tile = entry.getValue();
                Packet tilePacket = tile.getUpdatePacket();
                for (EntityPlayer player : players) {
                    player.playerConnection.sendPacket(tilePacket);
                }
            }
            if (empty) {
                for (int i = 0; i < sections.length; i++) {
                    if (sections[i] == emptySection) {
                        sections[i] = null;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public boolean hasEntities(net.minecraft.server.v1_8_R3.Chunk nmsChunk) {
        for (int i = 0; i < nmsChunk.entitySlices.length; i++) {
            EntitySlice<Entity> slice = nmsChunk.entitySlices[i];
            if (slice != null && !slice.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeSectionLighting(ChunkSection section, int layer, boolean sky) {
        if (section != null) {
            section.a(new NibbleArray());
            if (sky) {
                section.b(new NibbleArray());
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
        WorldData worlddata = sdm.getWorldData();
        final WorldSettings worldSettings;
        if (worlddata == null) {
            worldSettings = new WorldSettings(creator.seed(), WorldSettings.EnumGamemode.getById(server.getDefaultGameMode().getValue()), generateStructures, hardcore, type);
            worldSettings.setGeneratorSettings(creator.generatorSettings());
            worlddata = new WorldData(worldSettings, name);
        } else {
            worldSettings = null;
        }
        worlddata.checkName(name);
        final WorldServer internal = (WorldServer)new WorldServer(console, sdm, worlddata, dimension, console.methodProfiler, creator.environment(), generator).b();
        startSet(true); // Temporarily allow async chunk load since the world isn't added yet
        if (worldSettings != null) {
            internal.a(worldSettings);
        }
        endSet(true);
        internal.scoreboard = server.getScoreboardManager().getMainScoreboard().getHandle();
        internal.tracker = new EntityTracker(internal);
        internal.addIWorldAccess(new WorldManager(console, internal));
        internal.worldData.setDifficulty(EnumDifficulty.EASY);
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
        pos.c(x, y, z);
        nmsWorld.x(pos);
    }

    private WorldServer nmsWorld;

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
        return new BukkitChunk_1_8(this, x, z);
    }

    @Override
    public void setSkyLight(ChunkSection section, int x, int y, int z, int value) {
        section.a(x & 15, y & 15, z & 15, value);
    }

    @Override
    public void setBlockLight(ChunkSection section, int x, int y, int z, int value) {
        section.b(x & 15, y & 15, z & 15, value);
    }

    @Override
    public int getSkyLight(ChunkSection section, int x, int y, int z) {
        return section.d(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmmittedLight(ChunkSection section, int x, int y, int z) {
        return section.e(x & 15, y & 15, z & 15);
    }

    @Override
    public int getOpacity(ChunkSection section, int x, int y, int z) {
        int combined = getCombinedId4Data(section, x, y, z);
        if (combined == 0) {
            return 0;
        }
        Block block = Block.getById(FaweCache.getId(combined));
        return block.p();
    }

    @Override
    public int getBrightness(ChunkSection section, int x, int y, int z) {
        int combined = getCombinedId4Data(section, x, y, z);
        if (combined == 0) {
            return 0;
        }
        Block block = Block.getById(FaweCache.getId(combined));
        return block.r();
    }

    @Override
    public boolean hasBlock(ChunkSection section, int x, int y, int z) {
        int i = FaweCache.CACHE_J[y & 15][z & 15][x & 15];
        return section.getIdArray()[i] != 0;
    }

    @Override
    public int getOpacityBrightnessPair(ChunkSection section, int x, int y, int z) {
        int combined = getCombinedId4Data(section, x, y, z);
        if (combined == 0) {
            return 0;
        }
        Block block = Block.getById(FaweCache.getId(combined));
        return MathMan.pair16(block.p(), block.r());
    }

    @Override
    public void relightBlock(int x, int y, int z) {
        pos.c(x, y, z);
        nmsWorld.c(EnumSkyBlock.BLOCK, pos);
    }

    @Override
    public void relightSky(int x, int y, int z) {
        pos.c(x, y, z);
        nmsWorld.c(EnumSkyBlock.SKY, pos);
    }
}

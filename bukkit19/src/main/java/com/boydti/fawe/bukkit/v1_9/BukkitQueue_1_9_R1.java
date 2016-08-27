package com.boydti.fawe.bukkit.v1_9;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.BytePair;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.internal.Constants;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_9_R2.Block;
import net.minecraft.server.v1_9_R2.BlockPosition;
import net.minecraft.server.v1_9_R2.Blocks;
import net.minecraft.server.v1_9_R2.ChunkSection;
import net.minecraft.server.v1_9_R2.DataBits;
import net.minecraft.server.v1_9_R2.DataPalette;
import net.minecraft.server.v1_9_R2.DataPaletteBlock;
import net.minecraft.server.v1_9_R2.Entity;
import net.minecraft.server.v1_9_R2.EntityPlayer;
import net.minecraft.server.v1_9_R2.EntityTracker;
import net.minecraft.server.v1_9_R2.EntityTypes;
import net.minecraft.server.v1_9_R2.EnumDifficulty;
import net.minecraft.server.v1_9_R2.EnumSkyBlock;
import net.minecraft.server.v1_9_R2.IBlockData;
import net.minecraft.server.v1_9_R2.IDataManager;
import net.minecraft.server.v1_9_R2.MinecraftServer;
import net.minecraft.server.v1_9_R2.NBTTagCompound;
import net.minecraft.server.v1_9_R2.NibbleArray;
import net.minecraft.server.v1_9_R2.PacketPlayOutMapChunk;
import net.minecraft.server.v1_9_R2.PlayerChunk;
import net.minecraft.server.v1_9_R2.PlayerChunkMap;
import net.minecraft.server.v1_9_R2.ServerNBTManager;
import net.minecraft.server.v1_9_R2.TileEntity;
import net.minecraft.server.v1_9_R2.WorldData;
import net.minecraft.server.v1_9_R2.WorldManager;
import net.minecraft.server.v1_9_R2.WorldServer;
import net.minecraft.server.v1_9_R2.WorldSettings;
import net.minecraft.server.v1_9_R2.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_9_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_9_R2.CraftServer;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.ChunkGenerator;

public class BukkitQueue_1_9_R1 extends BukkitQueue_0<Chunk, ChunkSection[], ChunkSection> {

    private static IBlockData air;
    private static Field fieldBits;

    public BukkitQueue_1_9_R1(final String world) {
        super(world);
        checkVersion("v1_9_R2");
        if (air == null) {
            try {
                Field fieldAir = DataPaletteBlock.class.getDeclaredField("a");
                fieldAir.setAccessible(true);
                air = (IBlockData) fieldAir.get(null);
                fieldBits = DataPaletteBlock.class.getDeclaredField("b");
                fieldBits.setAccessible(true);
                if (adapter == null) {
                    setupAdapter(new FaweAdapter_1_9());
                    Fawe.debug("Using adapter: " + adapter);
                    Fawe.debug("=========================================");
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ChunkSection[] getCachedSections(World world, int cx, int cz) {
        CraftChunk chunk = (CraftChunk) world.getChunkAt(cx, cz);
        return chunk.getHandle().getSections();
    }

    @Override
    public ChunkSection getCachedSection(ChunkSection[] chunkSections, int cy) {
        return chunkSections[cy];
    }

    @Override
    public int getCombinedId4Data(ChunkSection lastSection, int x, int y, int z) {
        DataPaletteBlock dataPalette = lastSection.getBlocks();
        IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        Block block = ibd.getBlock();
        int id = Block.getId(block);
        if (FaweCache.hasData(id)) {
            return (id << 4) + block.toLegacyData(ibd);
        } else {
            return id << 4;
        }
    }

    @Override
    public void refreshChunk(FaweChunk fc) {
        BukkitChunk_1_9 fs = (BukkitChunk_1_9) fc;
        ensureChunkLoaded(fc.getX(), fc.getZ());
        Chunk chunk = fs.getChunk();
        if (!chunk.isLoaded()) {
            return;
        }
        try {
            net.minecraft.server.v1_9_R2.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
            WorldServer w = (WorldServer) nmsChunk.getWorld();
            PlayerChunkMap chunkMap = w.getPlayerChunkMap();
            PlayerChunk playerChunk = chunkMap.getChunk(nmsChunk.locX, nmsChunk.locZ);
            if (playerChunk == null) {
                return;
            }
            if (playerChunk.c.isEmpty()) {
                return;
            }
            // Send chunks
            int mask = fc.getBitMask();
            if (mask == 65535 && hasEntities(nmsChunk)) {
                PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, 65280);
                for (EntityPlayer player : playerChunk.c) {
                    player.playerConnection.sendPacket(packet);
                }
                mask = 255;
            }
            PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, mask);
            for (EntityPlayer player : playerChunk.c) {
                player.playerConnection.sendPacket(packet);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean hasEntities(net.minecraft.server.v1_9_R2.Chunk nmsChunk) {
        for (int i = 0; i < nmsChunk.entitySlices.length; i++) {
            List<Entity> slice = nmsChunk.entitySlices[i];
            if (slice != null && !slice.isEmpty()) {
                return true;
            }
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
        final IDataManager sdm = new ServerNBTManager(server.getWorldContainer(), name, true, server.getHandle().getServer().getDataConverterManager());
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
        internal.a(worldSettings);
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
    public void setFullbright(ChunkSection[] sections) {
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section != null) {
                byte[] bytes = section.getSkyLightArray().asBytes();
                Arrays.fill(bytes, Byte.MAX_VALUE);
            }
        }
    }

    @Override
    public boolean removeLighting(ChunkSection[] sections, RelightMode mode, boolean sky) {
        if (mode == RelightMode.ALL) {
            for (int i = 0; i < sections.length; i++) {
                ChunkSection section = sections[i];
                if (section != null) {
                    section.a(new NibbleArray());
                    if (sky) {
                        section.b(new NibbleArray());
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void relight(int x, int y, int z) {
        pos.c(x, y, z);
        nmsWorld.w(pos);
    }

    private WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        World world = super.getImpWorld();
        this.nmsWorld = ((CraftWorld) world).getHandle();
        return super.getImpWorld();
    }

    public boolean isSurrounded(final char[][] sections, final int x, final int y, final int z) {
        return this.isSolid(this.getId(sections, x, y + 1, z))
                && this.isSolid(this.getId(sections, x + 1, y - 1, z))
                && this.isSolid(this.getId(sections, x - 1, y, z))
                && this.isSolid(this.getId(sections, x, y, z + 1))
                && this.isSolid(this.getId(sections, x, y, z - 1));
    }

    public boolean isSolid(final int id) {
        return !FaweCache.isTransparent(id);
    }

    public int getId(final char[][] sections, final int x, final int y, final int z) {
        if ((x < 0) || (x > 15) || (z < 0) || (z > 15)) {
            return 1;
        }
        if ((y < 0) || (y > 255)) {
            return 1;
        }
        final int i = FaweCache.CACHE_I[y][z][x];
        final char[] section = sections[i];
        if (section == null) {
            return 0;
        }
        final int j = FaweCache.CACHE_J[y][z][x];
        return section[j] >> 4;
    }

    public void setCount(int tickingBlockCount, int nonEmptyBlockCount, ChunkSection section) throws NoSuchFieldException, IllegalAccessException {
        Class<? extends ChunkSection> clazz = section.getClass();
        Field fieldTickingBlockCount = clazz.getDeclaredField("tickingBlockCount");
        Field fieldNonEmptyBlockCount = clazz.getDeclaredField("nonEmptyBlockCount");
        fieldTickingBlockCount.setAccessible(true);
        fieldNonEmptyBlockCount.setAccessible(true);
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    public void setPalette(ChunkSection section, DataPaletteBlock palette) throws NoSuchFieldException, IllegalAccessException {
        Field fieldSection = ChunkSection.class.getDeclaredField("blockIds");
        fieldSection.setAccessible(true);
        fieldSection.set(section, palette);
    }

    public ChunkSection newChunkSection(int y2, boolean flag, char[] array) {
        try {
            if (array == null) {
                return new ChunkSection(y2, flag);
            } else {
                return new ChunkSection(y2, flag, array);
            }
        } catch (Throwable e) {
            try {
                if (array == null) {
                    Constructor<ChunkSection> constructor = ChunkSection.class.getDeclaredConstructor(int.class, boolean.class, IBlockData[].class);
                    return constructor.newInstance(y2, flag, (IBlockData[]) null);
                } else {
                    Constructor<ChunkSection> constructor = ChunkSection.class.getDeclaredConstructor(int.class, boolean.class, char[].class, IBlockData[].class);
                    return constructor.newInstance(y2, flag, array, (IBlockData[]) null);
                }
            } catch (Throwable e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, ChunkSection[] sections, Map<?, ?> tilesGeneric, Collection<?>[] entitiesGeneric, Set<UUID> createdEntities, boolean all) throws Exception {
        Map<BlockPosition, TileEntity> tiles = (Map<BlockPosition, TileEntity>) tilesGeneric;
        Collection<Entity>[] entities = (Collection<Entity>[]) entitiesGeneric;
        CharFaweChunk previous = (CharFaweChunk) getFaweChunk(fs.getX(), fs.getZ());
        // Copy blocks
        char[][] idPrevious = new char[16][];
        for (int layer = 0; layer < sections.length; layer++) {
            if (fs.getCount(layer) != 0 || all) {
                ChunkSection section = sections[layer];
                if (section != null) {
                    short solid = 0;
                    char[] previousLayer = idPrevious[layer] = new char[4096];
                    DataPaletteBlock blocks = section.getBlocks();
                    for (int j = 0; j < 4096; j++) {
                        int x = FaweCache.CACHE_X[0][j];
                        int y = FaweCache.CACHE_Y[0][j];
                        int z = FaweCache.CACHE_Z[0][j];
                        IBlockData ibd = blocks.a(x, y, z);
                        Block block = ibd.getBlock();
                        int combined = Block.getId(block);
                        if (FaweCache.hasData(combined)) {
                            combined = (combined << 4) + block.toLegacyData(ibd);
                        } else {
                            combined = combined << 4;
                        }
                        if (combined > 1) {
                            solid++;
                        }
                        previousLayer[j] = (char) combined;
                    }
                    previous.count[layer] = solid;
                    previous.air[layer] = (short) (4096 - solid);
                }
            }
        }
        previous.ids = idPrevious;
        // Copy tiles
        if (tiles != null) {
            for (Map.Entry<BlockPosition, TileEntity> entry : tiles.entrySet()) {
                TileEntity tile = entry.getValue();
                NBTTagCompound tag = new NBTTagCompound();
                BlockPosition pos = entry.getKey();
                CompoundTag nativeTag = getTag(tile);
                previous.setTile(pos.getX() & 15, pos.getY(), pos.getZ() & 15, nativeTag);
            }
        }
        // Copy entities
        if (entities != null) {
            for (Collection<Entity> entityList : entities) {
                for (Entity ent : entityList) {
                    if (ent instanceof EntityPlayer || (!createdEntities.isEmpty() && createdEntities.contains(ent.getUniqueID()))) {
                        continue;
                    }
                    int x = ((int) Math.round(ent.locX) & 15);
                    int z = ((int) Math.round(ent.locZ) & 15);
                    int y = (int) Math.round(ent.locY);
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
                            CompoundTag nativeTag = (CompoundTag) methodToNative.invoke(adapter, tag);
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

    private BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition(0, 0, 0);

    @Override
    public CompoundTag getTileEntity(Chunk chunk, int x, int y, int z) {
        Map<BlockPosition, TileEntity> tiles = ((CraftChunk) chunk).getHandle().getTileEntities();
        pos.c(x, y, z);
        TileEntity tile = tiles.get(pos);
        return tile != null ? getTag(tile) : null;
    }

    public CompoundTag getTag(TileEntity tile) {
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.save(tag); // readTagIntoEntity
            return (CompoundTag) methodToNative.invoke(adapter, tag);
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    @Override
    public Chunk getChunk(World world, int x, int z) {
        return world.getChunkAt(x, z);
    }

    @Override
    public boolean setComponents(final FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        final BukkitChunk_1_9 fs = (BukkitChunk_1_9) fc;
        final Chunk chunk = (Chunk) fs.getChunk();
        final World world = chunk.getWorld();
        chunk.load(true);
        try {
            final boolean flag = world.getEnvironment() == Environment.NORMAL;
            net.minecraft.server.v1_9_R2.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
            nmsChunk.f(true); // Modified
            nmsChunk.mustSave = true;
            net.minecraft.server.v1_9_R2.World nmsWorld = nmsChunk.world;
            ChunkSection[] sections = nmsChunk.getSections();
            Class<? extends net.minecraft.server.v1_9_R2.Chunk> clazzChunk = nmsChunk.getClass();
            final Field ef = clazzChunk.getDeclaredField("entitySlices");
            final Collection<Entity>[] entities = (Collection<Entity>[]) ef.get(nmsChunk);
            Map<BlockPosition, TileEntity> tiles = nmsChunk.getTileEntities();
            // Remove entities
            for (int i = 0; i < entities.length; i++) {
                int count = fs.getCount(i);
                if (count == 0) {
                    continue;
                } else if (count >= 4096) {
                    entities[i].clear();
                } else {
                    char[] array = fs.getIdArray(i);
                    Collection<Entity> ents = new ArrayList<>(entities[i]);
                    for (Entity entity : ents) {
                        if (entity instanceof EntityPlayer) {
                            continue;
                        }
                        int x = ((int) Math.round(entity.locX) & 15);
                        int z = ((int) Math.round(entity.locZ) & 15);
                        int y = (int) Math.round(entity.locY);
                        if (array == null || y < 0 || y > 255) {
                            continue;
                        }
                        if (y < 0 || y > 255 || array[FaweCache.CACHE_J[y][z][x]] != 0) {
                            nmsWorld.removeEntity(entity);
                        }
                    }
                }
            }
            HashSet<UUID> entsToRemove = fs.getEntityRemoves();
            if (entsToRemove.size() > 0) {
                for (int i = 0; i < entities.length; i++) {
                    Collection<Entity> ents = new ArrayList<>(entities[i]);
                    for (Entity entity : ents) {
                        if (entsToRemove.contains(entity.getUniqueID())) {
                            nmsWorld.removeEntity(entity);
                        }
                    }
                }
            }
            // Set entities
            Set<UUID> createdEntities = new HashSet<>();
            Set<CompoundTag> entitiesToSpawn = fs.getEntities();
            for (CompoundTag nativeTag : entitiesToSpawn) {
                Map<String, Tag> entityTagMap = ReflectionUtils.getMap(nativeTag.getValue());
                StringTag idTag = (StringTag) entityTagMap.get("Id");
                ListTag posTag = (ListTag) entityTagMap.get("Pos");
                ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                if (idTag == null || posTag == null || rotTag == null) {
                    Fawe.debug("Unknown entity tag: " + nativeTag);
                    continue;
                }
                double x = posTag.getDouble(0);
                double y = posTag.getDouble(1);
                double z = posTag.getDouble(2);
                float yaw = rotTag.getFloat(0);
                float pitch = rotTag.getFloat(1);
                String id = idTag.getValue();
                Entity entity = EntityTypes.createEntityByName(id, nmsWorld);
                if (entity != null) {
                    UUID uuid = entity.getUniqueID();
                    entityTagMap.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                    entityTagMap.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                    if (nativeTag != null) {
                        NBTTagCompound tag = (NBTTagCompound) methodFromNative.invoke(adapter, nativeTag);
                        for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                            tag.remove(name);
                        }
                        entity.f(tag);
                    }
                    entity.setLocation(x, y, z, yaw, pitch);
                    nmsWorld.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                    createdEntities.add(entity.getUniqueID());
                }
            }
            // Change task?
            if (changeTask != null) {
                CharFaweChunk previous = getPrevious(fs, sections, tiles, entities, createdEntities, false);
                changeTask.run(previous);
            }
            // Trim tiles
            Iterator<Map.Entry<BlockPosition, TileEntity>> iterator = tiles.entrySet().iterator();
            HashMap<BlockPosition, TileEntity> toRemove = null;
            while (iterator.hasNext()) {
                Map.Entry<BlockPosition, TileEntity> tile = iterator.next();
                BlockPosition pos = tile.getKey();
                int lx = pos.getX() & 15;
                int ly = pos.getY();
                int lz = pos.getZ() & 15;
                int j = FaweCache.CACHE_I[ly][lz][lx];
                char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int k = FaweCache.CACHE_J[ly][lz][lx];
                if (array[k] != 0) {
                    if (toRemove == null) {
                        toRemove = new HashMap<>();
                    }
                    toRemove.put(tile.getKey(), tile.getValue());
                }
            }
            if (toRemove != null) {
                for (Entry<BlockPosition, TileEntity> entry : toRemove.entrySet()) {
                    BlockPosition bp = entry.getKey();
                    TileEntity tile = entry.getValue();
                    tiles.remove(bp);
                    tile.y();
                    nmsWorld.s(bp);
                    tile.invalidateBlockCache();
                }

            }
            // Set blocks
            for (int j = 0; j < sections.length; j++) {
                int count = fs.getCount(j);
                if (count == 0) {
                    continue;
                }
                final char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                ChunkSection section = sections[j];
                if (section == null) {
                    if (fs.sectionPalettes != null && fs.sectionPalettes[j] != null) {
                        section = sections[j] = newChunkSection(j << 4, flag, null);
                        setPalette(section, fs.sectionPalettes[j]);
                        setCount(0, count - fs.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = newChunkSection(j << 4, flag, array);
                    }
                    continue;
                } else if (count >= 4096) {
                    if (fs.sectionPalettes != null && fs.sectionPalettes[j] != null) {
                        setPalette(section, fs.sectionPalettes[j]);
                        setCount(0, count - fs.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = newChunkSection(j << 4, flag, array);
                    }
                    continue;
                }
                DataPaletteBlock nibble = section.getBlocks();
                Field fieldBits = nibble.getClass().getDeclaredField("b");
                fieldBits.setAccessible(true);
                DataBits bits = (DataBits) fieldBits.get(nibble);

                Field fieldPalette = nibble.getClass().getDeclaredField("c");
                fieldPalette.setAccessible(true);
                DataPalette palette = (DataPalette) fieldPalette.get(nibble);
                int nonEmptyBlockCount = 0;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            char combinedId = array[FaweCache.CACHE_J[y][z][x]];
                            switch (combinedId) {
                                case 0:
                                    IBlockData existing = nibble.a(x, y, z);
                                    if (existing != air) {
                                        nonEmptyBlockCount++;
                                    }
                                    continue;
                                case 1:
                                    nibble.setBlock(x, y, z, Blocks.AIR.getBlockData());
                                    continue;
                                default:
                                    nonEmptyBlockCount++;
                                    nibble.setBlock(x, y, z, Block.getById(combinedId >> 4).fromLegacyData(combinedId & 0xF));
                            }
                        }
                    }
                }
                setCount(0, nonEmptyBlockCount, section);
            }
            // Set biomes
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
                        nmsChunk.getBiomeIndex()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome;
                    }
                }
            }
            // Set tiles
            Map<BytePair, CompoundTag> tilesToSpawn = fs.getTiles();
            int bx = fs.getX() << 4;
            int bz = fs.getZ() << 4;

            for (Map.Entry<BytePair, CompoundTag> entry : tilesToSpawn.entrySet()) {
                CompoundTag nativeTag = entry.getValue();
                BytePair pair = entry.getKey();
                BlockPosition pos = new BlockPosition(MathMan.unpair16x((byte) pair.get0()) + bx, pair.get1() & 0xFF, MathMan.unpair16y((byte) pair.get0()) + bz); // Set pos
                TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) methodFromNative.invoke(adapter, nativeTag);
                    tileEntity.a(tag); // ReadTagIntoTile
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        final int[][] biomes = fs.getBiomeArray();
        final Biome[] values = Biome.values();
        if (biomes != null) {
            for (int x = 0; x < 16; x++) {
                final int[] array = biomes[x];
                if (array == null) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    final int biome = array[z];
                    if (biome == 0) {
                        continue;
                    }
                    chunk.getBlock(x, 0, z).setBiome(values[biome]);
                }
            }
        }
        return true;
    }

    @Deprecated
    public boolean unloadChunk(final String world, final Chunk chunk) {
        net.minecraft.server.v1_9_R2.Chunk c = ((CraftChunk) chunk).getHandle();
        c.mustSave = false;
        if (chunk.isLoaded()) {
            chunk.unload(false, false);
        }
        return true;
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return new BukkitChunk_1_9(this, x, z);
    }

    @Override
    public void setSkyLight(ChunkSection section, int x, int y, int z, int value) {
        section.getSkyLightArray().a(x & 15, y & 15, z & 15, value);
    }

    @Override
    public void setBlockLight(ChunkSection section, int x, int y, int z, int value) {
        section.getEmittedLightArray().a(x & 15, y & 15, z & 15, value);
    }

    @Override
    public int getSkyLight(ChunkSection section, int x, int y, int z) {
        return section.b(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmmittedLight(ChunkSection section, int x, int y, int z) {
        return section.c(x & 15, y & 15, z & 15);
    }

    @Override
    public int getOpacity(ChunkSection section, int x, int y, int z) {
        DataPaletteBlock dataPalette = section.getBlocks();
        IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        return ibd.c();
    }

    @Override
    public int getBrightness(ChunkSection section, int x, int y, int z) {
        DataPaletteBlock dataPalette = section.getBlocks();
        IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        return ibd.d();
    }

    @Override
    public int getOpacityBrightnessPair(ChunkSection section, int x, int y, int z) {
        DataPaletteBlock dataPalette = section.getBlocks();
        IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        return MathMan.pair16(ibd.c(), ibd.d());
    }

    private DataBits lastBits;
    private DataPaletteBlock lastBlocks;

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

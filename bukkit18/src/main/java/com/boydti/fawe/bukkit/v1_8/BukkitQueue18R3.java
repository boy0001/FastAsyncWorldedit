package com.boydti.fawe.bukkit.v1_8;

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
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.internal.Constants;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTracker;
import net.minecraft.server.v1_8_R3.EntityTypes;
import net.minecraft.server.v1_8_R3.EnumDifficulty;
import net.minecraft.server.v1_8_R3.EnumSkyBlock;
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
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.ChunkGenerator;

public class BukkitQueue18R3 extends BukkitQueue_0<Chunk, ChunkSection[], ChunkSection> {

    public BukkitQueue18R3(final String world) {
        super(world);
        checkVersion("v1_8_R3");
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return getWorld().isChunkLoaded(x, z);
    }

    public World getWorld(String world) {
        return Bukkit.getWorld(world);
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z) {
        return world.regenerateChunk(x, z);
    }

    @Override
    public boolean loadChunk(World world, int x, int z, boolean generate) {
        return getCachedSections(world, x, z) != null;
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
    public ChunkSection[] getCachedSections(World world, int x, int z) {
        Chunk chunk = world.getChunkAt(x, z);
        if (chunk == null) {
            return null;
        }
        if (!chunk.isLoaded()) {
            chunk.load(true);
        }
        return ((CraftChunk) chunk).getHandle().getSections();
    }

    @Override
    public int getCombinedId4Data(ChunkSection section, int x, int y, int z) {
        char[] ls = section.getIdArray();
        return ls[FaweCache.CACHE_J[y][x & 15][z & 15]];
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public ChunkSection getCachedSection(ChunkSection[] chunkSections, int cy) {
        return chunkSections[cy];
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, ChunkSection[] sections, Map<?, ?> tilesGeneric, Collection<?>[] entitiesGeneric, Set<UUID> createdEntities, boolean all) throws Exception {
        Map<BlockPosition, TileEntity> tiles = (Map<BlockPosition, TileEntity>) tilesGeneric;
        Collection<Entity>[] entities = (Collection<Entity>[]) entitiesGeneric;
        CharFaweChunk previous = (CharFaweChunk) getFaweChunk(fs.getX(), fs.getZ());
        char[][] idPrevious = new char[16][];
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
        previous.ids = idPrevious;
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
                    int x = ((int) Math.round(ent.locX) & 15);
                    int z = ((int) Math.round(ent.locZ) & 15);
                    int y = (int) Math.round(ent.locY);
                    int i = FaweCache.CACHE_I[y][x][z];
                    char[] array = fs.getIdArray(i);
                    if (array == null) {
                        continue;
                    }
                    int j = FaweCache.CACHE_J[y][x][z];
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

    public CompoundTag getTag(TileEntity tile) {
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.b(tag); // readTagIntoEntity
            return (CompoundTag) methodToNative.invoke(adapter, tag);
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    private BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition(0, 0, 0);

    @Override
    public CompoundTag getTileEntity(Chunk chunk, int x, int y, int z) {
        Map<BlockPosition, TileEntity> tiles = ((CraftChunk) chunk).getHandle().getTileEntities();
        pos.c(x, y, z);
        TileEntity tile = tiles.get(pos);
        return tile != null ? getTag(tile) : null;
    }


    @Override
    public Chunk getChunk(World world, int x, int z) {
        return world.getChunkAt(x, z);
    }

    @Override
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        CharFaweChunk<Chunk> fs = (CharFaweChunk<Chunk>) fc;
        CraftChunk chunk = (CraftChunk) fs.getChunk();
        net.minecraft.server.v1_8_R3.Chunk nmsChunk = chunk.getHandle();
        nmsChunk.f(true); // Modified
        nmsChunk.mustSave = true;
        net.minecraft.server.v1_8_R3.World nmsWorld = nmsChunk.getWorld();
        try {
            final boolean flag = getWorld().getEnvironment() == World.Environment.NORMAL;
            // Sections
            ChunkSection[] sections = nmsChunk.getSections();
            Map<BlockPosition, TileEntity> tiles = nmsChunk.getTileEntities();
            Collection<net.minecraft.server.v1_8_R3.Entity>[] entities = nmsChunk.getEntitySlices();

            // Remove entities
            for (int i = 0; i < 16; i++) {
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
                        if (array == null) {
                            continue;
                        }
                        if (y < 0 || y > 255 || array[FaweCache.CACHE_J[y][x][z]] != 0) {
                            nmsWorld.removeEntity(entity);
                        }
                    }
                }
            }
            // Set entities
            Set<UUID> createdEntities = new HashSet<>();
            Set<CompoundTag> entitiesToSpawn = fs.getEntities();
            for (CompoundTag nativeTag : entitiesToSpawn) {
                Map<String, Tag> entityTagMap = nativeTag.getValue();
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
                    if (nativeTag != null) {
                        NBTTagCompound tag = (NBTTagCompound)methodFromNative.invoke(adapter, nativeTag);
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
            // Run change task if applicable
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
                int j = FaweCache.CACHE_I[ly][lx][lz];
                char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int k = FaweCache.CACHE_J[ly][lx][lz];
                if (array[k] != 0) {
                    if (toRemove == null) {
                        toRemove = new HashMap<>();
                    }
                    toRemove.put(tile.getKey(), tile.getValue());
                }
            }
            if (toRemove != null) {
                for (Map.Entry<BlockPosition, TileEntity> entry : toRemove.entrySet()) {
                    BlockPosition bp = entry.getKey();
                    TileEntity tile = entry.getValue();
                    tiles.remove(bp);
                    tile.y();
                    nmsWorld.t(bp);
                    tile.E();
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
            // Set blocks
            for (int j = 0; j < sections.length; j++) {
                if (fs.getCount(j) == 0) {
                    continue;
                }
                char[] newArray = fs.getIdArray(j);
                if (newArray == null) {
                    continue;
                }
                ChunkSection section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    section = new ChunkSection(j << 4, flag, newArray);
                    sections[j] = section;
                    continue;
                }
                char[] currentArray = section.getIdArray();
                int solid = 0;
                for (int k = 0; k < newArray.length; k++) {
                    char n = newArray[k];
                    switch (n) {
                        case 0:
                            continue;
                        case 1:
                            if (currentArray[k] > 1) {
                                solid++;
                                currentArray[k] = 0;
                            }
                            continue;
                        default:
                            solid++;
                            currentArray[k] = n;
                            continue;
                    }
                }
                setCount(0, solid, section);
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
                BlockPosition pos = new BlockPosition(MathMan.unpair16x(pair.pair[0]) + bx, pair.pair[1] & 0xFF, MathMan.unpair16y(pair.pair[0]) + bz); // Set pos
                TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) methodFromNative.invoke(adapter, nativeTag);
                    tileEntity.a(tag); // ReadTagIntoTile
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return true;
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

    @Override
    public void refreshChunk(FaweChunk fc) {
        BukkitChunk_1_8 fs = (BukkitChunk_1_8) fc;
        ensureChunkLoaded(fc.getX(), fc.getZ());
        Chunk chunk = fs.getChunk();
        if (!chunk.isLoaded()) {
            return;
        }
        try {
            net.minecraft.server.v1_8_R3.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
            WorldServer w = (WorldServer) nmsChunk.getWorld();
            PlayerChunkMap chunkMap = w.getPlayerChunkMap();
            int x = nmsChunk.locX;
            int z = nmsChunk.locZ;
            if (!chunkMap.isChunkInUse(x, z)) {
                return;
            }
            Field fieldChunkMap = chunkMap.getClass().getDeclaredField("d");
            fieldChunkMap.setAccessible(true);
            LongHashMap<Object> map = (LongHashMap<Object>) fieldChunkMap.get(chunkMap);
            long pair = (long) x + 2147483647L | (long) z + 2147483647L << 32;
            Object playerChunk = map.getEntry(pair);
            Field fieldPlayers = playerChunk.getClass().getDeclaredField("b");
            fieldPlayers.setAccessible(true);
            Collection<EntityPlayer> players = (Collection<EntityPlayer>) fieldPlayers.get(playerChunk);
            if (players.isEmpty()) {
                return;
            }
            // Send chunks
            int mask = fc.getBitMask();
            if (mask == 65535 && hasEntities(nmsChunk)) {
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
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    public boolean hasEntities(net.minecraft.server.v1_8_R3.Chunk nmsChunk) {
        for (int i = 0; i < nmsChunk.entitySlices.length; i++) {
            List<Entity> slice = nmsChunk.entitySlices[i];
            if (slice != null && !slice.isEmpty()) {
                return true;
            }
        }
        return false;
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
    public void relight(int x, int y, int z) {
        pos.c(x, y, z);
        nmsWorld.x(pos);
    }

    private WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        World world = super.getImpWorld();
        this.nmsWorld = ((CraftWorld) world).getHandle();
        return super.getImpWorld();
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
        int i = FaweCache.CACHE_J[y & 15][x & 15][z & 15];
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

package com.boydti.fawe.bukkit.v1_7;

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
import net.minecraft.server.v1_7_R4.Block;
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
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.v1_7_R4.CraftChunk;
import org.bukkit.craftbukkit.v1_7_R4.CraftServer;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.ChunkGenerator;

public class BukkitQueue17 extends BukkitQueue_0<Chunk, ChunkSection[], ChunkSection> {

    public BukkitQueue17(final String world) {
        super(world);
        checkVersion("v1_7_R4");
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
                byte[] bytes = section.getSkyLightArray().a;
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
    public int getCombinedId4Data(ChunkSection ls, int x, int y, int z) {
        byte[] ids = ls.getIdArray();
        NibbleArray datasNibble = ls.getDataArray();
        int i = FaweCache.CACHE_J[y & 15][z & 15][x & 15];
        int combined = (ids[i] << 4) + (datasNibble == null ? 0 : datasNibble.a(x & 15, y & 15, z & 15));
        return combined;
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
        Map<ChunkPosition, TileEntity> tiles = (Map<ChunkPosition, TileEntity>) tilesGeneric;
        Collection<Entity>[] entities = (Collection<Entity>[]) entitiesGeneric;
        CharFaweChunk previous = (CharFaweChunk) getFaweChunk(fs.getX(), fs.getZ());
        char[][] idPrevious = new char[16][];
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
        previous.ids = idPrevious;
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

    @Override
    public CompoundTag getTileEntity(Chunk chunk, int x, int y, int z) {
        Map<ChunkPosition, TileEntity> tiles = ((CraftChunk) chunk).getHandle().tileEntities;
        ChunkPosition pos = new ChunkPosition(x, y, z);
        TileEntity tile = tiles.get(pos);
        return tile != null ? getTag(tile) : null;
    }


    @Override
    public Chunk getChunk(World world, int x, int z) {
        return world.getChunkAt(x, z);
    }

    @Override
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        BukkitChunk_1_7 fs = (BukkitChunk_1_7) fc;
        CraftChunk chunk = (CraftChunk) fs.getChunk();
        net.minecraft.server.v1_7_R4.Chunk nmsChunk = chunk.getHandle();
        nmsChunk.e(); // Modified
        nmsChunk.mustSave = true;
        net.minecraft.server.v1_7_R4.World nmsWorld = nmsChunk.world;
        try {
            final boolean flag = getWorld().getEnvironment() == World.Environment.NORMAL;
            // Sections
            ChunkSection[] sections = nmsChunk.getSections();
            Map<ChunkPosition, TileEntity> tiles = nmsChunk.tileEntities;
            Collection<net.minecraft.server.v1_7_R4.Entity>[] entities = nmsChunk.entitySlices;

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
                        if (y < 0 || y > 255 || array[FaweCache.CACHE_J[y][z][x]] != 0) {
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
            Iterator<Map.Entry<ChunkPosition, TileEntity>> iterator = tiles.entrySet().iterator();
            HashMap<ChunkPosition, TileEntity> toRemove = null;
            while (iterator.hasNext()) {
                Map.Entry<ChunkPosition, TileEntity> tile = iterator.next();
                ChunkPosition pos = tile.getKey();
                int lx = pos.x & 15;
                int ly = pos.y;
                int lz = pos.z & 15;
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
                for (Map.Entry<ChunkPosition, TileEntity> entry : toRemove.entrySet()) {
                    ChunkPosition bp = entry.getKey();
                    TileEntity tile = entry.getValue();
                    tiles.remove(bp);
                    tile.s();
                    nmsWorld.p(bp.x, bp.y, bp.z);
                    tile.u();
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
                byte[] newIdArray = fs.getByteIdArray(j);
                if (newIdArray == null) {
                    continue;
                }
                NibbleArray newDataArray = fs.getDataArray(j);
                ChunkSection section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    sections[j] = section = new ChunkSection(j << 4, flag);
                    section.setIdArray(newIdArray);
                    section.setDataArray(newDataArray);
                    continue;
                }
                byte[] currentIdArray = section.getIdArray();
                NibbleArray currentDataArray = section.getDataArray();
                boolean data = currentDataArray != null;
                if (!data) {
                    section.setDataArray(newDataArray);
                }
                boolean fill = true;
                int solid = 0;
                for (int k = 0; k < newIdArray.length; k++) {
                    byte n = newIdArray[k];
                    switch (n) {
                        case 0:
                            fill = false;
                            continue;
                        case -1:
                            fill = false;
                            if (currentIdArray[k] != 0) {
                                solid++;
                            }
                            currentIdArray[k] = 0;
                            continue;
                        default:
                            solid++;
                            currentIdArray[k] = n;
                            if (data) {
                                int x = FaweCache.CACHE_X[0][k];
                                int y = FaweCache.CACHE_Y[0][k];
                                int z = FaweCache.CACHE_Z[0][k];
                                int newData = newDataArray == null ? 0 : newDataArray.a(x, y, z);
                                int currentData = currentDataArray == null ? 0 : currentDataArray.a(x, y, z);
                                if (newData != currentData) {
                                    currentDataArray.a(x, y, z, newData);
                                }
                            }
                            continue;
                    }
                }
                setCount(0, solid, section);
                if (fill) {
                    fs.setCount(j, Short.MAX_VALUE);
                }
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
                        nmsChunk.m()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome; // Biome array
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
                TileEntity tileEntity = nmsWorld.getTileEntity(MathMan.unpair16x((byte) pair.get0()) + bx, pair.get1() & 0xFF, MathMan.unpair16y((byte) pair.get0()) + bz);
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
        BukkitChunk_1_7 fs = (BukkitChunk_1_7) fc;
        ensureChunkLoaded(fc.getX(), fc.getZ());
        Chunk chunk = fs.getChunk();
        if (!chunk.isLoaded()) {
            return;
        }
        try {
            net.minecraft.server.v1_7_R4.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
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
            int mask = fc.getBitMask();
            if (mask == 65535 && hasEntities(nmsChunk)) {
                PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, 65280, 25);
                for (EntityPlayer player : players) {
                    player.playerConnection.sendPacket(packet);
                }
                mask = 255;
            }
            PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, mask, 25);
            for (EntityPlayer player : players) {
                player.playerConnection.sendPacket(packet);
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
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
    public boolean removeLighting(ChunkSection[] sections, RelightMode mode, boolean sky) {
        if (mode == RelightMode.ALL) {
            for (int i = 0; i < sections.length; i++) {
                ChunkSection section = sections[i];
                if (section != null) {
                    section.setEmittedLightArray(null);
                    if (sky) {
                        section.setSkyLightArray(null);
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
        final WorldSettings worldSettings = new WorldSettings(creator.seed(), EnumGamemode.getById(server.getDefaultGameMode().getValue()), generateStructures, hardcore, type);
        startSet(true);
        final WorldServer internal = new WorldServer(console, sdm, name, dimension, worldSettings, console.methodProfiler, creator.environment(), generator);
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

    private WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        World world = super.getImpWorld();
        this.nmsWorld = ((CraftWorld) world).getHandle();
        return super.getImpWorld();
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
        return section.getTypeId(x, y, z).k();
    }

    @Override
    public int getBrightness(ChunkSection section, int x, int y, int z) {
        return section.getTypeId(x, y, z).m();
    }

    @Override
    public boolean hasBlock(ChunkSection section, int x, int y, int z) {
        int i = FaweCache.CACHE_J[y & 15][z & 15][x & 15];
        return section.getIdArray()[i] != 0;
    }

    @Override
    public int getOpacityBrightnessPair(ChunkSection section, int x, int y, int z) {
        Block block = section.getTypeId(x, y, z);
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

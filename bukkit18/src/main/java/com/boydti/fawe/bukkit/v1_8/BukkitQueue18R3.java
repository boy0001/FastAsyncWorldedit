package com.boydti.fawe.bukkit.v1_8;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.BytePair;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.PseudoRandom;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTracker;
import net.minecraft.server.v1_8_R3.EntityTrackerEntry;
import net.minecraft.server.v1_8_R3.EntityTypes;
import net.minecraft.server.v1_8_R3.LongHashMap;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NibbleArray;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PlayerChunkMap;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class BukkitQueue18R3 extends BukkitQueue_0<Chunk, ChunkSection[], char[]> {

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
    public int getCombinedId4Data(char[] ls, int x, int y, int z) {
        return ls[FaweCache.CACHE_J[y][x & 15][z & 15]];
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public char[] getCachedSection(ChunkSection[] chunkSections, int cy) {
        ChunkSection section = chunkSections[cy];
        return section == null ? null : section.getIdArray();
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
            Set<Map.Entry<BlockPosition, TileEntity>> entryset = tiles.entrySet();
            Iterator<Map.Entry<BlockPosition, TileEntity>> iterator = entryset.iterator();
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
                    tile.getValue().E();
                    iterator.remove();
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
        sendChunk(fc, null);
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
    public void refreshChunk(World world, Chunk chunk) {
        if (!chunk.isLoaded()) {
            return;
        }
        try {
            net.minecraft.server.v1_8_R3.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
            ChunkCoordIntPair pos = nmsChunk.j(); // getPosition()
            WorldServer w = (WorldServer) nmsChunk.getWorld();
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
            LongHashMap<Object> map = (LongHashMap<Object>) fieldChunkMap.get(chunkMap);
            long pair = (long) x + 2147483647L | (long) z + 2147483647L << 32;
            Object playerChunk = map.getEntry(pair);
            Field fieldPlayers = playerChunk.getClass().getDeclaredField("b");
            fieldPlayers.setAccessible(true);
            final HashSet<EntityPlayer> players = new HashSet<>((Collection<EntityPlayer>)fieldPlayers.get(playerChunk));
            if (players.size() == 0) {
                return;
            }
            HashSet<EntityTrackerEntry> entities = new HashSet<>();
            List<Entity>[] entitieSlices = nmsChunk.getEntitySlices();
            for (List<Entity> slice : entitieSlices) {
                if (slice == null) {
                    continue;
                }
                for (Entity ent : slice) {
                    EntityTrackerEntry entry = tracker.trackedEntities.get(ent.getId());
                    if (entry == null) {
                        continue;
                    }
                    entities.add(entry);
                    PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(ent.getId());
                    for (EntityPlayer player : players) {
                        player.playerConnection.sendPacket(packet);
                    }
                }
            }
            // Send chunks
            PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, 65535);
            for (EntityPlayer player : players) {
                player.playerConnection.sendPacket(packet);
            }
            // send ents
            for (final EntityTrackerEntry entry : entities) {
                try {
                    TaskManager.IMP.later(new Runnable() {
                        @Override
                        public void run() {
                            for (EntityPlayer player : players) {
                                boolean result = entry.trackedPlayers.remove(player);
                                if (result && entry.tracker != player) {
                                    entry.updatePlayer(player);
                                }
                            }
                        }
                    }, 2);
                } catch (Throwable e) {
                    MainUtil.handleError(e);
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public boolean fixLighting(FaweChunk chunk, RelightMode mode) {
        if (mode == RelightMode.NONE) {
            return true;
        }
        try {
            CharFaweChunk<Chunk> fc = (CharFaweChunk<Chunk>) chunk;
            CraftChunk craftChunk = (CraftChunk) fc.getChunk();
            net.minecraft.server.v1_8_R3.Chunk nmsChunk = craftChunk.getHandle();
            if (!craftChunk.isLoaded()) {
                return false;
            }
            ChunkSection[] sections = nmsChunk.getSections();
            final boolean flag = craftChunk.getWorld().getEnvironment() == World.Environment.NORMAL;
            if (mode == RelightMode.ALL) {
                for (int i = 0; i < sections.length; i++) {
                    ChunkSection section = sections[i];
                    if (section != null) {
                        section.a(new NibbleArray());
                        if (flag) {
                            section.b(new NibbleArray());
                        }
                    }
                }
            }
            if (flag) {
                if (mode == RelightMode.ALL) {
                    nmsChunk.initLighting();
                } else {
                    int i = nmsChunk.g();
                    for (int x = 0; x < 16; ++x) {
                        for (int z = 0; z < 16; ++z) {
                            int l = 15;
                            int y = i + 16 - 1;
                            do {
                                int opacity = nmsChunk.getTypeAbs(x, y, z).p();
                                if (opacity == 0 && l != 15) {
                                    opacity = 1;
                                }
                                l -= opacity;
                                if (l > 0) {
                                    ChunkSection section = sections[y >> 4];
                                    if (section != null) {
                                        section.a(x, y & 15, z, l);
                                    }
                                }
                                --y;
                            } while (y > 0 && l > 0);
                        }
                    }
                }
            }

            if (fc.getTotalRelight() == 0 && mode == RelightMode.MINIMAL) {
                return true;
            }
            net.minecraft.server.v1_8_R3.World nmsWorld = nmsChunk.getWorld();

            int X = fc.getX() << 4;
            int Z = fc.getZ() << 4;

            for (int j = 0; j < sections.length; j++) {
                ChunkSection section = sections[j];
                if (section == null) {
                    continue;
                }
                if (((fc.getRelight(j) == 0) && mode == RelightMode.MINIMAL) || (fc.getCount(j) == 0 && mode != RelightMode.ALL) || ((fc.getCount(j) >= 4096) && (fc.getAir(j) == 0)) || fc.getAir(j) == 4096) {
                    continue;
                }
                char[] array = section.getIdArray();
                if (array == null) {
                    continue;
                }
                if (mode == RelightMode.ALL) {
                    for (int k = array.length - 1; k >= 0; k--) {
                        final int x = FaweCache.CACHE_X[j][k];
                        final int y = FaweCache.CACHE_Y[j][k];
                        final int z = FaweCache.CACHE_Z[j][k];
                        if (isSurrounded(sections, x, y, z)) {
                            continue;
                        }
                        pos.c(X + x, y, Z + z);
                        nmsWorld.x(pos);
                    }
                    continue;
                }
                for (int k = array.length - 1; k >= 0; k--) {
                    final int i = array[k];
                    final short id = (short) (i >> 4);
                    switch (id) { // Lighting
                        case 0:
                            continue;
                        default:
                            if (mode == RelightMode.MINIMAL) {
                                continue;
                            }
                            if (PseudoRandom.random.random(3) != 0) {
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
                        case 213:
                            final int x = FaweCache.CACHE_X[j][k];
                            final int y = FaweCache.CACHE_Y[j][k];
                            final int z = FaweCache.CACHE_Z[j][k];
                            if (isSurrounded(sections, x, y, z)) {
                                continue;
                            }
                            pos.c(X + x, y, Z + z);
                            nmsWorld.x(pos);
                    }
                }
            }
            return true;
        } catch (Throwable e) {
            if (Thread.currentThread() == Fawe.get().getMainThread()) {
                MainUtil.handleError(e);
            }
        }
        return false;
    }

    public boolean isSurrounded(ChunkSection[] sections, int x, int y, int z) {
        return isSolid(getId(sections, x, y + 1, z))
                && isSolid(getId(sections, x + 1, y - 1, z))
                && isSolid(getId(sections, x - 1, y, z))
                && isSolid(getId(sections, x, y, z + 1))
                && isSolid(getId(sections, x, y, z - 1));
    }

    public boolean isSolid(int i) {
        return Material.getMaterial(i).isOccluding();
    }

    public int getId(ChunkSection[] sections, int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            return 1;
        }
        if (y < 0 || y > 255) {
            return 1;
        }
        int i = FaweCache.CACHE_I[y][x][z];
        ChunkSection section = sections[i];
        if (section == null) {
            return 0;
        }
        char[] array = section.getIdArray();
        int j = FaweCache.CACHE_J[y][x][z];
        return array[j] >> 4;
    }
}

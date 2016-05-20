package com.boydti.fawe.bukkit.v1_9;

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
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.internal.Constants;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
import net.minecraft.server.v1_9_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_9_R2.ChunkSection;
import net.minecraft.server.v1_9_R2.DataBits;
import net.minecraft.server.v1_9_R2.DataPalette;
import net.minecraft.server.v1_9_R2.DataPaletteBlock;
import net.minecraft.server.v1_9_R2.Entity;
import net.minecraft.server.v1_9_R2.EntityHuman;
import net.minecraft.server.v1_9_R2.EntityPlayer;
import net.minecraft.server.v1_9_R2.EntityTracker;
import net.minecraft.server.v1_9_R2.EntityTrackerEntry;
import net.minecraft.server.v1_9_R2.EntityTypes;
import net.minecraft.server.v1_9_R2.IBlockData;
import net.minecraft.server.v1_9_R2.NBTTagCompound;
import net.minecraft.server.v1_9_R2.NibbleArray;
import net.minecraft.server.v1_9_R2.Packet;
import net.minecraft.server.v1_9_R2.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_9_R2.PacketPlayOutMapChunk;
import net.minecraft.server.v1_9_R2.PlayerChunk;
import net.minecraft.server.v1_9_R2.PlayerChunkMap;
import net.minecraft.server.v1_9_R2.TileEntity;
import net.minecraft.server.v1_9_R2.WorldServer;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_9_R2.CraftChunk;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class BukkitQueue_1_9_R1 extends BukkitQueue_0<Chunk, ChunkSection[], DataPaletteBlock> {

    private IBlockData air;

    public BukkitQueue_1_9_R1(final String world) {
        super(world);
        checkVersion("v1_9_R2");
        try {
            Field fieldAir = DataPaletteBlock.class.getDeclaredField("a");
            fieldAir.setAccessible(true);
            air = (IBlockData) fieldAir.get(null);
            if (adapter == null) {
                setupAdapter(new FaweAdapter_1_9());
                Fawe.debug("Using adapter: " + adapter);
                Fawe.debug("=========================================");
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ChunkSection[] getCachedSections(World world, int cx, int cz) {
        CraftChunk chunk = (CraftChunk) world.getChunkAt(cx, cz);
        return chunk.getHandle().getSections();
    }

    @Override
    public DataPaletteBlock getCachedSection(ChunkSection[] chunkSections, int cy) {
        ChunkSection nibble = chunkSections[cy];
        return nibble != null ? nibble.getBlocks() : null;
    }

    @Override
    public int getCombinedId4Data(DataPaletteBlock lastSection, int x, int y, int z) {
        IBlockData ibd = lastSection.a(x & 15, y & 15, z & 15);
        Block block = ibd.getBlock();
        int id = Block.getId(block);
        if (FaweCache.hasData(id)) {
            return (id << 4) + block.toLegacyData(ibd);
        } else {
            return id << 4;
        }
    }

    @Override
    public void refreshChunk(World world, Chunk chunk) {
        if (!chunk.isLoaded()) {
            return;
        }
        net.minecraft.server.v1_9_R2.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
        ChunkCoordIntPair pos = nmsChunk.k(); // getPosition()
        WorldServer w = (WorldServer) nmsChunk.getWorld();
        PlayerChunkMap chunkMap = w.getPlayerChunkMap();
        PlayerChunk playerChunk = chunkMap.getChunk(pos.x, pos.z);
        if (playerChunk == null) {
            return;
        }
        HashSet<EntityPlayer> set = new HashSet<EntityPlayer>(playerChunk.c);
        EntityTracker tracker = w.getTracker();
        // Get players
        HashSet<EntityPlayer> players = new HashSet<>();
        for (EntityHuman human : w.players) {
            if (set.contains(human)) {
                players.add((EntityPlayer) human);
            }
        }
        if (players.size() == 0) {
            return;
        }
        HashSet<EntityTrackerEntry> entities = new HashSet<>();
        List<Entity>[] entitieSlices = playerChunk.chunk.getEntitySlices();
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
        for (EntityPlayer player : players) {
            player.playerConnection.networkManager.a();
        }
        // Send chunks
        PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(playerChunk.chunk, '\uffff');
        for (EntityPlayer player : players) {
            player.playerConnection.sendPacket(packet);
        }
        // send ents
        for (List<Entity> slice : entitieSlices) {
            if (slice == null) {
                continue;
            }
            for (Entity ent : slice) {
                EntityTrackerEntry entry = tracker.trackedEntities.get(ent.getId());
                if (entry == null) {
                    continue;
                }
                try {
                    Method methodE = entry.getClass().getDeclaredMethod("e");
                    methodE.setAccessible(true);
                    Packet p = (Packet) methodE.invoke(entry);
                    TaskManager.IMP.later(new Runnable() {
                        @Override
                        public void run() {
                            for (EntityPlayer player : players) {
                                boolean result = entry.trackedPlayers.remove(player);
                                if (result && ent != player) {
                                    entry.updatePlayer(player);
                                    entry.trackedPlayers.add(player);
                                }
                            }
                        }
                    }, 2);
                } catch (Throwable e) {
                    MainUtil.handleError(e);
                }
            }
        }
    }

    @Override
    public boolean fixLighting(final FaweChunk pc, RelightMode mode) {
        if (mode == RelightMode.NONE) {
            return true;
        }
        try {
            CharFaweChunk bc = (CharFaweChunk) pc;
            Chunk chunk = (Chunk) bc.getChunk();
            if (!chunk.isLoaded()) {
                if (Fawe.get().getMainThread() != Thread.currentThread()) {
                    return false;
                }
                chunk.load(false);
            }
            net.minecraft.server.v1_9_R2.Chunk c = ((CraftChunk) chunk).getHandle();
            ChunkSection[] sections = c.getSections();
            if (mode == RelightMode.ALL) {
                for (int i = 0; i < sections.length; i++) {
                    ChunkSection section = sections[i];
                    if (section != null) {
                        section.a(new NibbleArray());
                        section.b(new NibbleArray());
                    }
                }
            }
            c.initLighting();
            if (((bc.getTotalRelight() == 0) && mode == RelightMode.MINIMAL)) {
                return true;
            }
            if (mode == RelightMode.ALL) {
                bc = getPrevious(bc, c.getSections(), null, null, null, true);
            }
            int total = bc.getTotalCount();
            net.minecraft.server.v1_9_R2.World w = c.world;
            final int X = chunk.getX() << 4;
            final int Z = chunk.getZ() << 4;
            for (int j = sections.length - 1; j >= 0; j--) {
                final Object section = sections[j];
                if (section == null) {
                    continue;
                }
                if (((bc.getRelight(j) == 0) && mode == RelightMode.MINIMAL) || (bc.getCount(j) == 0 && mode != RelightMode.ALL) || ((bc.getCount(j) >= 4096) && (bc.getAir(j) == 0)) || bc.getAir(j) == 4096) {
                    continue;
                }
                final char[] array = bc.getIdArray(j);
                if (array == null) {
                    continue;
                }
                if (mode == RelightMode.ALL) {
                    for (int k = array.length - 1; k >= 0; k--) {
                        final int x = FaweCache.CACHE_X[j][k];
                        final int y = FaweCache.CACHE_Y[j][k];
                        final int z = FaweCache.CACHE_Z[j][k];
                        if (this.isSurrounded(bc.getCombinedIdArrays(), x, y, z)) {
                            continue;
                        }
                        pos.c(X + x, y, Z + z);
                        w.w(pos);
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
                            final int x = FaweCache.CACHE_X[j][k];
                            final int y = FaweCache.CACHE_Y[j][k];
                            final int z = FaweCache.CACHE_Z[j][k];
                            if (this.isSurrounded(bc.getCombinedIdArrays(), x, y, z)) {
                                continue;
                            }
                            pos.c(X + x, y, Z + z);
                            w.w(pos);
                    }
                }
            }
            return true;
        } catch (final Throwable e) {
            if (Thread.currentThread() == Fawe.get().getMainThread()) {
                MainUtil.handleError(e);
            }
        }
        return false;
    }

    public boolean isSurrounded(final char[][] sections, final int x, final int y, final int z) {
        return this.isSolid(this.getId(sections, x, y + 1, z))
                && this.isSolid(this.getId(sections, x + 1, y - 1, z))
                && this.isSolid(this.getId(sections, x - 1, y, z))
                && this.isSolid(this.getId(sections, x, y, z + 1))
                && this.isSolid(this.getId(sections, x, y, z - 1));
    }

    public boolean isSolid(final int i) {
        if (i != 0) {
            final Material material = Material.getMaterial(i);
            return (material != null) && Material.getMaterial(i).isOccluding();
        }
        return false;
    }

    public int getId(final char[][] sections, final int x, final int y, final int z) {
        if ((x < 0) || (x > 15) || (z < 0) || (z > 15)) {
            return 1;
        }
        if ((y < 0) || (y > 255)) {
            return 1;
        }
        final int i = FaweCache.CACHE_I[y][x][z];
        final char[] section = sections[i];
        if (section == null) {
            return 0;
        }
        final int j = FaweCache.CACHE_J[y][x][z];
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
                        int j = FaweCache.CACHE_J[y][x][z];
                        if (array[j] != 0) {
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
            Set<Entry<BlockPosition, TileEntity>> entryset = tiles.entrySet();
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
                    tile.getValue().invalidateBlockCache();
                    iterator.remove();
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
                            char combinedId = array[FaweCache.CACHE_J[y][x][z]];
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
        sendChunk(fs, null);
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
}

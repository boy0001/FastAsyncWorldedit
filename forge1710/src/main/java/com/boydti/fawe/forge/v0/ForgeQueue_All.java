package com.boydti.fawe.forge.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.BytePair;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.IntegerPair;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;

public class ForgeQueue_All extends NMSMappedFaweQueue<World, Chunk, ExtendedBlockStorage[], ExtendedBlockStorage> {

    private Method methodFromNative;
    private Method methodToNative;

    public ForgeQueue_All(String world) {
        super(world);
        try {
            Class<?> converter = Class.forName("com.sk89q.worldedit.forge.NBTConverter");
            this.methodFromNative = converter.getDeclaredMethod("toNative", Tag.class);
            this.methodToNative = converter.getDeclaredMethod("fromNative", NBTBase.class);
            methodFromNative.setAccessible(true);
            methodToNative.setAccessible(true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompoundTag getTileEntity(Chunk chunk, int x, int y, int z) {
        Map<ChunkPosition, TileEntity> tiles = chunk.chunkTileEntityMap;
        ChunkPosition pos = new ChunkPosition(x, y, z);
        TileEntity tile = tiles.get(pos);
        return tile != null ? getTag(tile) : null;
    }

    public CompoundTag getTag(TileEntity tile) {
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.readFromNBT(tag); // readTagIntoEntity
            return (CompoundTag) methodToNative.invoke(null, tag);
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    @Override
    public Chunk getChunk(World world, int x, int z) {
        Chunk chunk = world.getChunkProvider().provideChunk(x, z);
        if (chunk != null && !chunk.isChunkLoaded) {
            chunk.onChunkLoad();
        }
        return chunk;
    }

    @Override
    public boolean loadChunk(World world, int x, int z, boolean generate) {
        return getCachedSections(world, x, z) != null;
    }

    @Override
    public ExtendedBlockStorage[] getCachedSections(World world, int cx, int cz) {
        Chunk chunk = world.getChunkProvider().provideChunk(cx, cz);
        if (chunk != null && !chunk.isChunkLoaded) {
            chunk.onChunkLoad();
        }
        return chunk != null ? chunk.getBlockStorageArray() : null;
    }

    @Override
    public ExtendedBlockStorage getCachedSection(ExtendedBlockStorage[] extendedBlockStorages, int cy) {
        return extendedBlockStorages[cy];
    }

    @Override
    public int getCombinedId4Data(ExtendedBlockStorage ls, int x, int y, int z) {
        byte[] ids = ls.getBlockLSBArray();
        NibbleArray datasNibble = ls.getBlockMSBArray();
        int i = FaweCache.CACHE_J[y & 15][x & 15][z & 15];
        int combined = (ids[i] << 4) + (datasNibble == null ? 0 : datasNibble.get(x & 15, y & 15, z & 15));
        return combined;
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.getChunkProvider().chunkExists(x, z);
    }

    @Override
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
        try {
            IChunkProvider provider = world.getChunkProvider();
            if (!(provider instanceof ChunkProviderServer)) {
                return false;
            }
            ChunkProviderServer chunkServer = (ChunkProviderServer) provider;

            Chunk mcChunk;
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                mcChunk.onChunkUnload();
            }
            Field u;
            try {
                u = ChunkProviderServer.class.getDeclaredField("field_73248_b"); // chunksToUnload
            } catch(NoSuchFieldException e) {
                u = ChunkProviderServer.class.getDeclaredField("chunksToUnload");
            }
            u.setAccessible(true);
            Set unloadQueue = (Set) u.get(chunkServer);
            Field m;
            try {
                m = ChunkProviderServer.class.getDeclaredField("field_73244_f"); // loadedChunkHashMap
            } catch(NoSuchFieldException e) {
                m = ChunkProviderServer.class.getDeclaredField("loadedChunkHashMap");
            }
            m.setAccessible(true);
            LongHashMap loadedMap = (LongHashMap) m.get(chunkServer);
            Field lc;
            try {
                lc = ChunkProviderServer.class.getDeclaredField("field_73245_g"); // loadedChunkHashMap
            } catch(NoSuchFieldException e) {
                lc = ChunkProviderServer.class.getDeclaredField("loadedChunks");
            }
            lc.setAccessible(true);
            @SuppressWarnings("unchecked") List loaded = (List) lc.get(chunkServer);
            Field p;
            try {
                p = ChunkProviderServer.class.getDeclaredField("field_73246_d"); // currentChunkProvider
            } catch(NoSuchFieldException e) {
                p = ChunkProviderServer.class.getDeclaredField("currentChunkProvider");
            }
            p.setAccessible(true);
            IChunkProvider chunkProvider = (IChunkProvider) p.get(chunkServer);
            long pos = ChunkCoordIntPair.chunkXZ2Int(x, z);
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                mcChunk.onChunkUnload();
            }
            unloadQueue.remove(pos);
            loadedMap.remove(pos);
            mcChunk = chunkProvider.provideChunk(x, z);
            loadedMap.add(pos, mcChunk);
            loaded.add(mcChunk);
            if (mcChunk != null) {
                mcChunk.onChunkLoad();
                mcChunk.populateChunk(chunkProvider, chunkProvider, x, z);
            }
        } catch (Throwable t) {
            MainUtil.handleError(t);
            return false;
        }
        return true;
    }

    private final RunnableVal<IntegerPair> loadChunk = new RunnableVal<IntegerPair>() {
        @Override
        public void run(IntegerPair loc) {
            Chunk chunk = getWorld().getChunkProvider().provideChunk(loc.x, loc.z);
            if (chunk != null && !chunk.isChunkLoaded) {
                chunk.onChunkLoad();
            }

        }
    };

    @Override
    public void refreshChunk(World world, net.minecraft.world.chunk.Chunk nmsChunk) {
        if (!nmsChunk.isChunkLoaded) {
            return;
        }
        try {
            ChunkCoordIntPair pos = nmsChunk.getChunkCoordIntPair();
            WorldServer w = (WorldServer) nmsChunk.worldObj;
            PlayerManager chunkMap = w.getPlayerManager();
            int x = pos.chunkXPos;
            int z = pos.chunkZPos;
            if (!chunkMap.func_152621_a(x, z)) {
                return;
            }
            EntityTracker tracker = w.getEntityTracker();
            final HashSet<EntityPlayerMP> players = new HashSet<>();
            for (EntityPlayer player : (List<EntityPlayer>) w.playerEntities) {
                if (player instanceof EntityPlayerMP) {
                    if (chunkMap.isPlayerWatchingChunk((EntityPlayerMP) player, x, z)) {
                        players.add((EntityPlayerMP) player);
                    }
                }
            }
            if (players.size() == 0) {
                return;
            }
            HashSet<EntityTrackerEntry> entities = new HashSet<>();
            Collection<Entity>[] entitieSlices = nmsChunk.entityLists;
            IntHashMap entries = null;
            for (Field field : tracker.getClass().getDeclaredFields()) {
                if (field.getType() == IntHashMap.class) {
                    field.setAccessible(true);
                    entries = (IntHashMap) field.get(tracker);
                }
            }
            for (Collection<Entity> slice : entitieSlices) {
                if (slice == null) {
                    continue;
                }
                for (Entity ent : slice) {
                    EntityTrackerEntry entry = entries != null ? (EntityTrackerEntry) entries.lookup(ent.getEntityId()) : null;
                    if (entry == null) {
                        continue;
                    }
                    entities.add(entry);
                    S13PacketDestroyEntities packet = new S13PacketDestroyEntities(ent.getEntityId());
                    for (EntityPlayerMP player : players) {
                        player.playerNetServerHandler.sendPacket(packet);
                    }
                }
            }
            // Send chunks
            S21PacketChunkData packet = new S21PacketChunkData(nmsChunk, false, 65535);
            for (EntityPlayerMP player : players) {
                player.playerNetServerHandler.sendPacket(packet);
            }
            // send ents
            for (final EntityTrackerEntry entry : entities) {
                try {
                    TaskManager.IMP.later(new Runnable() {
                        @Override
                        public void run() {
                            for (EntityPlayerMP player : players) {
                                boolean result = entry.trackingPlayers.remove(player);
                                if (result && entry.myEntity != player) {
                                    entry.tryStartWachingThis(player);
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
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        ForgeChunk_All fs = (ForgeChunk_All) fc;
        net.minecraft.world.chunk.Chunk nmsChunk = fs.getChunk();
        net.minecraft.world.World nmsWorld = nmsChunk.worldObj;
        try {
            boolean flag = !nmsWorld.provider.hasNoSky;
            // Sections
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            Map<ChunkPosition, TileEntity> tiles = nmsChunk.chunkTileEntityMap;
            List<Entity>[] entities = nmsChunk.entityLists;

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
                        int x = ((int) Math.round(entity.posX) & 15);
                        int z = ((int) Math.round(entity.posZ) & 15);
                        int y = (int) Math.round(entity.posY);
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
                NBTTagCompound tag = (NBTTagCompound)methodFromNative.invoke(null, nativeTag);
                Entity entity = EntityList.createEntityFromNBT(tag, nmsWorld);
                if (entity != null) {
                    entity.setPositionAndRotation(x, y, z, yaw, pitch);
                    nmsWorld.spawnEntityInWorld(entity);
                }
            }
            // Run change task if applicable
            if (changeTask != null) {
                CharFaweChunk previous = getPrevious(fs, sections, tiles, entities, createdEntities, false);
                changeTask.run(previous);
            }
            // Trim tiles
            Set<Map.Entry<ChunkPosition, TileEntity>> entryset = tiles.entrySet();
            Iterator<Map.Entry<ChunkPosition, TileEntity>> iterator = entryset.iterator();
            while (iterator.hasNext()) {
                Map.Entry<ChunkPosition, TileEntity> tile = iterator.next();
                ChunkPosition pos = tile.getKey();
                int lx = pos.chunkPosX & 15;
                int ly = pos.chunkPosY;
                int lz = pos.chunkPosZ & 15;
                int j = FaweCache.CACHE_I[ly][lx][lz];
                char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int k = FaweCache.CACHE_J[ly][lx][lz];
                if (array[k] != 0) {
                    tile.getValue().invalidate();;
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
            // Efficiently merge sections
            for (int j = 0; j < sections.length; j++) {
                if (fs.getCount(j) == 0) {
                    continue;
                }
                byte[] newIdArray = fs.getByteIdArray(j);
                if (newIdArray == null) {
                    continue;
                }
                NibbleArray newDataArray = fs.getDataArray(j);
                ExtendedBlockStorage section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    sections[j] = section = new ExtendedBlockStorage(j << 4, !getWorld().provider.hasNoSky);
                    section.setBlockLSBArray(newIdArray);
                    section.setBlockMetadataArray(newDataArray);
                    continue;
                }
                byte[] currentIdArray = section.getBlockLSBArray();
                NibbleArray currentDataArray = section.getMetadataArray();
                boolean data = currentDataArray != null;
                if (!data) {
                    section.setBlockMetadataArray(newDataArray);
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
                                int x = FaweCache.CACHE_X[j][k];
                                int y = FaweCache.CACHE_Y[j][k];
                                int z = FaweCache.CACHE_Z[j][k];
                                int newData = newDataArray == null ? 0 : newDataArray.get(x, y, z);
                                int currentData = currentDataArray == null ? 0 : currentDataArray.get(x, y, z);
                                if (newData != currentData) {
                                    currentDataArray.set(x, y, z, newData);
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
                        nmsChunk.getBiomeArray()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome;
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
                int x = MathMan.unpair16x(pair.pair[0]) + bx;
                int y = pair.pair[1] & 0xFF;
                int z = MathMan.unpair16y(pair.pair[0]) + bz;
                TileEntity tileEntity = nmsWorld.getTileEntity(x, y, z);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) methodFromNative.invoke(null, nativeTag);
                    tileEntity.readFromNBT(tag); // ReadTagIntoTile
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
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
        sendChunk(fs, null);
        return true;
    }

    public void setCount(int tickingBlockCount, int nonEmptyBlockCount, ExtendedBlockStorage section) throws NoSuchFieldException, IllegalAccessException {
        Class<? extends ExtendedBlockStorage> clazz = section.getClass();
        Field fieldTickingBlockCount = clazz.getDeclaredField("field_76683_c"); // tickRefCount
        Field fieldNonEmptyBlockCount = clazz.getDeclaredField("field_76682_b"); // blockRefCount
        fieldTickingBlockCount.setAccessible(true);
        fieldNonEmptyBlockCount.setAccessible(true);
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, ExtendedBlockStorage[] sections, Map<?, ?> tilesGeneric, Collection<?>[] entitiesGeneric, Set<UUID> createdEntities, boolean all) throws Exception {
        Map<ChunkPosition, TileEntity> tiles = (Map<ChunkPosition, TileEntity>) tilesGeneric;
        Collection<Entity>[] entities = (Collection<Entity>[]) entitiesGeneric;
        CharFaweChunk previous = (CharFaweChunk) getFaweChunk(fs.getX(), fs.getZ());
        char[][] idPrevious = new char[16][];
        for (int layer = 0; layer < sections.length; layer++) {
            if (fs.getCount(layer) != 0 || all) {
                ExtendedBlockStorage section = sections[layer];
                if (section != null) {
                    byte[] currentIdArray = section.getBlockLSBArray();
                    NibbleArray currentDataArray = section.getMetadataArray();
                    char[] array = new char[4096];
                    for (int j = 0; j < currentIdArray.length; j++) {
                        int x = FaweCache.CACHE_X[layer][j];
                        int y = FaweCache.CACHE_Y[layer][j];
                        int z = FaweCache.CACHE_Z[layer][j];
                        int id = currentIdArray[j] & 0xFF;
                        byte data = (byte) currentDataArray.get(x, y & 15, z);
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
                tile.readFromNBT(tag); // readTileEntityIntoTag
                ChunkPosition pos = entry.getKey();
                CompoundTag nativeTag = (CompoundTag) methodToNative.invoke(null, tag);
                previous.setTile(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, nativeTag);
            }
        }
        if (entities != null) {
            for (Collection<Entity> entityList : entities) {
                for (Entity ent : entityList) {
                    if (ent instanceof EntityPlayer || (!createdEntities.isEmpty() && createdEntities.contains(ent.getUniqueID()))) {
                        continue;
                    }
                    int x = ((int) Math.round(ent.posX) & 15);
                    int z = ((int) Math.round(ent.posZ) & 15);
                    int y = (int) Math.round(ent.posY);
                    int i = FaweCache.CACHE_I[y][x][z];
                    char[] array = fs.getIdArray(i);
                    if (array == null) {
                        continue;
                    }
                    int j = FaweCache.CACHE_J[y][x][z];
                    if (array[j] != 0) {
                        String id = EntityList.getEntityString(ent);
                        if (id != null) {
                            NBTTagCompound tag = ent.getEntityData();  // readEntityIntoTag
                            CompoundTag nativeTag = (CompoundTag) methodToNative.invoke(null, tag);
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

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return new ForgeChunk_All(this, x, z);
    }

    @Override
    public boolean fixLighting(FaweChunk<?> fc, RelightMode mode) {
        if (mode == RelightMode.NONE) {
            return true;
        }
        try {
            ForgeChunk_All bc = (ForgeChunk_All) fc;
            net.minecraft.world.chunk.Chunk nmsChunk = bc.getChunk();
            if (!nmsChunk.isChunkLoaded) {
                return false;
            }
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            if (mode == RelightMode.ALL) {
                for (int i = 0; i < sections.length; i++) {
                    ExtendedBlockStorage section = sections[i];
                    if (section != null) {
                        section.setSkylightArray(new NibbleArray(4096, 4));
                        section.setBlocklightArray(new NibbleArray(4096, 4));
                    }
                }
            }
            nmsChunk.generateSkylightMap();
            if (bc.getTotalRelight() == 0 && mode == RelightMode.MINIMAL) {
                return true;
            }
            net.minecraft.world.World nmsWorld = nmsChunk.worldObj;

            int X = fc.getX() << 4;
            int Z = fc.getZ() << 4;

            for (int j = 0; j < sections.length; j++) {
                ExtendedBlockStorage section = sections[j];
                if (section == null) {
                    continue;
                }
                if (((bc.getRelight(j) == 0) && mode == RelightMode.MINIMAL) || (bc.getCount(j) == 0 && mode != RelightMode.ALL) || ((bc.getCount(j) >= 4096) && (bc.getAir(j) == 0)) || bc.getAir(j) == 4096) {
                    continue;
                }
                byte[] array = section.getBlockLSBArray();
                if (mode == RelightMode.ALL) {
                    for (int k = array.length - 1; k >= 0; k--) {
                        final int x = FaweCache.CACHE_X[j][k];
                        final int y = FaweCache.CACHE_Y[j][k];
                        final int z = FaweCache.CACHE_Z[j][k];
                        if (isSurrounded(sections, x, y, z)) {
                            continue;
                        }
                        nmsWorld.func_147451_t(X + x, y, Z + z);
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
                            if (isSurrounded(sections, x, y, z)) {
                                continue;
                            }
                            nmsWorld.func_147451_t(X + x, y, Z + z);
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
        byte[] array = section.getBlockLSBArray();
        int j = FaweCache.CACHE_J[y][x][z];
        return array[j];
    }
}

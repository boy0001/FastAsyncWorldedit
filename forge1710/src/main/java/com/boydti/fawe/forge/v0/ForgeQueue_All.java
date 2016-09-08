package com.boydti.fawe.forge.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.BytePair;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;

public class ForgeQueue_All extends NMSMappedFaweQueue<World, Chunk, ExtendedBlockStorage[], ExtendedBlockStorage> {

    private static Method methodFromNative;
    private static Method methodToNative;

    public ForgeQueue_All(com.sk89q.worldedit.world.World world) {
        super(world);
        if (methodFromNative == null) {
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
        getImpWorld();
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
        int i = FaweCache.CACHE_J[y & 15][z & 15][x & 15];
        int combined = (ids[i] << 4) + (datasNibble == null ? 0 : datasNibble.get(x & 15, y & 15, z & 15));
        return combined;
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.getChunkProvider().chunkExists(x, z);
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
    public void refreshChunk(FaweChunk fc) {
        ForgeChunk_All fs = (ForgeChunk_All) fc;
        ensureChunkLoaded(fc.getX(), fc.getZ());
        Chunk nmsChunk = fs.getChunk();
        if (!nmsChunk.isChunkLoaded) {
            return;
        }
        try {
            WorldServer w = (WorldServer) nmsChunk.worldObj;
            PlayerManager chunkMap = w.getPlayerManager();
            int x = nmsChunk.xPosition;
            int z = nmsChunk.zPosition;
            if (!chunkMap.func_152621_a(x, z)) {
                return;
            }
            HashSet<EntityPlayerMP> players = new HashSet<>();
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
            int mask = fc.getBitMask();
            if (mask == 65535 && hasEntities(nmsChunk)) {
                S21PacketChunkData packet = new S21PacketChunkData(nmsChunk, false, 65280);
                for (EntityPlayerMP player : players) {
                    player.playerNetServerHandler.sendPacket(packet);
                }
                mask = 255;
            }
            S21PacketChunkData packet = new S21PacketChunkData(nmsChunk, false, mask);
            for (EntityPlayerMP player : players) {
                player.playerNetServerHandler.sendPacket(packet);
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    public boolean hasEntities(Chunk nmsChunk) {
        for (int i = 0; i < nmsChunk.entityLists.length; i++) {
            List slice = nmsChunk.entityLists[i];
            if (slice != null && !slice.isEmpty()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        ForgeChunk_All fs = (ForgeChunk_All) fc;
        net.minecraft.world.chunk.Chunk nmsChunk = fs.getChunk();
        net.minecraft.world.World nmsWorld = nmsChunk.worldObj;
        nmsChunk.setChunkModified();
        nmsChunk.sendUpdates = true;
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
                int j = FaweCache.CACHE_I[ly][lz][lx];
                char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int k = FaweCache.CACHE_J[ly][lz][lx];
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
                char[] charArray = fs.getIdArray(j);
                for (int k = 0; k < newIdArray.length; k++) {
                    char combined = charArray[k];
                    switch (combined) {
                        case 0:
                            fill = false;
                            continue;
                        case 1:
                            fill = false;
                            if (currentIdArray[k] != 0) {
                                solid++;
                            }
                            currentIdArray[k] = 0;
                            continue;
                        default:
                            solid++;
                            currentIdArray[k] = newIdArray[k];
                            if (data) {
                                int dataByte = FaweCache.getData(combined);
                                int x = FaweCache.CACHE_X[0][k];
                                int y = FaweCache.CACHE_Y[0][k];
                                int z = FaweCache.CACHE_Z[0][k];
                                int newData = newDataArray.get(x, y, z);
                                currentDataArray.set(x, y, z, newData);
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
                int x = MathMan.unpair16x((byte) pair.get0()) + bx;
                int y = pair.get1() & 0xFF;
                int z = MathMan.unpair16y((byte) pair.get0()) + bz;
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
                    int i = FaweCache.CACHE_I[y][z][x];
                    char[] array = fs.getIdArray(i);
                    if (array == null) {
                        continue;
                    }
                    int j = FaweCache.CACHE_J[y][z][x];
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
    public boolean removeLighting(ExtendedBlockStorage[] sections, RelightMode mode, boolean sky) {
        if (mode == RelightMode.ALL) {
            for (int i = 0; i < sections.length; i++) {
                ExtendedBlockStorage section = sections[i];
                if (section != null) {
                    section.setBlocklightArray(new NibbleArray(4096, 4));
                    if (sky) {
                        section.setSkylightArray(new NibbleArray(4096, 4));
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean hasSky() {
        return !nmsWorld.provider.hasNoSky;
    }

    @Override
    public void setFullbright(ExtendedBlockStorage[] sections) {
        for (int i = 0; i < sections.length; i++) {
            ExtendedBlockStorage section = sections[i];
            if (section != null) {
                byte[] bytes = section.getSkylightArray().data;
                Arrays.fill(bytes, (byte) 255);
            }
        }
    }

    @Override
    public void relight(int x, int y, int z) {
        nmsWorld.func_147451_t(x, y, z);
    }

    private WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        if (nmsWorld != null) {
            return nmsWorld;
        }
        String[] split = getWorldName().split(";");
        int id = Integer.parseInt(split[split.length - 1]);
        return nmsWorld = DimensionManager.getWorld(id);
    }

    @Override
    public void setSkyLight(ExtendedBlockStorage section, int x, int y, int z, int value) {
        section.getSkylightArray().set(x & 15, y & 15, z & 15, value);
    }

    @Override
    public void setBlockLight(ExtendedBlockStorage section, int x, int y, int z, int value) {
        section.getBlocklightArray().set(x & 15, y & 15, z & 15, value);
    }

    @Override
    public int getSkyLight(ExtendedBlockStorage section, int x, int y, int z) {
        return section.getExtSkylightValue(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmmittedLight(ExtendedBlockStorage section, int x, int y, int z) {
        return section.getExtBlocklightValue(x & 15, y & 15, z & 15);
    }

    @Override
    public int getOpacity(ExtendedBlockStorage section, int x, int y, int z) {
        int combined = getCombinedId4Data(section, x, y, z);
        if (combined == 0) {
            return 0;
        }
        Block block = Block.getBlockById(FaweCache.getId(combined));
        return block.getLightOpacity();
    }

    @Override
    public int getBrightness(ExtendedBlockStorage section, int x, int y, int z) {
        int combined = getCombinedId4Data(section, x, y, z);
        if (combined == 0) {
            return 0;
        }
        Block block = Block.getBlockById(FaweCache.getId(combined));
        return block.getLightValue();
    }

    @Override
    public int getOpacityBrightnessPair(ExtendedBlockStorage section, int x, int y, int z) {
        int combined = getCombinedId4Data(section, x, y, z);
        if (combined == 0) {
            return 0;
        }
        Block block = Block.getBlockById(FaweCache.getId(combined));
        return MathMan.pair16(block.getLightOpacity(), block.getLightValue());
    }

    @Override
    public boolean hasBlock(ExtendedBlockStorage section, int x, int y, int z) {
        int i = FaweCache.CACHE_J[y & 15][z & 15][x & 15];
        return section.getBlockLSBArray()[i] != 0;
    }

    @Override
    public void relightBlock(int x, int y, int z) {
        nmsWorld.updateLightByType(EnumSkyBlock.Block, x, y, z);
    }

    @Override
    public void relightSky(int x, int y, int z) {
        nmsWorld.updateLightByType(EnumSkyBlock.Sky, x, y, z);
    }

    @Override
    public File getSaveFolder() {
        return new File(((WorldServer) getWorld()).getChunkSaveLocation(), "region");
    }
}

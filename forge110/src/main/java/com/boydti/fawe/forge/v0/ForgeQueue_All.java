package com.boydti.fawe.forge.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
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
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;

public class ForgeQueue_All extends NMSMappedFaweQueue<World, Chunk, ExtendedBlockStorage[], ExtendedBlockStorage> {

    private static Method methodFromNative;
    private static Method methodToNative;

    public ForgeQueue_All(String world) {
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

    private BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0, 0, 0);

    @Override
    public CompoundTag getTileEntity(Chunk chunk, int x, int y, int z) {
        Map<BlockPos, TileEntity> tiles = chunk.getTileEntityMap();
        pos.setPos(x, y, z);
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
        if (chunk != null && !chunk.isLoaded()) {
            chunk.onChunkLoad();
        }
        return chunk;
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return getWorld().getChunkProvider().getLoadedChunk(x, z) != null;
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z) {
        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return false;
        }


        try {
            ChunkProviderServer chunkServer = (ChunkProviderServer) provider;
            IChunkGenerator gen = chunkServer.chunkGenerator;
            long pos = ChunkPos.chunkXZ2Int(x, z);
            Chunk mcChunk;
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                mcChunk.onChunkUnload();
            }
            PlayerChunkMap playerManager = ((WorldServer) getWorld()).getPlayerChunkMap();
            List<EntityPlayerMP> oldWatchers = null;
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                PlayerChunkMapEntry entry = playerManager.getEntry(x, z);
                if (entry != null) {
                    Field fieldPlayers = PlayerChunkMap.class.getDeclaredField("players");
                    fieldPlayers.setAccessible(true);
                    oldWatchers = (List<EntityPlayerMP>) fieldPlayers.get(entry);
                    playerManager.removeEntry(entry);
                }
                mcChunk.onChunkUnload();
            }
            try {
                Field droppedChunksSetField = chunkServer.getClass().getDeclaredField("field_73248_b");
                droppedChunksSetField.setAccessible(true);
                Set droppedChunksSet = (Set) droppedChunksSetField.get(chunkServer);
                droppedChunksSet.remove(pos);
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
            chunkServer.id2ChunkMap.remove(pos);
            mcChunk = gen.provideChunk(x, z);
            chunkServer.id2ChunkMap.put(pos, mcChunk);
            if (mcChunk != null) {
                mcChunk.onChunkLoad();
                mcChunk.populateChunk(chunkServer, chunkServer.chunkGenerator);
            }
            if (oldWatchers != null) {
                for (EntityPlayerMP player : oldWatchers) {
                    playerManager.addPlayer(player);
                }
            }
            return true;
        } catch (Throwable t) {
            MainUtil.handleError(t);
            return false;
        }
    }

    @Override
    public boolean loadChunk(World world, int x, int z, boolean generate) {
        return getCachedSections(world, x, z) != null;
    }

    @Override
    public ExtendedBlockStorage[] getCachedSections(World world, int x, int z) {
        Chunk chunk = world.getChunkProvider().provideChunk(x, z);
        if (chunk != null && !chunk.isLoaded()) {
            chunk.onChunkLoad();
        }
        return chunk == null ? null : chunk.getBlockStorageArray();
    }

    @Override
    public ExtendedBlockStorage getCachedSection(ExtendedBlockStorage[] chunk, int cy) {
        return chunk[cy];
    }

    @Override
    public int getCombinedId4Data(ExtendedBlockStorage section, int x, int y, int z) {
        IBlockState ibd = section.getData().get(x & 15, y & 15, z & 15);
        Block block = ibd.getBlock();
        int id = Block.getIdFromBlock(block);
        if (FaweCache.hasData(id)) {
            return (id << 4) + block.getMetaFromState(ibd);
        } else {
            return id << 4;
        }
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.getChunkProvider().getLoadedChunk(x, z) != null;
    }

    public void setCount(int tickingBlockCount, int nonEmptyBlockCount, ExtendedBlockStorage section) throws NoSuchFieldException, IllegalAccessException {
        Class<? extends ExtendedBlockStorage> clazz = section.getClass();
        Field fieldTickingBlockCount = clazz.getDeclaredField("field_76683_c");
        Field fieldNonEmptyBlockCount = clazz.getDeclaredField("field_76682_b");
        fieldTickingBlockCount.setAccessible(true);
        fieldNonEmptyBlockCount.setAccessible(true);
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, ExtendedBlockStorage[] sections, Map<?, ?> tilesGeneric, Collection<?>[] entitiesGeneric, Set<UUID> createdEntities, boolean all) throws Exception {
        Map<BlockPos, TileEntity> tiles = (Map<BlockPos, TileEntity>) tilesGeneric;
        ClassInheritanceMultiMap<Entity>[] entities = (ClassInheritanceMultiMap<Entity>[]) entitiesGeneric;
        CharFaweChunk previous = (CharFaweChunk) getFaweChunk(fs.getX(), fs.getZ());
        char[][] idPrevious = new char[16][];
        for (int layer = 0; layer < sections.length; layer++) {
            if (fs.getCount(layer) != 0 || all) {
                ExtendedBlockStorage section = sections[layer];
                if (section != null) {
                    short solid = 0;
                    char[] previousLayer = idPrevious[layer] = new char[4096];
                    BlockStateContainer blocks = section.getData();
                    for (int j = 0; j < 4096; j++) {
                        int x = FaweCache.CACHE_X[0][j];
                        int y = FaweCache.CACHE_Y[0][j];
                        int z = FaweCache.CACHE_Z[0][j];
                        IBlockState ibd = blocks.get(x, y, z);
                        Block block = ibd.getBlock();
                        int combined = Block.getIdFromBlock(block);
                        if (FaweCache.hasData(combined)) {
                            combined = (combined << 4) + block.getMetaFromState(ibd);
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
        if (tiles != null) {
            for (Map.Entry<BlockPos, TileEntity> entry : tiles.entrySet()) {
                TileEntity tile = entry.getValue();
                NBTTagCompound tag = new NBTTagCompound();
                tile.readFromNBT(tag); // readTileEntityIntoTag
                BlockPos pos = entry.getKey();
                CompoundTag nativeTag = (CompoundTag) methodToNative.invoke(null, tag);
                previous.setTile(pos.getX(), pos.getY(), pos.getZ(), nativeTag);
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

    private final IBlockState air = Blocks.AIR.getDefaultState();

    @Override
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        ForgeChunk_All fs = (ForgeChunk_All) fc;
        net.minecraft.world.chunk.Chunk nmsChunk = fs.getChunk();
        nmsChunk.setModified(true);
        net.minecraft.world.World nmsWorld = getWorld();
        try {
            boolean flag = !nmsWorld.provider.getHasNoSky();
            // Sections
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            Map<BlockPos, TileEntity> tiles = nmsChunk.getTileEntityMap();
            ClassInheritanceMultiMap<Entity>[] entities = nmsChunk.getEntityLists();


            // Remove entities
            for (int i = 0; i < 16; i++) {
                int count = fs.getCount(i);
                if (count == 0) {
                    continue;
                } else if (count >= 4096) {
                    entities[i] = new ClassInheritanceMultiMap<>(Entity.class);
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
            Set<Map.Entry<BlockPos, TileEntity>> entryset = tiles.entrySet();
            Iterator<Map.Entry<BlockPos, TileEntity>> iterator = entryset.iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, TileEntity> tile = iterator.next();
                BlockPos pos = tile.getKey();
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
                int count = fs.getCount(j);
                if (count == 0) {
                    continue;
                }
                final char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                ExtendedBlockStorage section = sections[j];
                if (section == null) {
                    if (fs.sectionPalettes != null && fs.sectionPalettes[j] != null) {
                        section = sections[j] = new ExtendedBlockStorage(j << 4, flag);
                        setPalette(section, fs.sectionPalettes[j]);
                        setCount(0, count - fs.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = section = new ExtendedBlockStorage(j << 4, flag);
                    }
                } else if (count >= 4096) {
                    if (fs.sectionPalettes != null && fs.sectionPalettes[j] != null) {
                        setPalette(section, fs.sectionPalettes[j]);
                        setCount(0, count - fs.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = section = new ExtendedBlockStorage(j << 4, flag);
                    }
                }
                BlockStateContainer nibble = section.getData();
                int nonEmptyBlockCount = 0;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            char combinedId = array[FaweCache.CACHE_J[y][x][z]];
                            switch (combinedId) {
                                case 0:
                                    IBlockState existing = nibble.get(x, y, z);
                                    if (existing != air) {
                                        nonEmptyBlockCount++;
                                    }
                                    continue;
                                case 1:
                                    nibble.set(x, y, z, air);
                                    continue;
                                default:
                                    nonEmptyBlockCount++;
                                    nibble.set(x, y, z, Block.getBlockById(combinedId >> 4).getStateFromMeta(combinedId & 0xF));
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
                BlockPos pos = new BlockPos(MathMan.unpair16x(pair.pair[0]) + bx, pair.pair[1] & 0xFF, MathMan.unpair16y(pair.pair[0]) + bz); // Set pos
                TileEntity tileEntity = nmsWorld.getTileEntity(pos);
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

    public void setPalette(ExtendedBlockStorage section, BlockStateContainer palette) throws NoSuchFieldException, IllegalAccessException {
        Field fieldSection = ExtendedBlockStorage.class.getDeclaredField("data");
        fieldSection.setAccessible(true);
        fieldSection.set(section, palette);
    }

    @Override
    public void refreshChunk(World world, net.minecraft.world.chunk.Chunk nmsChunk) {
        if (!nmsChunk.isLoaded()) {
            return;
        }
        try {
            ChunkPos pos = nmsChunk.getChunkCoordIntPair();
            WorldServer w = (WorldServer) nmsChunk.getWorld();
            PlayerChunkMap chunkMap = w.getPlayerChunkMap();
            int x = pos.chunkXPos;
            int z = pos.chunkZPos;
            if (!chunkMap.contains(x, z)) {
                return;
            }
            EntityTracker tracker = w.getEntityTracker();
            HashSet<EntityPlayerMP> players = new HashSet<>();
            for (EntityPlayer player : w.playerEntities) {
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
            ClassInheritanceMultiMap<Entity>[] entitieSlices = nmsChunk.getEntityLists();
            IntHashMap<EntityTrackerEntry> entries = null;
            for (Field field : tracker.getClass().getDeclaredFields()) {
                if (field.getType() == IntHashMap.class) {
                    field.setAccessible(true);
                    entries = (IntHashMap<EntityTrackerEntry>) field.get(tracker);
                    break;
                }
            }
            for (ClassInheritanceMultiMap<Entity> slice : entitieSlices) {
                if (slice == null) {
                    continue;
                }
                for (Entity ent : slice) {
                    EntityTrackerEntry entry = entries != null ? entries.lookup(ent.getEntityId()) : null;
                    if (entry == null) {
                        continue;
                    }
                    entities.add(entry);
                    SPacketDestroyEntities packet = new SPacketDestroyEntities(ent.getEntityId());
                    for (EntityPlayerMP player : players) {
                        player.connection.sendPacket(packet);
                    }
                }
            }
            // Send chunks
            SPacketChunkData packet = new SPacketChunkData(nmsChunk, 65535);
            for (EntityPlayerMP player : players) {
                player.connection.sendPacket(packet);
            }
            // send ents
            for (EntityTrackerEntry entry : entities) {
                try {
                    TaskManager.IMP.later(new Runnable() {
                        @Override
                        public void run() {
                            for (EntityPlayerMP player : players) {
                                boolean result = entry.trackingPlayers.remove(player);
                                if (result && entry.getTrackedEntity() != player) {
                                    entry.updatePlayerEntity(player);
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
    public FaweChunk<Chunk> getFaweChunk(int x, int z) {
        return new ForgeChunk_All(this, x, z);
    }

    @Override
    public boolean removeLighting(ExtendedBlockStorage[] sections, RelightMode mode, boolean sky) {
        if (mode == RelightMode.ALL) {
            for (int i = 0; i < sections.length; i++) {
                ExtendedBlockStorage section = sections[i];
                if (section != null) {
                    section.setBlocklightArray(new NibbleArray());
                    if (sky) {
                        section.setSkylightArray(new NibbleArray());
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean hasSky() {
        return !nmsWorld.provider.getHasNoSky();
    }

    @Override
    public void setFullbright(ExtendedBlockStorage[] sections) {
        for (int i = 0; i < sections.length; i++) {
            ExtendedBlockStorage section = sections[i];
            if (section != null) {
                byte[] bytes = section.getSkylightArray().getData();
                Arrays.fill(bytes, (byte) 255);
            }
        }
    }

    @Override
    public void relight(int x, int y, int z) {
        pos.setPos(x, y, z);
        nmsWorld.checkLight(pos);
    }

    private WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        if (nmsWorld != null) {
            return nmsWorld;
        }
        String[] split = getWorldName().split(";");
        int id = Integer.parseInt(split[split.length - 1]);
        nmsWorld = DimensionManager.getWorld(id);
        return nmsWorld;
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
        BlockStateContainer dataPalette = section.getData();
        IBlockState ibd = dataPalette.get(x & 15, y & 15, z & 15);
        return ibd.getLightOpacity();
    }

    @Override
    public int getBrightness(ExtendedBlockStorage section, int x, int y, int z) {
        BlockStateContainer dataPalette = section.getData();
        IBlockState ibd = dataPalette.get(x & 15, y & 15, z & 15);
        return ibd.getLightValue();
    }

    @Override
    public int getOpacityBrightnessPair(ExtendedBlockStorage section, int x, int y, int z) {
        BlockStateContainer dataPalette = section.getData();
        IBlockState ibd = dataPalette.get(x & 15, y & 15, z & 15);
        return MathMan.pair16(ibd.getLightOpacity(), ibd.getLightValue());
    }

    @Override
    public void relightBlock(int x, int y, int z) {
        pos.setPos(x, y, z);
        nmsWorld.checkLightFor(EnumSkyBlock.BLOCK, pos);
    }

    @Override
    public void relightSky(int x, int y, int z) {
        pos.setPos(x, y, z);
        nmsWorld.checkLightFor(EnumSkyBlock.SKY, pos);
    }

    @Override
    public File getSaveFolder() {
        return new File(((WorldServer) getWorld()).getChunkSaveLocation(), "region");
    }
}

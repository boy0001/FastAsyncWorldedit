package com.boydti.fawe.forge.v1710;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.forge.ForgePlayer;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.brush.visualization.VisualChunk;
import com.boydti.fawe.object.number.LongAdder;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.world.biome.BaseBiome;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
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

    protected static Method methodFromNative;
    protected static Method methodToNative;
    protected static ExtendedBlockStorage emptySection;

    public ForgeQueue_All(com.sk89q.worldedit.world.World world) {
        super(world);
        getImpWorld();
    }

    public ForgeQueue_All(String world) {
        super(world);
        getImpWorld();
    }

    static {
        try {
            emptySection = new ExtendedBlockStorage(0, true);
            Class<?> converter = Class.forName("com.sk89q.worldedit.forge.NBTConverter");
            methodFromNative = converter.getDeclaredMethod("toNative", Tag.class);
            methodToNative = converter.getDeclaredMethod("fromNative", NBTBase.class);
            methodFromNative.setAccessible(true);
            methodToNative.setAccessible(true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveChunk(Chunk chunk) {
        chunk.setChunkModified();
    }

    @Override
    public void setHeightMap(FaweChunk chunk, byte[] heightMap) {
        Chunk forgeChunk = (Chunk) chunk.getChunk();
        if (forgeChunk != null) {
            int[] otherMap = forgeChunk.heightMap;
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
    public CompoundTag getTileEntity(Chunk chunk, int x, int y, int z) {
        Map<ChunkPosition, TileEntity> tiles = chunk.chunkTileEntityMap;
        ChunkPosition pos = new ChunkPosition(x, y, z);
        TileEntity tile = tiles.get(pos);
        return tile != null ? getTag(tile) : null;
    }

    public CompoundTag getTag(TileEntity tile) {
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag); // readTagIntoEntity
            return (CompoundTag) methodToNative.invoke(null, tag);
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    @Override
    public ExtendedBlockStorage[] getSections(Chunk chunk) {
        return chunk.getBlockStorageArray();
    }

    @Override
    public int getBiome(Chunk chunk, int x, int z) {
        return chunk.getBiomeArray()[((z & 15) << 4) + (x & 15)];
    }

    @Override
    public Chunk loadChunk(World world, int x, int z, boolean generate) {
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        if (generate) {
            return provider.originalLoadChunk(x, z);
        } else {
            return provider.loadChunk(x, z);
        }
    }

    @Override
    public ExtendedBlockStorage[] getCachedSections(World world, int cx, int cz) {
        Chunk chunk = (Chunk) ((ChunkProviderServer)world.getChunkProvider()).loadedChunkHashMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cx, cz));
        if (chunk != null) {
            return chunk.getBlockStorageArray();
        }
        return null;
    }

    @Override
    public Chunk getCachedChunk(World world, int cx, int cz) {
        return (Chunk) ((ChunkProviderServer)world.getChunkProvider()).loadedChunkHashMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cx, cz));
    }

    @Override
    public ExtendedBlockStorage getCachedSection(ExtendedBlockStorage[] ExtendedBlockStorages, int cy) {
        return ExtendedBlockStorages[cy];
    }

    @Override
    public int getCombinedId4Data(ExtendedBlockStorage ls, int x, int y, int z) {
        byte[] ids = ls.getBlockLSBArray();
        NibbleArray datasNibble = ls.getBlockMSBArray();
        int i = FaweCache.CACHE_J[y & 15][z & 15][x & 15];
        int combined = ((ids[i] & 0xFF) << 4) + (datasNibble == null ? 0 : datasNibble.get(x & 15, y & 15, z & 15));
        return combined;
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z, BaseBiome biome, Long seed) {
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

    protected final RunnableVal<IntegerPair> loadChunk = new RunnableVal<IntegerPair>() {
        @Override
        public void run(IntegerPair loc) {
            Chunk chunk = getWorld().getChunkProvider().provideChunk(loc.x, loc.z);
            if (chunk != null && !chunk.isChunkLoaded) {
                chunk.onChunkLoad();
            }

        }
    };

    @Override
    public void sendChunk(int x, int z, int bitMask) {
        Chunk chunk = getCachedChunk(getWorld(), x, z);
        if (chunk != null) {
            sendChunk(chunk, bitMask);
        }
    }

    @Override
    public void refreshChunk(FaweChunk fc) {
        Chunk chunk = getCachedChunk(getWorld(), fc.getX(), fc.getZ());
        if (chunk != null) {
            sendChunk(chunk, fc.getBitMask());
        }
    }

    public void sendChunk(Chunk nmsChunk, int mask) {
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
            boolean empty = false;
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            for (int i = 0; i < sections.length; i++) {
                if (sections[i] == null) {
                    sections[i] = emptySection;
                    empty = true;
                }
            }
            if (mask == 0 || mask == 65535 && hasEntities(nmsChunk)) {
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
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {
        try {
            PlayerManager playerManager = ((WorldServer) getWorld()).getPlayerManager();
            boolean watching = false;
            boolean[] watchingArr = new boolean[players.length];
            for (int i = 0; i < players.length; i++) {
                EntityPlayerMP player = (EntityPlayerMP) ((ForgePlayer) players[i]).parent;
                if (playerManager.isPlayerWatchingChunk(player, chunk.getX(), chunk.getZ())) {
                    watchingArr[i] = true;
                    watching = true;
                }
            }
            if (!watching) return;
            final LongAdder size = new LongAdder();
            if (chunk instanceof VisualChunk) {
                size.add(((VisualChunk) chunk).size());
            } else if (chunk instanceof CharFaweChunk) {
                size.add(((CharFaweChunk) chunk).getTotalCount());
            } else {
                chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                    @Override
                    public void run(int localX, int y, int localZ, int combined) {
                        size.add(1);
                    }
                });
            }
            if (size.intValue() == 0) return;
            S22PacketMultiBlockChange packet = new S22PacketMultiBlockChange();
            ByteBuf byteBuf = new UnpooledByteBufAllocator(true).buffer();
            final PacketBuffer buffer = new PacketBuffer(byteBuf);
            buffer.writeInt(chunk.getX());
            buffer.writeInt(chunk.getZ());
            buffer.writeVarIntToBuffer(size.intValue());
            chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                @Override
                public void run(int localX, int y, int localZ, int combined) {
                    short index = (short) (localX << 12 | localZ << 8 | y);
                    buffer.writeShort(index);
                    buffer.writeVarIntToBuffer(combined);
                }
            });
            packet.readPacketData(buffer);
            for (int i = 0; i < players.length; i++) {
                if (watchingArr[i]) ((EntityPlayerMP) ((ForgePlayer) players[i]).parent).playerNetServerHandler.sendPacket(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                        byte data = (byte) (currentDataArray != null ? currentDataArray.get(x, y & 15, z) : 0);
                        previous.setBlock(x, y, z, id, data);
                    }
                }
            }
        }
        if (tiles != null) {
            for (Map.Entry<ChunkPosition, TileEntity> entry : tiles.entrySet()) {
                TileEntity tile = entry.getValue();
                NBTTagCompound tag = new NBTTagCompound();
                tile.writeToNBT(tag); // readTileEntityIntoTag
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

    protected WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        if (nmsWorld != null || getWorldName() == null) {
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

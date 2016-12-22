package com.boydti.fawe.forge.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.forge.ForgePlayer;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.world.biome.BaseBiome;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
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

    protected final static Method methodFromNative;
    protected final static Method methodToNative;
    protected final static Field fieldTickingBlockCount;
    protected final static Field fieldNonEmptyBlockCount;

    static {
        try {
            Class<?> converter = Class.forName("com.sk89q.worldedit.forge.NBTConverter");
            methodFromNative = converter.getDeclaredMethod("toNative", Tag.class);
            methodToNative = converter.getDeclaredMethod("fromNative", NBTBase.class);
            methodFromNative.setAccessible(true);
            methodToNative.setAccessible(true);

            fieldTickingBlockCount = ExtendedBlockStorage.class.getDeclaredField("field_76683_c");
            fieldNonEmptyBlockCount = ExtendedBlockStorage.class.getDeclaredField("field_76682_b");
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount.setAccessible(true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public ForgeQueue_All(com.sk89q.worldedit.world.World world) {
        super(world);
        getImpWorld();
    }

    public ForgeQueue_All(String world) {
        super(world);
        getImpWorld();
    }

    @Override
    public void setHeightMap(FaweChunk chunk, byte[] heightMap) {
        Chunk forgeChunk = (Chunk) chunk.getChunk();
        if (forgeChunk != null) {
            int[] otherMap = forgeChunk.getHeightMap();
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
    public void sendBlockUpdate(Map<Long, Map<Short, Character>> blockMap, FawePlayer... players) {
        for (Map.Entry<Long, Map<Short, Character>> chunkEntry : blockMap.entrySet()) {
            try {
                long chunkHash = chunkEntry.getKey();
                Map<Short, Character> blocks = chunkEntry.getValue();
                SPacketMultiBlockChange packet = new SPacketMultiBlockChange();
                int cx = MathMan.unpairIntX(chunkHash);
                int cz = MathMan.unpairIntY(chunkHash);
                ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
                PacketBuffer buffer = new PacketBuffer(byteBuf);
                buffer.writeInt(cx);
                buffer.writeInt(cz);
                buffer.writeVarIntToBuffer(blocks.size());
                for (Map.Entry<Short, Character> blockEntry : blocks.entrySet()) {
                    buffer.writeShort(blockEntry.getKey());
                    buffer.writeVarIntToBuffer(blockEntry.getValue());
                }
                packet.readPacketData(buffer);
                for (FawePlayer player : players) {
                    ((ForgePlayer) player).parent.connection.sendPacket(packet);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    protected BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0, 0, 0);

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
            tile.writeToNBT(tag); // readTagIntoEntity
            CompoundTag result = (CompoundTag) methodToNative.invoke(null, tag);
            return result;
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
    public boolean regenerateChunk(World world, int x, int z, BaseBiome biome, Long seed) {
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
                    Field fieldPlayers = PlayerChunkMapEntry.class.getDeclaredField("field_187283_c");
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

    public int getNonEmptyBlockCount(ExtendedBlockStorage section) throws IllegalAccessException {
        return (int) fieldNonEmptyBlockCount.get(section);
    }

    public void setCount(int tickingBlockCount, int nonEmptyBlockCount, ExtendedBlockStorage section) throws NoSuchFieldException, IllegalAccessException {
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, ExtendedBlockStorage[] sections, Map<?, ?> tilesGeneric, Collection<?>[] entitiesGeneric, Set<UUID> createdEntities, boolean all) throws Exception {
        Map<BlockPos, TileEntity> tiles = (Map<BlockPos, TileEntity>) tilesGeneric;
        ClassInheritanceMultiMap<Entity>[] entities = (ClassInheritanceMultiMap<Entity>[]) entitiesGeneric;
        CharFaweChunk previous = (CharFaweChunk) getFaweChunk(fs.getX(), fs.getZ());
        char[][] idPrevious = previous.getCombinedIdArrays();
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
        if (tiles != null) {
            for (Map.Entry<BlockPos, TileEntity> entry : tiles.entrySet()) {
                TileEntity tile = entry.getValue();
                NBTTagCompound tag = new NBTTagCompound();
                tile.writeToNBT(tag); // readTileEntityIntoTag
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

    protected final static IBlockState air = Blocks.AIR.getDefaultState();

    public void setPalette(ExtendedBlockStorage section, BlockStateContainer palette) throws NoSuchFieldException, IllegalAccessException {
        Field fieldSection = ExtendedBlockStorage.class.getDeclaredField("data");
        fieldSection.setAccessible(true);
        fieldSection.set(section, palette);
    }

    @Override
    public void refreshChunk(FaweChunk fc) {
        ForgeChunk_All fs = (ForgeChunk_All) fc;
        ensureChunkLoaded(fc.getX(), fc.getZ());
        Chunk nmsChunk = fs.getChunk();
        if (!nmsChunk.isLoaded()) {
            return;
        }
        try {
            ChunkPos pos = nmsChunk.getChunkCoordIntPair();
            WorldServer w = (WorldServer) nmsChunk.getWorld();
            PlayerChunkMap chunkMap = w.getPlayerChunkMap();
            int x = pos.chunkXPos;
            int z = pos.chunkZPos;
            PlayerChunkMapEntry chunkMapEntry = chunkMap.getEntry(x, z);
            if (chunkMapEntry == null) {
                return;
            }
            final ArrayDeque<EntityPlayerMP> players = new ArrayDeque<>();
            chunkMapEntry.hasPlayerMatching(input -> {
                players.add(input);
                return false;
            });
            int mask = fc.getBitMask();
            if (mask == 0 || mask == 65535 && hasEntities(nmsChunk)) {
                SPacketChunkData packet = new SPacketChunkData(nmsChunk, 65280);
                for (EntityPlayerMP player : players) {
                    player.connection.sendPacket(packet);
                }
                mask = 255;
            }
            SPacketChunkData packet = new SPacketChunkData(nmsChunk, mask);
            for (EntityPlayerMP player : players) {
                player.connection.sendPacket(packet);
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    public boolean hasEntities(Chunk nmsChunk) {
        ClassInheritanceMultiMap<Entity>[] entities = nmsChunk.getEntityLists();
        for (int i = 0; i < entities.length; i++) {
            ClassInheritanceMultiMap<Entity> slice = entities[i];
            if (slice != null && !slice.isEmpty()) {
                return true;
            }
        }
        return false;
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

    protected WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        if (nmsWorld != null || getWorldName() == null) {
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

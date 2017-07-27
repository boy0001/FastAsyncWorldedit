package com.boydti.fawe.sponge.v1_12;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

import org.spongepowered.api.Sponge;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.visualization.VisualChunk;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.sponge.SpongePlayer;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.sponge.SpongeWorldEdit;
import com.sk89q.worldedit.sponge.adapter.SpongeImplAdapter;
import com.sk89q.worldedit.world.biome.BaseBiome;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
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
import net.minecraft.world.biome.BiomeCache;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.WorldInfo;

public class SpongeQueue_1_12 extends NMSMappedFaweQueue<World, net.minecraft.world.chunk.Chunk, ExtendedBlockStorage[], ExtendedBlockStorage> {

    protected final static Method methodFromNative;
    protected final static Method methodToNative;
    protected final static Field fieldTickingBlockCount;
    protected final static Field fieldNonEmptyBlockCount;

    protected final static Field fieldId2ChunkMap;
    protected final static Field fieldChunkGenerator;

    protected static Field fieldBiomes;
    protected static Field fieldSeed;
    protected static Field fieldBiomeCache;
    protected static Field fieldBiomes2;
    protected static Field fieldGenLayer1;
    protected static Field fieldGenLayer2;
    protected static ExtendedBlockStorage emptySection;

    private static MutableGenLayer genLayer;

    protected static final SpongeImplAdapter adapter;

    static {
        try {
            emptySection = new ExtendedBlockStorage(0, true);
            adapter = SpongeWorldEdit.inst().getAdapter();
            methodFromNative = adapter.getClass().getDeclaredMethod("toNative", Tag.class);
            methodToNative = adapter.getClass().getDeclaredMethod("fromNative", NBTBase.class);
            methodFromNative.setAccessible(true);
            methodToNative.setAccessible(true);

            fieldId2ChunkMap = ChunkProviderServer.class.getDeclaredField("field_73244_f");
            fieldChunkGenerator = ChunkProviderServer.class.getDeclaredField("field_186029_c");
            fieldId2ChunkMap.setAccessible(true);
            fieldChunkGenerator.setAccessible(true);

            fieldBiomes = ChunkGeneratorOverworld.class.getDeclaredField("field_185981_C"); // biomesForGeneration
            fieldBiomes.setAccessible(true);
            fieldSeed = WorldInfo.class.getDeclaredField("field_76100_a"); // randomSeed
            fieldSeed.setAccessible(true);
            fieldBiomeCache = BiomeProvider.class.getDeclaredField("field_76942_f"); // biomeCache
            fieldBiomeCache.setAccessible(true);
            fieldBiomes2 = BiomeProvider.class.getDeclaredField("field_76943_g"); // biomesToSpawnIn
            fieldBiomes2.setAccessible(true);
            fieldGenLayer1 = BiomeProvider.class.getDeclaredField("field_76944_d"); // genBiomes
            fieldGenLayer2 = BiomeProvider.class.getDeclaredField("field_76945_e"); // biomeIndexLayer
            fieldGenLayer1.setAccessible(true);
            fieldGenLayer2.setAccessible(true);

            fieldTickingBlockCount = ExtendedBlockStorage.class.getDeclaredField("field_76683_c");
            fieldNonEmptyBlockCount = ExtendedBlockStorage.class.getDeclaredField("field_76682_b");
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount.setAccessible(true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public SpongeQueue_1_12(com.sk89q.worldedit.world.World world) {
        super(world);
        getImpWorld();
    }

    public SpongeQueue_1_12(String world) {
        super(world);
        getImpWorld();
    }

    @Override
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {
        try {
            PlayerChunkMap playerManager = ((WorldServer) getWorld()).getPlayerChunkMap();
            boolean watching = false;
            for (int i = 0; i < players.length; i++) {
                EntityPlayerMP player = (EntityPlayerMP) ((SpongePlayer) players[i]).parent;
                if (!playerManager.isPlayerWatchingChunk(player, chunk.getX(), chunk.getZ())) {
                    players[i] = null;
                } else {
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
            SPacketMultiBlockChange packet = new SPacketMultiBlockChange();
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
            final PacketBuffer buffer = new PacketBuffer(byteBuf);
            buffer.writeInt(chunk.getX());
            buffer.writeInt(chunk.getZ());
            buffer.writeVarInt(size.intValue());
            chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                @Override
                public void run(int localX, int y, int localZ, int combined) {
                    short index = (short) (localX << 12 | localZ << 8 | y);
                    buffer.writeShort(index);
                    buffer.writeVarInt(combined);
                }
            });
            packet.readPacketData(buffer);
            for (FawePlayer player : players) {
                if (player != null) ((EntityPlayerMP) ((SpongePlayer) player).parent).connection.sendPacket(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveChunk(Chunk chunk) {
        chunk.setModified(true);
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
            return provider.provideChunk(x, z);
        } else {
            return provider.loadChunk(x, z);
        }
    }

    @Override
    public ExtendedBlockStorage[] getCachedSections(World world, int cx, int cz) {
        Chunk chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
        if (chunk != null) {
            return chunk.getBlockStorageArray();
        }
        return null;
    }

    @Override
    public Chunk getCachedChunk(World world, int cx, int cz) {
        return world.getChunkProvider().getLoadedChunk(cx, cz);
    }

    @Override
    public ExtendedBlockStorage getCachedSection(ExtendedBlockStorage[] ExtendedBlockStorages, int cy) {
        return ExtendedBlockStorages[cy];
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
            CompoundTag result = (CompoundTag) methodToNative.invoke(SpongeQueue_1_12.adapter, tag);
            return result;
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    public boolean regenerateChunk(net.minecraft.world.World world, int x, int z) {
        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return false;
        }
        BlockFalling.fallInstantly = true;
        try {
            ChunkProviderServer chunkServer = (ChunkProviderServer) provider;
            IChunkGenerator gen = (IChunkGenerator) fieldChunkGenerator.get(chunkServer);
            long pos = ChunkPos.asLong(x, z);
            Chunk mcChunk;
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                mcChunk.onUnload();
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
                mcChunk.onUnload();
            }
            try {
                Field droppedChunksSetField = chunkServer.getClass().getDeclaredField("field_73248_b");
                droppedChunksSetField.setAccessible(true);
                Set droppedChunksSet = (Set) droppedChunksSetField.get(chunkServer);
                droppedChunksSet.remove(pos);
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
            Long2ObjectMap<Chunk> id2ChunkMap = (Long2ObjectMap<Chunk>) fieldId2ChunkMap.get(chunkServer);
            id2ChunkMap.remove(pos);
            mcChunk = gen.generateChunk(x, z);
            id2ChunkMap.put(pos, mcChunk);
            if (mcChunk != null) {
                mcChunk.onLoad();
                mcChunk.populate(chunkServer, gen);
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
        } finally {
            BlockFalling.fallInstantly = false;
        }
    }

    @Override
    public boolean regenerateChunk(net.minecraft.world.World world, int x, int z, BaseBiome biome, Long seed) {
        if (biome != null) {
            try {
                if (seed == null) {
                    seed = world.getSeed();
                }
                nmsWorld.getWorldInfo().getSeed();
                boolean result;
                ChunkGeneratorOverworld generator = new ChunkGeneratorOverworld(nmsWorld, seed, false, "");
                net.minecraft.world.biome.Biome base = net.minecraft.world.biome.Biome.getBiome(biome.getId());
                net.minecraft.world.biome.Biome[] existingBiomes = new net.minecraft.world.biome.Biome[256];
                Arrays.fill(existingBiomes, base);
                fieldBiomes.set(generator, existingBiomes);
                boolean cold = base.getTemperature() <= 1;
                IChunkGenerator existingGenerator = (IChunkGenerator) fieldChunkGenerator.get(nmsWorld.getChunkProvider());
                long existingSeed = world.getSeed();
                {
                    if (genLayer == null) genLayer = new MutableGenLayer(seed);
                    genLayer.set(biome.getId());
                    Object existingGenLayer1 = fieldGenLayer1.get(nmsWorld.provider.getBiomeProvider());
                    Object existingGenLayer2 = fieldGenLayer2.get(nmsWorld.provider.getBiomeProvider());
                    fieldGenLayer1.set(nmsWorld.provider.getBiomeProvider(), genLayer);
                    fieldGenLayer2.set(nmsWorld.provider.getBiomeProvider(), genLayer);

                    fieldSeed.set(nmsWorld.getWorldInfo(), seed);

                    ReflectionUtils.setFailsafeFieldValue(fieldBiomeCache, this.nmsWorld.provider.getBiomeProvider(), new BiomeCache(this.nmsWorld.provider.getBiomeProvider()));

                    ReflectionUtils.setFailsafeFieldValue(fieldChunkGenerator, this.nmsWorld.getChunkProvider(), generator);

                    result = regenerateChunk(world, x, z);

                    ReflectionUtils.setFailsafeFieldValue(fieldChunkGenerator, this.nmsWorld.getChunkProvider(), existingGenerator);

                    fieldSeed.set(nmsWorld.getWorldInfo(), existingSeed);

                    fieldGenLayer1.set(nmsWorld.provider.getBiomeProvider(), existingGenLayer1);
                    fieldGenLayer2.set(nmsWorld.provider.getBiomeProvider(), existingGenLayer2);
                }
                return result;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return regenerateChunk(world, x, z);
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
                CompoundTag nativeTag = (CompoundTag) methodToNative.invoke(SpongeQueue_1_12.adapter, tag);
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
                            NBTTagCompound tag = new NBTTagCompound();
                            ent.writeToNBT(tag);  // readEntityIntoTag
                            CompoundTag nativeTag = (CompoundTag) methodToNative.invoke(SpongeQueue_1_12.adapter, tag);
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
        if (!nmsChunk.isLoaded()) {
            return;
        }
        try {
            ChunkPos pos = nmsChunk.getPos();
            WorldServer w = (WorldServer) nmsChunk.getWorld();
            PlayerChunkMap chunkMap = w.getPlayerChunkMap();
            int x = pos.x;
            int z = pos.z;
            PlayerChunkMapEntry chunkMapEntry = chunkMap.getEntry(x, z);
            if (chunkMapEntry == null) {
                return;
            }
            final ArrayDeque<EntityPlayerMP> players = new ArrayDeque<>();
            chunkMapEntry.hasPlayerMatching(input -> {
                players.add(input);
                return false;
            });
            boolean empty = false;
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            for (int i = 0; i < sections.length; i++) {
                if (sections[i] == null) {
                    sections[i] = emptySection;
                    empty = true;
                }
            }
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
        return new SpongeChunk_1_12(this, x, z);
    }

    @Override
    public boolean removeLighting(ExtendedBlockStorage[] sections, RelightMode mode, boolean sky) {
        if (mode == RelightMode.ALL) {
            for (int i = 0; i < sections.length; i++) {
                ExtendedBlockStorage section = sections[i];
                if (section != null) {
                    section.setBlockLight(new NibbleArray());
                    if (sky) {
                        section.setSkyLight(new NibbleArray());
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean hasSky() {
        return nmsWorld.provider.hasSkyLight();
    }

    @Override
    public void setFullbright(ExtendedBlockStorage[] sections) {
        for (int i = 0; i < sections.length; i++) {
            ExtendedBlockStorage section = sections[i];
            if (section != null) {
                byte[] bytes = section.getSkyLight().getData();
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
    public net.minecraft.world.World getImpWorld() {
        if (nmsWorld != null || getWorldName() == null) {
            return nmsWorld;
        }
        nmsWorld = (WorldServer) Sponge.getServer().getWorld(getWorldName()).get();
        return nmsWorld;
    }

    @Override
    public void setSkyLight(ExtendedBlockStorage section, int x, int y, int z, int value) {
        section.getSkyLight().set(x & 15, y & 15, z & 15, value);
    }

    @Override
    public void setBlockLight(ExtendedBlockStorage section, int x, int y, int z, int value) {
        section.getBlockLight().set(x & 15, y & 15, z & 15, value);
    }

    @Override
    public int getSkyLight(ExtendedBlockStorage section, int x, int y, int z) {
        return section.getSkyLight(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmmittedLight(ExtendedBlockStorage section, int x, int y, int z) {
        return section.getBlockLight(x & 15, y & 15, z & 15);
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
        return new File(((WorldServer) getWorld()).getSaveHandler().getWorldDirectory(), "region");
    }
}

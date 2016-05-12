package com.boydti.fawe.sponge.v1_8;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.PseudoRandom;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.util.BlockPos;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.UnmodifiableBlockVolume;
import org.spongepowered.api.world.extent.worker.MutableBlockVolumeWorker;
import org.spongepowered.api.world.extent.worker.procedure.BlockVolumeMapper;

public class SpongeQueue_ALL extends NMSMappedFaweQueue<World, net.minecraft.world.chunk.Chunk, ExtendedBlockStorage[], char[]> {
    public SpongeQueue_ALL(String world) {
        super(world);
    }

    @Override
    public void refreshChunk(World world, net.minecraft.world.chunk.Chunk chunk) {
        if (!chunk.isLoaded()) {
            return;
        }
        int cx = chunk.xPosition;
        int cz = chunk.zPosition;
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            int view = player.getViewDistance();
            EntityPlayerMP nmsPlayer = (EntityPlayerMP) player;
            Location<World> loc = player.getLocation();
            int px = loc.getBlockX() >> 4;
            int pz = loc.getBlockZ() >> 4;
            int dx = Math.abs(cx - (loc.getBlockX() >> 4));
            int dz = Math.abs(cz - (loc.getBlockZ() >> 4));
            if ((dx > view) || (dz > view)) {
                continue;
            }
            NetHandlerPlayServer con = nmsPlayer.playerNetServerHandler;
            net.minecraft.world.chunk.Chunk  nmsChunk = (net.minecraft.world.chunk.Chunk) chunk;
            con.sendPacket(new S21PacketChunkData(nmsChunk, false, 65535));
            // Try sending true, 0 first
            // Try bulk chunk packet
        }
    }

    @Override
    public char[] getCachedSection(ExtendedBlockStorage[] chunk, int cy) {
        ExtendedBlockStorage value = chunk[cy];
        return value == null ? null : value.getData();
    }

    @Override
    public World getWorld(String world) {
        return Sponge.getServer().getWorld(this.world).get();
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        Chunk chunk = world.getChunk(x << 4, 0, z << 4).orElse(null);
        return chunk != null && chunk.isLoaded();
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z) {
        try {
            net.minecraft.world.World nmsWorld = (net.minecraft.world.World) world;
            IChunkProvider provider = nmsWorld.getChunkProvider();
            if (!(provider instanceof ChunkProviderServer)) {
                return false;
            }
            ChunkProviderServer chunkServer = (ChunkProviderServer) provider;
            Field chunkProviderField = chunkServer.getClass().getDeclaredField("field_73246_d");
            chunkProviderField.setAccessible(true);
            IChunkProvider chunkProvider = (IChunkProvider) chunkProviderField.get(chunkServer);
            long pos = ChunkCoordIntPair.chunkXZ2Int(x, z);
            net.minecraft.world.chunk.Chunk mcChunk;
            if (chunkServer.chunkExists(x, z)) {
                mcChunk = chunkServer.loadChunk(x, z);
                mcChunk.onChunkUnload();
            }
            Field droppedChunksSetField = chunkServer.getClass().getDeclaredField("field_73248_b");
            droppedChunksSetField.setAccessible(true);
            Set droppedChunksSet = (Set) droppedChunksSetField.get(chunkServer);
            droppedChunksSet.remove(pos);
            Field id2ChunkMapField = chunkServer.getClass().getDeclaredField("field_73244_f");
            id2ChunkMapField.setAccessible(true);
            LongHashMap<net.minecraft.world.chunk.Chunk> id2ChunkMap = (LongHashMap<net.minecraft.world.chunk.Chunk>) id2ChunkMapField.get(chunkServer);
            id2ChunkMap.remove(pos);
            mcChunk = chunkProvider.provideChunk(x, z);
            id2ChunkMap.add(pos, mcChunk);
            List<net.minecraft.world.chunk.Chunk> loadedChunks = chunkServer.func_152380_a();
            loadedChunks.add(mcChunk);
            if (mcChunk != null) {
                mcChunk.onChunkLoad();
                mcChunk.populateChunk(chunkProvider, chunkProvider, x, z);
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    private BlockState AIR = BlockTypes.AIR.getDefaultState();

    @Override
    public boolean setComponents(FaweChunk fc) {
        SpongeChunk_1_8 fs = (SpongeChunk_1_8) fc;
        net.minecraft.world.chunk.Chunk nmsChunk = fs.getChunk();
        Chunk spongeChunk = (Chunk) nmsChunk;

        char[][] ids = ((SpongeChunk_1_8) fc).getCombinedIdArrays();
        MutableBlockVolumeWorker<? extends Chunk> blockWorker = spongeChunk.getBlockWorker();
        blockWorker.map(new BlockVolumeMapper() {
            @Override
            public BlockState map(UnmodifiableBlockVolume volume, int xx, int y, int zz) {
                int x = xx & 15;
                int z = zz & 15;
                int i = FaweCache.CACHE_I[y][x][z];
                char[] array = ids[i];
                if (array == null) {
                    return null;
                }
                int combinedId = array[FaweCache.CACHE_J[y][x][z]];
                switch (combinedId) {
                    case 0:
                        return null;
                    case 1:
                        return AIR;
                    default:
                        int id = combinedId >> 4;
                        Block block = Block.getBlockById(id);
                        int data = combinedId & 0xf;
                        IBlockState ibd;
                        if (data != 0) {
                            ibd = block.getStateFromMeta(data);
                        } else {
                            ibd = block.getDefaultState();
                        }
                        return (BlockState) ibd;
                }
            }
        });
        sendChunk(fs);
        return true;
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
    public FaweChunk<net.minecraft.world.chunk.Chunk> getChunk(int x, int z) {
        return new SpongeChunk_1_8(this, x, z);
    }


    @Override
    public boolean fixLighting(FaweChunk fc, boolean fixAll) {
        try {
            SpongeChunk_1_8 bc = (SpongeChunk_1_8) fc;
            net.minecraft.world.chunk.Chunk nmsChunk = bc.getChunk();
            if (!nmsChunk.isLoaded()) {
                if (!((Chunk) nmsChunk).loadChunk(false)) {
                    return false;
                }
            }
            nmsChunk.generateSkylightMap();
            if (bc.getTotalRelight() == 0 && !fixAll) {
                return true;
            }
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            net.minecraft.world.World nmsWorld = nmsChunk.getWorld();

            int X = bc.getX() << 4;
            int Z = bc.getZ() << 4;


            for (int j = 0; j < sections.length; j++) {
                ExtendedBlockStorage section = sections[j];
                if (section == null) {
                    continue;
                }
                if ((bc.getRelight(j) == 0 && !fixAll) || bc.getCount(j) == 0 || (bc.getCount(j) >= 4096 && bc.getAir(j) == 0)) {
                    continue;
                }
                char[] array = section.getData();
                int l = PseudoRandom.random.random(2);
                for (int k = 0; k < array.length; k++) {
                    int i = array[k];
                    if (i < 16) {
                        continue;
                    }
                    short id = (short) (i >> 4);
                    switch (id) { // Lighting
                        default:
                            if (!fixAll) {
                                continue;
                            }
                            if ((k & 1) == l) {
                                l = 1 - l;
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
                            int x = FaweCache.CACHE_X[j][k];
                            int y = FaweCache.CACHE_Y[j][k];
                            int z = FaweCache.CACHE_Z[j][k];
                            if (isSurrounded(sections, x, y, z)) {
                                continue;
                            }
                            BlockPos pos = new BlockPos(X + x, y, Z + z);
                            nmsWorld.checkLight(pos);
                    }
                }
            }
            return true;
        } catch (Throwable e) {
            if (Thread.currentThread() == Fawe.get().getMainThread()) {
                e.printStackTrace();
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
        char[] array = section.getData();
        int j = FaweCache.CACHE_J[y][x][z];
        return array[j] >> 4;
    }

    @Override
    public boolean loadChunk(World world, int x, int z, boolean generate) {
        return getCachedChunk(world, x, z) != null;
    }

    @Override
    public ExtendedBlockStorage[] getCachedChunk(World world, int cx, int cz) {
        Chunk chunk = world.loadChunk(cx, 0, cz, true).orElse(null);
        return ((net.minecraft.world.chunk.Chunk) chunk).getBlockStorageArray();
    }

    @Override
    public int getCombinedId4Data(char[] chars, int x, int y, int z) {
        return chars[FaweCache.CACHE_J[y][x & 15][z & 15]];
    }
}

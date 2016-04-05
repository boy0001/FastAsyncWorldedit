package com.boydti.fawe.forge.v1_8;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.forge.SpongeUtil;
import com.boydti.fawe.forge.v0.SpongeQueue_0;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.util.TaskManager;
import com.flowpowered.math.vector.Vector3i;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SpongeQueue_1_8 extends SpongeQueue_0 {

    public World spongeWorld;

    public SpongeQueue_1_8(String world) {
        super(world);
    }

    @Override
    public Collection<FaweChunk<Chunk>> sendChunk(Collection<FaweChunk<Chunk>> fcs) {
        if (fcs.isEmpty()) {
            return fcs;
        }
        for (FaweChunk<Chunk> chunk : fcs) {
            sendChunk(chunk);
        }
        fcs.clear();
        return fcs;
    }

    public void sendChunk(FaweChunk<Chunk> fc) {
        fixLighting(fc, Settings.FIX_ALL_LIGHTING);
        Chunk chunk = fc.getChunk();
        if (!chunk.isLoaded()) {
            return;
        }
        World world = chunk.getWorld();
        Vector3i pos = chunk.getBlockMin();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
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

    private int lcx = Integer.MIN_VALUE;
    private int lcz = Integer.MIN_VALUE;
    private int lcy = Integer.MIN_VALUE;
    private net.minecraft.world.chunk.Chunk lc;
    private char[] ls;

    @Override
    public int getCombinedId4Data(int x, int y, int z) {
        if (y < 0 || y > 255) {
            return 0;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lcx || cz != lcz) {
            if (spongeWorld == null) {
                spongeWorld = Sponge.getServer().getWorld(world).get();
            }
            lcx = cx;
            lcz = cz;
            lc = (net.minecraft.world.chunk.Chunk) spongeWorld.getChunk(cx, 0, cz).get();
        } else if (cy == lcy) {
            return ls != null ? ls[FaweCache.CACHE_J[y][x & 15][z & 15]] : 0;
        }
        ExtendedBlockStorage storage = lc.getBlockStorageArray()[cy];
        if (storage == null) {
            ls = null;
            return 0;
        }
        ls = storage.getData();
        return ls[FaweCache.CACHE_J[y][x & 15][z & 15]];
    }

    @Override
    public boolean setComponents(FaweChunk<Chunk> fc) {
        SpongeChunk_1_8 fs = (SpongeChunk_1_8) fc;
        Chunk spongeChunk = fc.getChunk();
        net.minecraft.world.World nmsWorld = (net.minecraft.world.World) spongeChunk.getWorld();
        try {
            boolean flag = !nmsWorld.provider.getHasNoSky();
            // Sections
            net.minecraft.world.chunk.Chunk nmsChunk = (net.minecraft.world.chunk.Chunk) spongeChunk;
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            Map<BlockPos, TileEntity> tiles = nmsChunk.getTileEntityMap();
            ClassInheritanceMultiMap<Entity>[] entities = nmsChunk.getEntityLists();
            // Trim tiles
            Set<Entry<BlockPos, TileEntity>> entryset = tiles.entrySet();
            Iterator<Entry<BlockPos, TileEntity>> iterator = entryset.iterator();
            while (iterator.hasNext()) {
                Entry<BlockPos, TileEntity> tile = iterator.next();
                BlockPos pos = tile.getKey();
                int lx = pos.getX() & 15;
                int ly = pos.getY();
                int lz = pos.getZ() & 15;
                int j = FaweCache.CACHE_I[ly][lx][lz];
                int k = FaweCache.CACHE_J[ly][lx][lz];
                char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                if (array[k] != 0) {
                    iterator.remove();
                }
            }
            // Trim entities
            for (int i = 0; i < 16; i++) {
                if ((entities[i] != null) && (fs.getCount(i) >= 4096)) {
                    entities[i] = new ClassInheritanceMultiMap<>(Entity.class);
                }
            }
            // Efficiently merge sections
            for (int j = 0; j < sections.length; j++) {
                if (fs.getCount(j) == 0) {
                    continue;
                }
                char[] newArray = fs.getIdArray(j);
                if (newArray == null) {
                    continue;
                }
                ExtendedBlockStorage section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    section = new ExtendedBlockStorage(j << 4, flag);
                    section.setData(newArray);
                    sections[j] = section;
                    continue;
                }
                char[] currentArray = section.getData();
                boolean fill = true;
                for (int k = 0; k < newArray.length; k++) {
                    char n = newArray[k];
                    switch (n) {
                        case 0:
                            fill = false;
                            continue;
                        case 1:
                            fill = false;
                            currentArray[k] = 0;
                            continue;
                        default:
                            currentArray[k] = n;
                            continue;
                    }
                }
                if (fill) {
                    fs.setCount(j, Short.MAX_VALUE);
                }
            }
//            // Clear
        } catch (Throwable e) {
            e.printStackTrace();
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
                    spongeChunk.setBiome(x, z, SpongeUtil.getBiome(biome));
                }
            }
        }
        TaskManager.IMP.later(new Runnable() {
            @Override
            public void run() {
                sendChunk(fs);
            }
        }, 1);
        return true;
    }

    /**
     * This should be overridden by any specialized queues.
     * @param x
     * @param z
     */
    @Override
    public SpongeChunk_1_8 getChunk(int x, int z) {
        return new SpongeChunk_1_8(this, x, z);
    }

    @Override
    public boolean fixLighting(FaweChunk<?> pc, boolean fixAll) {
        try {
            SpongeChunk_1_8 bc = (SpongeChunk_1_8) pc;
            Chunk spongeChunk = bc.getChunk();
            net.minecraft.world.chunk.Chunk nmsChunk = (net.minecraft.world.chunk.Chunk) spongeChunk;
            if (!spongeChunk.isLoaded()) {
                if (!spongeChunk.loadChunk(false)) {
                    return false;
                }
            } else {
                spongeChunk.unloadChunk();
                spongeChunk.loadChunk(false);
            }
            nmsChunk.generateSkylightMap();
            if (bc.getTotalRelight() == 0 && !fixAll) {
                return true;
            }
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            net.minecraft.world.World nmsWorld = nmsChunk.getWorld();

            int X = pc.getX() << 4;
            int Z = pc.getZ() << 4;


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
            e.printStackTrace();
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
}

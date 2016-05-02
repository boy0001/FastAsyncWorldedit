package com.boydti.fawe.bukkit.v1_8;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.BukkitPlayer;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.PseudoRandom;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.TileEntity;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

public class BukkitQueue18R3 extends BukkitQueue_0<Chunk, ChunkSection[], char[]> {

    public BukkitQueue18R3(final String world) {
        super(world);
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
        return getCachedChunk(world, x, z) != null;
    }

    @Override
    public ChunkSection[] getCachedChunk(World world, int x, int z) {
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
    public boolean setComponents(FaweChunk fc) {
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
                    entities[i].clear();
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
                ChunkSection section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    section = new ChunkSection(j << 4, flag, newArray);
                    sections[j] = section;
                    continue;
                }
                char[] currentArray = section.getIdArray();
                boolean fill = true;
                int solid = 0;
                for (int k = 0; k < newArray.length; k++) {
                    char n = newArray[k];
                    switch (n) {
                        case 0:
                            fill = false;
                            continue;
                        case 1:
                            fill = false;
                            if (currentArray[k] > 1) {
                                solid++;
                            }
                            currentArray[k] = 0;
                            continue;
                        default:
                            solid++;
                            currentArray[k] = n;
                            continue;
                    }
                }
                setCount(0, solid, section);
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
                    nmsChunk.getBiomeIndex()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome;
                }
            }
        }
        sendChunk(fc);
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
        net.minecraft.server.v1_8_R3.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
        ChunkCoordIntPair pos = nmsChunk.j();
        int cx = pos.x;
        int cz = pos.z;
        int view = Bukkit.getViewDistance();
        for (FawePlayer fp : Fawe.get().getCachedPlayers()) {
            BukkitPlayer bukkitPlayer = (BukkitPlayer) fp;
            if (!bukkitPlayer.getWorld().equals(world)) {
                continue;
            }
            net.minecraft.server.v1_8_R3.EntityPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer.parent).getHandle();
            FaweLocation loc = fp.getLocation();
            int px = loc.x >> 4;
            int pz = loc.z >> 4;
            int dx = Math.abs(cx - (loc.x >> 4));
            int dz = Math.abs(cz - (loc.z >> 4));
            if ((dx > view) || (dz > view)) {
                continue;
            }
            PlayerConnection con = nmsPlayer.playerConnection;
            con.sendPacket(new PacketPlayOutMapChunk(nmsChunk, false, 65535));
        }
    }

    @Override
    public boolean fixLighting(FaweChunk chunk, boolean fixAll) {
        try {
            CharFaweChunk<Chunk> fc = (CharFaweChunk<Chunk>) chunk;
            CraftChunk craftChunk = (CraftChunk) fc.getChunk();
            net.minecraft.server.v1_8_R3.Chunk nmsChunk = craftChunk.getHandle();
            if (!craftChunk.isLoaded()) {
                return false;
            }
            nmsChunk.initLighting();
            if (fc.getTotalRelight() == 0 && !fixAll) {
                return true;
            }
            ChunkSection[] sections = nmsChunk.getSections();
            net.minecraft.server.v1_8_R3.World nmsWorld = nmsChunk.getWorld();

            int X = fc.getX() << 4;
            int Z = fc.getZ() << 4;

            BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition(0, 0, 0);
            for (int j = 0; j < sections.length; j++) {
                ChunkSection section = sections[j];
                if (section == null) {
                    continue;
                }
                if ((fc.getRelight(j) == 0 && !fixAll) || fc.getCount(j) == 0 || (fc.getCount(j) >= 4096 && fc.getAir(j) == 0)) {
                    continue;
                }
                char[] array = section.getIdArray();
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
                            pos.c(X + x, y, Z + z);
                            nmsWorld.x(pos);
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

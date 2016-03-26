package com.boydti.fawe.bukkit.v1_8;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.ChunkLoc;

public class FaweGenerator_1_8 extends ChunkGenerator implements Listener {
    private boolean events;
    
    private final ChunkGenerator parent;
    private final List<BlockPopulator> pops;
    private final Object provider;
    
    private short[][] ids;
    private byte[][] data;
    private Map<?, ?> tiles;
    private List<?>[] entities;
    private Biome[][] biomes;
    
    private final World world;
    
    private final BukkitQueue_1_8 queue;
    
    private void registerEvents() {
        if (events) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, Fawe.<FaweBukkit> imp());
    }
    
    @EventHandler
    private void onPopulate(final ChunkPopulateEvent event) {
        final World world = event.getWorld();
        final ChunkGenerator gen = world.getGenerator();
        if (gen instanceof FaweGenerator_1_8) {
            final FaweGenerator_1_8 fawe = (FaweGenerator_1_8) gen;
            if (fawe.data == null) {
                return;
            }
            fawe.populate(event.getChunk());
            decouple((FaweGenerator_1_8) gen, world);
        }
    }
    
    public void setBlock(final short[][] result, final int x, final int y, final int z, final short blkid) {
        if (result[FaweCache.CACHE_I[y][x][z]] == null) {
            result[FaweCache.CACHE_I[y][x][z]] = new short[4096];
        }
        result[FaweCache.CACHE_I[y][x][z]][FaweCache.CACHE_J[y][x][z]] = blkid;
    }
    
    public void setBlock(final short[][] result, final int x, final int y, final int z, final short[] blkid) {
        if (blkid.length == 1) {
            setBlock(result, x, y, z, blkid[0]);
        }
        final short id = blkid[FaweCache.RANDOM.random(blkid.length)];
        if (result[FaweCache.CACHE_I[y][x][z]] == null) {
            result[FaweCache.CACHE_I[y][x][z]] = new short[4096];
        }
        result[FaweCache.CACHE_I[y][x][z]][FaweCache.CACHE_J[y][x][z]] = id;
    }
    
    public void setBlocks(final short[][] ids, final byte[][] data, final int x, final int z) {
        this.ids = ids;
        this.data = data == null ? new byte[16][] : data;
        if (parent == null) {
            inject(this, world);
        }
        world.regenerateChunk(x, z);
    }
    
    /**
     * Regenerate chunk with the provided id / data / block count<br>
     *  - You can provide null for datas / count but it will be marginally slower
     * @param ids
     * @param datas
     * @param count
     * @param chunk
     */
    @Deprecated
    public void regenerateBlocks(final short[][] ids, byte[][] datas, short[] count, final Chunk chunk) {
        if (datas == null) {
            datas = new byte[16][];
        }
        if (count == null) {
            count = new short[16];
        }
        final int x = chunk.getX();
        final int z = chunk.getZ();
        
        boolean skip = true;
        for (int i = 0; i < 16; i++) {
            if (count[i] < 4096) {
                skip = false;
                break;
            }
        }
        
        if (!skip) {
            try {
                chunk.load(true);
                biomes = new Biome[16][16];
                final int X = x << 4;
                final int Z = z << 4;
                for (int xx = 0; xx < 16; xx++) {
                    final int xxx = X + x;
                    for (int zz = 0; zz < 16; zz++) {
                        final int zzz = Z + zz;
                        biomes[xx][zz] = world.getBiome(xxx, zzz);
                    }
                }
                final Method getHandele = chunk.getClass().getDeclaredMethod("getHandle");
                final Object c = getHandele.invoke(chunk);
                final Class<? extends Object> clazz = c.getClass();
                final Field sf = clazz.getDeclaredField("sections");
                sf.setAccessible(true);
                final Field tf = clazz.getDeclaredField("tileEntities");
                final Field ef = clazz.getDeclaredField("entitySlices");
                
                final Object[] sections = (Object[]) sf.get(c);
                final HashMap<?, ?> tiles = (HashMap<?, ?>) tf.get(c);
                final List<?>[] entities = (List<?>[]) ef.get(c);
                
                Method xm = null;
                Method ym = null;
                Method zm = null;
                
                // Copy entities / blockstates
                final Set<Entry<?, ?>> entryset = (Set<Entry<?, ?>>) (Set<?>) tiles.entrySet();
                final Iterator<Entry<?, ?>> iter = entryset.iterator();
                while (iter.hasNext()) {
                    final Entry<?, ?> tile = iter.next();
                    final Object loc = tile.getKey();
                    if (xm == null) {
                        final Class<? extends Object> clazz2 = loc.getClass().getSuperclass();
                        xm = clazz2.getDeclaredMethod("getX");
                        ym = clazz2.getDeclaredMethod("getY");
                        zm = clazz2.getDeclaredMethod("getZ");
                    }
                    final int lx = (int) xm.invoke(loc) & 15;
                    final int ly = (int) ym.invoke(loc);
                    final int lz = (int) zm.invoke(loc) & 15;
                    final int j = FaweCache.CACHE_I[ly][lx][lz];
                    final int k = FaweCache.CACHE_J[ly][lx][lz];
                    if (ids[j] == null) {
                        continue;
                    }
                    if (ids[j][k] != 0) {
                        iter.remove();
                    }
                }
                
                this.tiles = tiles;
                // Trim entities
                for (int i = 0; i < 16; i++) {
                    if ((entities[i] != null) && (count[i] >= 4096)) {
                        entities[i].clear();
                    }
                }
                this.entities = entities;
                
                // Efficiently merge sections
                Method getIdArray = null;
                for (int j = 0; j < sections.length; j++) {
                    if (count[j] >= 4096) {
                        continue;
                    }
                    final Object section = sections[j];
                    if (section == null) {
                        continue;
                    }
                    if (getIdArray == null) {
                        final Class<? extends Object> clazz2 = section.getClass();
                        getIdArray = clazz2.getDeclaredMethod("getIdArray");
                    }
                    final char[] array = (char[]) getIdArray.invoke(section);
                    for (int k = 0; k < array.length; k++) {
                        final int i = array[k];
                        if (i < 16) {
                            continue;
                        }
                        short[] va = ids[j];
                        if (va == null) {
                            va = new short[4096];
                            ids[j] = va;
                        }
                        final short v = va[k];
                        if (v != 0) {
                            continue;
                        }
                        final short id = FaweCache.CACHE_ID[i];
                        va[k] = id;
                        switch (id) {
                            case 0:
                            case 2:
                            case 4:
                            case 13:
                            case 14:
                            case 15:
                            case 20:
                            case 21:
                            case 22:
                            case 30:
                            case 32:
                            case 37:
                            case 39:
                            case 40:
                            case 41:
                            case 42:
                            case 45:
                            case 46:
                            case 47:
                            case 48:
                            case 49:
                            case 51:
                            case 56:
                            case 57:
                            case 58:
                            case 60:
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 73:
                            case 74:
                            case 78:
                            case 79:
                            case 80:
                            case 81:
                            case 82:
                            case 83:
                            case 85:
                            case 87:
                            case 88:
                            case 101:
                            case 102:
                            case 103:
                            case 110:
                            case 112:
                            case 113:
                            case 121:
                            case 122:
                            case 129:
                            case 133:
                            case 165:
                            case 166:
                            case 169:
                            case 170:
                            case 172:
                            case 173:
                            case 174:
                            case 181:
                            case 182:
                            case 188:
                            case 189:
                            case 190:
                            case 191:
                            case 192:
                                continue;
                            case 53:
                            case 67:
                            case 108:
                            case 109:
                            case 114:
                            case 128:
                            case 134:
                            case 135:
                            case 136:
                            case 156:
                            case 163:
                            case 164:
                            case 180:
                                byte db = FaweCache.CACHE_DATA[i];
                                if (db == 0) {
                                    db = -1;
                                }
                                if (datas[j] == null) {
                                    datas[j] = new byte[4096];
                                }
                                datas[j][k] = db;
                                continue;
                        }
                        final byte db = FaweCache.CACHE_DATA[i];
                        if (db == 0) {
                            continue;
                        }
                        if (datas[j] == null) {
                            datas[j] = new byte[4096];
                        }
                        datas[j][k] = db;
                    }
                }
                
            } catch (final Throwable e) {
                e.printStackTrace();
                return;
            }
        }
        // Execute
        this.ids = ids;
        data = datas;
        if (parent == null) {
            inject(this, world);
        }
        world.regenerateChunk(x, z);
    }
    
    public void inject(final FaweGenerator_1_8 gen, final World world) {
        queue.setGenerator(world, gen);
        queue.setPopulator(world, new ArrayList<BlockPopulator>());
        queue.setProvider(world, null);
    }
    
    public void decouple(final FaweGenerator_1_8 gen, final World world) {
        gen.data = null;
        gen.ids = null;
        gen.tiles = null;
        gen.entities = null;
        gen.biomes = null;
        if (gen.parent == null) {
            queue.setGenerator(world, gen.parent);
            queue.setPopulator(world, gen.pops);
            if (gen.provider != null) {
                queue.setProvider(world, gen.provider);
            }
        }
    }
    
    public FaweGenerator_1_8(final BukkitQueue_1_8 queue, final World world) {
        this.queue = queue;
        this.world = world;
        parent = world.getGenerator();
        pops = world.getPopulators();
        if (parent == null) {
            provider = queue.getProvider(world);
        } else {
            provider = null;
        }
        registerEvents();
    }
    
    @Override
    public short[][] generateExtBlockSections(final World world, final Random random, final int x, final int z, final BiomeGrid biomes) {
        short[][] result;
        if (ids != null) {
            result = ids;
            if ((biomes != null) && (this.biomes != null)) {
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        biomes.setBiome(i, j, this.biomes[i][j]);
                    }
                }
            }
        } else if (parent != null) {
            result = parent.generateExtBlockSections(world, random, x, z, biomes);
        } else {
            result = null;
        }
        return result;
    }
    
    public void populate(final Chunk chunk) {
        for (int i = 0; i < data.length; i++) {
            final byte[] section = data[i];
            if (section == null) {
                continue;
            }
            for (int j = 0; j < section.length; j++) {
                final byte v = section[j];
                if (v == 0) {
                    continue;
                }
                final int x = FaweCache.CACHE_X[i][j];
                final int y = FaweCache.CACHE_Y[i][j];
                final int z = FaweCache.CACHE_Z[i][j];
                chunk.getBlock(x, y, z).setData(v != -1 ? v : 0, false);
            }
        }
        if ((tiles != null) || (entities != null)) {
            queue.setEntitiesAndTiles(chunk, entities, tiles);
        }
        final BukkitChunk_1_8 fc = new BukkitChunk_1_8(new ChunkLoc(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
        fc.chunk = chunk;
        queue.fixLighting(fc, Settings.FIX_ALL_LIGHTING);
    }
    
    @Override
    public byte[] generate(final World world, final Random random, final int x, final int z) {
        if (ids == null) {
            try {
                parent.generate(world, random, x, z);
            } catch (final Throwable e) {
                return null;
            }
        }
        return null;
    }
    
    @Override
    public byte[][] generateBlockSections(final World world, final Random random, final int x, final int z, final BiomeGrid biomes) {
        if ((ids == null) && (parent != null)) {
            return parent.generateBlockSections(world, random, x, z, biomes);
        }
        return null;
    }
    
    @Override
    public boolean canSpawn(final World world, final int x, final int z) {
        if (parent != null) {
            return parent.canSpawn(world, x, z);
        }
        return true;
    }
    
    @Override
    public List<BlockPopulator> getDefaultPopulators(final World world) {
        if ((ids == null) && (parent != null)) {
            return parent.getDefaultPopulators(world);
        }
        return null;
    }
    
    @Override
    public Location getFixedSpawnLocation(final World world, final Random random) {
        if ((ids == null) && (parent != null)) {
            return parent.getFixedSpawnLocation(world, random);
        }
        return null;
    }
}

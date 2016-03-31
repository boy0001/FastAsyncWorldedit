package com.boydti.fawe.bukkit.v1_9;

import static com.boydti.fawe.util.ReflectionUtils.getRefClass;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.ChunkLoc;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.ReflectionUtils.RefClass;
import com.boydti.fawe.util.ReflectionUtils.RefConstructor;
import com.boydti.fawe.util.ReflectionUtils.RefField;
import com.boydti.fawe.util.ReflectionUtils.RefMethod;
import com.boydti.fawe.util.ReflectionUtils.RefMethod.RefExecutor;
import com.intellectualcrafters.plot.util.TaskManager;
import com.sk89q.worldedit.LocalSession;

public class BukkitQueue_1_9 extends BukkitQueue_0 {
    
    private final RefClass classMapChunk = getRefClass("{nms}.PacketPlayOutMapChunk");
    private final RefClass classChunk = getRefClass("{nms}.Chunk");
    private final RefClass classCraftChunk = getRefClass("{cb}.CraftChunk");
    private final RefClass classWorld = getRefClass("{nms}.World");
    private final RefField mustSave = classChunk.getField("mustSave");
    private final RefClass classBlockPosition = getRefClass("{nms}.BlockPosition");
    private final RefClass classChunkSection = getRefClass("{nms}.ChunkSection");
    private final RefClass classBlock = getRefClass("{nms}.Block");
    private final RefClass classIBlockData = getRefClass("{nms}.IBlockData");
    private final RefMethod methodGetHandleChunk;
    private final RefConstructor MapChunk;
    private final RefMethod methodInitLighting;
    private final RefConstructor classBlockPositionConstructor;
    private final RefConstructor classChunkSectionConstructor;
    private final RefMethod methodW;
    private final RefMethod methodAreNeighborsLoaded;
    private final RefField fieldSections;
    private final RefField fieldWorld;
    private final RefMethod methodGetBlocks;
    private final RefMethod methodSetType;
    private final RefMethod methodGetByCombinedId;
    private final Object air;
    private final RefMethod methodGetWorld;
    private final RefField tileEntityListTick;
    
    public BukkitQueue_1_9() throws NoSuchMethodException, RuntimeException {
        methodGetHandleChunk = classCraftChunk.getMethod("getHandle");
        methodInitLighting = classChunk.getMethod("initLighting");
        MapChunk = classMapChunk.getConstructor(classChunk.getRealClass(), boolean.class, int.class);
        classBlockPositionConstructor = classBlockPosition.getConstructor(int.class, int.class, int.class);
        methodW = classWorld.getMethod("w", classBlockPosition.getRealClass());
        fieldSections = classChunk.getField("sections");
        fieldWorld = classChunk.getField("world");
        methodGetByCombinedId = classBlock.getMethod("getByCombinedId", int.class);
        methodGetBlocks = classChunkSection.getMethod("getBlocks");
        methodSetType = classChunkSection.getMethod("setType", int.class, int.class, int.class, classIBlockData.getRealClass());
        methodAreNeighborsLoaded = classChunk.getMethod("areNeighborsLoaded", int.class);
        classChunkSectionConstructor = classChunkSection.getConstructor(int.class, boolean.class, char[].class);
        air = methodGetByCombinedId.call(0);
        this.tileEntityListTick = classWorld.getField("tileEntityListTick");
        this.methodGetWorld = classChunk.getMethod("getWorld");
    }
    
    @Override
    public Collection<FaweChunk<Chunk>> sendChunk(final Collection<FaweChunk<Chunk>> fcs) {
        for (FaweChunk<Chunk> fc : fcs) {
            Chunk chunk = fc.getChunk();
            ChunkLoc loc = fc.getChunkLoc();
            chunk.getWorld().refreshChunk(loc.x, loc.z);
        }
        return new ArrayList<>();
    }
    
    @Override
    public boolean fixLighting(final FaweChunk<?> pc, boolean fixAll) {
        try {
            BukkitChunk_1_9 bc = (BukkitChunk_1_9) pc;
            final Chunk chunk = bc.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load(false);
            }
            // Initialize lighting
            final Object c = methodGetHandleChunk.of(chunk).call();
            
            methodInitLighting.of(c).call();

            if ((bc.getTotalRelight() == 0 && !fixAll)) {
                return true;
            }

            final Object[] sections = (Object[]) fieldSections.of(c).get();
            final Object w = fieldWorld.of(c).get();

            final int X = chunk.getX() << 4;
            final int Z = chunk.getZ() << 4;
            
            RefExecutor relight = methodW.of(w);
            for (int j = 0; j < sections.length; j++) {
                final Object section = sections[j];
                if (section == null) {
                    continue;
                }
                if ((bc.getRelight(j) == 0 && !fixAll) || bc.getCount(j) == 0 || (bc.getCount(j) >= 4096 && bc.getAir(j) == 0)) {
                    continue;
                }
                final int[] array = bc.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int l = PseudoRandom.random.random(2);
                for (int k = 0; k < array.length; k++) {
                    final int i = array[k];
                    if (i < 16) {
                        continue;
                    }
                    final short id = (short) (i >> 4);
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
                            final int x = FaweCache.CACHE_X[j][k];
                            final int y = FaweCache.CACHE_Y[j][k];
                            final int z = FaweCache.CACHE_Z[j][k];
                            if (isSurrounded(bc.getIdArrays(), x, y, z)) {
                                continue;
                            }
                            final Object pos = classBlockPositionConstructor.create(X + x, y, Z + z);
                            relight.call(pos);
                    }
                }
            }
            return true;
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean isSurrounded(int[][] sections, int x, int y, int z) {
        return isSolid(getId(sections, x, y + 1, z))
        && isSolid(getId(sections, x + 1, y - 1, z))
        && isSolid(getId(sections, x - 1, y, z))
        && isSolid(getId(sections, x, y, z + 1))
        && isSolid(getId(sections, x, y, z - 1));
    }

    public boolean isSolid(int i) {
        if (i != 0) {
            Material material = Material.getMaterial(i);
            return material != null && Material.getMaterial(i).isOccluding();
        }
        return false;
    }

    public int getId(int[][] sections, int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            return 1;
        }
        if (y < 0 || y > 255) {
            return 1;
        }
        int i = FaweCache.CACHE_I[y][x][z];
        int[] section = sections[i];
        if (section == null) {
            return 0;
        }
        int j = FaweCache.CACHE_J[y][x][z];
        return section[j];
    }

    public Object getBlocks(final Object obj) {
        return methodGetBlocks.of(obj).call();
    }

    @Override
    public boolean setComponents(final FaweChunk<Chunk> pc) {
        final BukkitChunk_1_9 fs = (BukkitChunk_1_9) pc;
        Chunk chunk = pc.getChunk();
        final World world = chunk.getWorld();
        chunk.load(true);
        try {
            final boolean flag = world.getEnvironment() == Environment.NORMAL;

            // Sections
            final Method getHandele = chunk.getClass().getDeclaredMethod("getHandle");
            final Object c = getHandele.invoke(chunk);
            Object w = methodGetWorld.of(c).call();
            final Class<? extends Object> clazz = c.getClass();
            final Field sf = clazz.getDeclaredField("sections");
            sf.setAccessible(true);
            final Field tf = clazz.getDeclaredField("tileEntities");
            final Field ef = clazz.getDeclaredField("entitySlices");

            final Object[] sections = (Object[]) sf.get(c);
            final HashMap<?, ?> tiles = (HashMap<?, ?>) tf.get(c);
            final Collection<?>[] entities = (Collection<?>[]) ef.get(c);

            Method xm = null;
            Method ym = null;
            Method zm = null;

            // Trim tiles
            boolean removed = false;
            final Set<Entry<?, ?>> entryset = (Set<Entry<?, ?>>) (Set<?>) tiles.entrySet();
            final Iterator<Entry<?, ?>> iter = entryset.iterator();
            while (iter.hasNext()) {
                final Entry<?, ?> tile = iter.next();
                final Object pos = tile.getKey();
                if (xm == null) {
                    final Class<? extends Object> clazz2 = pos.getClass().getSuperclass();
                    xm = clazz2.getDeclaredMethod("getX");
                    ym = clazz2.getDeclaredMethod("getY");
                    zm = clazz2.getDeclaredMethod("getZ");
                }
                final int lx = (int) xm.invoke(pos) & 15;
                final int ly = (int) ym.invoke(pos);
                final int lz = (int) zm.invoke(pos) & 15;
                final int j = FaweCache.CACHE_I[ly][lx][lz];
                final int k = FaweCache.CACHE_J[ly][lx][lz];
                final int[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                if (array[k] != 0) {
                    removed = true;
                    iter.remove();
                }
            }
            if (removed) {
                ((Collection) this.tileEntityListTick.of(w).get()).clear();
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
                final int[] newArray = fs.getIdArray(j);
                if (newArray == null) {
                    continue;
                }
                Object section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    char[] array = new char[4096];
                    for (int i = 0; i < newArray.length; i++) {
                        int combined = newArray[i];
                        int id = combined & 4095;
                        int data = combined >> 12;
                        array[i] = (char) ((id << 4) + data);
                    }
                    section = sections[j] = newChunkSection(j << 4, flag, array);
                    continue;
                }
                final Object currentArray = getBlocks(section);
                RefExecutor setType = methodSetType.of(section);
                boolean fill = true;
                for (int k = 0; k < newArray.length; k++) {
                    final int n = newArray[k];
                    switch (n) {
                        case 0:
                            fill = false;
                            continue;
                        case -1: {
                            fill = false;
                            int x = FaweCache.CACHE_X[j][k];
                            int y = FaweCache.CACHE_Y[j][k];
                            int z = FaweCache.CACHE_Z[j][k];
                            setType.call(x, y & 15, z, air);
                            continue;
                        }
                        default: {
                            int x = FaweCache.CACHE_X[j][k];
                            int y = FaweCache.CACHE_Y[j][k];
                            int z = FaweCache.CACHE_Z[j][k];
                            int id = n;
                            Object iblock = methodGetByCombinedId.call(n);
                            setType.call(x, y & 15, z, iblock);
                            continue;
                        }
                    }
                }
                if (fill) {
                    fs.setCount(j, Short.MAX_VALUE);
                }
            }
            // Clear
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        int[][] biomes = fs.biomes;
        Biome[] values = Biome.values();
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
                    chunk.getBlock(x, 0, z).setBiome(values[biome]);
                }
            }
        }
        TaskManager.runTaskLater(new Runnable() {
            @Override
            public void run() {
                ChunkLoc loc = fs.getChunkLoc();
                world.refreshChunk(loc.x, loc.z);
            }
        }, 1);
        return true;
    }
    
    @Override
    public void clear() {
        super.clear();
        ArrayDeque<Chunk> toUnload = new ArrayDeque<>();
        final int distance = Bukkit.getViewDistance() + 2;
        HashMap<String, HashMap<IntegerPair, Integer>> players = new HashMap<>();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            // Clear history
            FawePlayer<Object> fp = FawePlayer.wrap(player);
            LocalSession s = fp.getSession();
            if (s != null) {
                s.clearHistory();
                s.setClipboard(null);
            }
            final Location loc = player.getLocation();
            final World worldObj = loc.getWorld();
            final String world = worldObj.getName();
            HashMap<IntegerPair, Integer> map = players.get(world);
            if (map == null) {
                map = new HashMap<>();
                players.put(world, map);
            }
            final IntegerPair origin = new IntegerPair(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            Integer val = map.get(origin);
            int check;
            if (val != null) {
                if (val == distance) {
                    continue;
                }
                check = distance - val;
            } else {
                check = distance;
                map.put(origin, distance);
            }
            for (int x = -distance; x <= distance; x++) {
                if ((x >= check) || (-x >= check)) {
                    continue;
                }
                for (int z = -distance; z <= distance; z++) {
                    if ((z >= check) || (-z >= check)) {
                        continue;
                    }
                    final int weight = distance - Math.max(Math.abs(x), Math.abs(z));
                    final IntegerPair chunk = new IntegerPair(x + origin.x, z + origin.z);
                    val = map.get(chunk);
                    if ((val == null) || (val < weight)) {
                        map.put(chunk, weight);
                    }
                }
            }
        }
        Fawe.get().getWorldEdit().clearSessions();
        for (final World world : Bukkit.getWorlds()) {
            final String name = world.getName();
            final HashMap<IntegerPair, Integer> map = players.get(name);
            if ((map == null) || (map.size() == 0)) {
                final boolean save = world.isAutoSave();
                world.setAutoSave(false);
                for (final Chunk chunk : world.getLoadedChunks()) {
                    unloadChunk(name, chunk);
                }
                world.setAutoSave(save);
                continue;
            }
            final Chunk[] chunks = world.getLoadedChunks();
            for (final Chunk chunk : chunks) {
                final int x = chunk.getX();
                final int z = chunk.getZ();
                if (!map.containsKey(new IntegerPair(x, z))) {
                    toUnload.add(chunk);
                } else if (chunk.getEntities().length > 4096) {
                    for (final Entity ent : chunk.getEntities()) {
                        ent.remove();
                    }
                }
            }
        }
        // GC again
        System.gc();
        System.gc();
        // If still critical memory
        int free = MemUtil.calculateMemory();
        if (free <= 1) {
            for (final Chunk chunk : toUnload) {
                unloadChunk(chunk.getWorld().getName(), chunk);
            }
        } else if (free == Integer.MAX_VALUE) {
            for (final Chunk chunk : toUnload) {
                chunk.unload(true, false);
            }
        } else {
            return;
        }
        toUnload = null;
        players = null;
        System.gc();
        System.gc();
        free = MemUtil.calculateMemory();
        if (free > 1) {
            return;
        }
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.size() > 0) {
            online.iterator().next().kickPlayer("java.lang.OutOfMemoryError");
        }
        online = null;
        System.gc();
        System.gc();
        free = MemUtil.calculateMemory();
        if ((free > 1) || (Bukkit.getOnlinePlayers().size() > 0)) {
            return;
        }
        for (final World world : Bukkit.getWorlds()) {
            final String name = world.getName();
            for (final Chunk chunk : world.getLoadedChunks()) {
                unloadChunk(name, chunk);
            }
        }
        System.gc();
        System.gc();
    }
    
    public Object newChunkSection(final int i, final boolean flag, final char[] ids) {
        return classChunkSectionConstructor.create(i, flag, ids);
    }
    
    @Override
    public FaweChunk<Chunk> getChunk(final ChunkLoc wrap) {
        return new BukkitChunk_1_9(wrap);
    }
    
    public boolean unloadChunk(final String world, final Chunk chunk) {
        final Object c = methodGetHandleChunk.of(chunk).call();
        mustSave.of(c).set(false);
        if (chunk.isLoaded()) {
            chunk.unload(false, false);
        }
        return true;
    }
    
    public ChunkGenerator setGenerator(final World world, final ChunkGenerator newGen) {
        try {
            final ChunkGenerator gen = world.getGenerator();
            final Class<? extends World> clazz = world.getClass();
            final Field generator = clazz.getDeclaredField("generator");
            generator.setAccessible(true);
            generator.set(world, newGen);
            
            final Field wf = clazz.getDeclaredField("world");
            wf.setAccessible(true);
            final Object w = wf.get(world);
            final Class<?> clazz2 = w.getClass().getSuperclass();
            final Field generator2 = clazz2.getDeclaredField("generator");
            generator2.set(w, newGen);
            
            return gen;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<BlockPopulator> setPopulator(final World world, final List<BlockPopulator> newPop) {
        try {
            final List<BlockPopulator> pop = world.getPopulators();
            final Field populators = world.getClass().getDeclaredField("populators");
            populators.setAccessible(true);
            populators.set(world, newPop);
            return pop;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void setEntitiesAndTiles(final Chunk chunk, final List<?>[] entities, final Map<?, ?> tiles) {
        try {
            final Class<? extends Chunk> clazz = chunk.getClass();
            final Method handle = clazz.getMethod("getHandle");
            final Object c = handle.invoke(chunk);
            final Class<? extends Object> clazz2 = c.getClass();
            
            if (tiles.size() > 0) {
                final Field tef = clazz2.getDeclaredField("tileEntities");
                final Map<?, ?> te = (Map<?, ?>) tef.get(c);
                final Method put = te.getClass().getMethod("putAll", Map.class);
                put.invoke(te, tiles);
            }
            
            final Field esf = clazz2.getDeclaredField("entitySlices");
            esf.setAccessible(true);
            esf.set(c, entities);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    
    public Object getProvider(final World world) {
        try {
            // Provider 1
            final Class<? extends World> clazz = world.getClass();
            final Field wf = clazz.getDeclaredField("world");
            wf.setAccessible(true);
            final Object w = wf.get(world);
            final Field provider = w.getClass().getSuperclass().getDeclaredField("chunkProvider");
            provider.setAccessible(true);
            // ChunkProviderServer
            final Class<? extends Object> clazz2 = w.getClass();
            final Field wpsf = clazz2.getDeclaredField("chunkProviderServer");
            // Store old provider server
            final Object worldProviderServer = wpsf.get(w);
            // Store the old provider
            final Field cp = worldProviderServer.getClass().getDeclaredField("chunkProvider");
            return cp.get(worldProviderServer);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public Object setProvider(final World world, Object newProvider) {
        try {
            // Provider 1
            final Class<? extends World> clazz = world.getClass();
            final Field wf = clazz.getDeclaredField("world");
            wf.setAccessible(true);
            final Object w = wf.get(world);
            // ChunkProviderServer
            final Class<? extends Object> clazz2 = w.getClass();
            final Field wpsf = clazz2.getDeclaredField("chunkProviderServer");
            // Store old provider server
            final Object worldProviderServer = wpsf.get(w);
            // Store the old provider
            final Field cp = worldProviderServer.getClass().getDeclaredField("chunkProvider");
            final Object oldProvider = cp.get(worldProviderServer);
            // Provider 2
            final Class<? extends Object> clazz3 = worldProviderServer.getClass();
            final Field provider2 = clazz3.getDeclaredField("chunkProvider");
            // If the provider needs to be calculated
            if (newProvider == null) {
                Method k;
                try {
                    k = clazz2.getDeclaredMethod("k");
                } catch (final Throwable e) {
                    try {
                        k = clazz2.getDeclaredMethod("j");
                    } catch (final Throwable e2) {
                        e2.printStackTrace();
                        return null;
                    }
                }
                k.setAccessible(true);
                final Object tempProviderServer = k.invoke(w);
                newProvider = cp.get(tempProviderServer);
                // Restore old provider
                wpsf.set(w, worldProviderServer);
            }
            // Set provider for provider server
            provider2.set(worldProviderServer, newProvider);
            // Return the previous provider
            return oldProvider;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

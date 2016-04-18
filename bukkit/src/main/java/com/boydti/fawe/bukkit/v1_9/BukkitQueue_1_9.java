package com.boydti.fawe.bukkit.v1_9;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.config.Settings;
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
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.LocalSession;
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
import java.util.concurrent.LinkedBlockingDeque;
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


import static com.boydti.fawe.util.ReflectionUtils.getRefClass;

public class BukkitQueue_1_9 extends BukkitQueue_0 {

    private final RefClass classMapChunk = getRefClass("{nms}.PacketPlayOutMapChunk");
    private final RefClass classChunk = getRefClass("{nms}.Chunk");
    private final RefClass classCraftChunk = getRefClass("{cb}.CraftChunk");
    private final RefClass classWorld = getRefClass("{nms}.World");
    private final RefField mustSave = this.classChunk.getField("mustSave");
    private final RefClass classBlockPosition = getRefClass("{nms}.BlockPosition");
    private final RefClass classChunkSection = getRefClass("{nms}.ChunkSection");
    private final RefClass classBlock = getRefClass("{nms}.Block");
    private final RefClass classIBlockData = getRefClass("{nms}.IBlockData");
    private final RefMethod methodGetHandleChunk;
    private final RefMethod methodInitLighting;
    private final RefConstructor classBlockPositionConstructor;
    private final RefConstructor classChunkSectionConstructor;
    private final RefMethod methodW;
    private final RefField fieldSections;
    private final RefField fieldWorld;
    private final RefMethod methodGetBlocks;
    private final RefMethod methodSetType;
    private final RefMethod methodGetType;
    private final RefMethod methodGetByCombinedId;
    private final RefMethod methodGetCombinedId;
    private final Object air;
    private final RefMethod methodGetWorld;
    private final RefField tileEntityListTick;

    public BukkitQueue_1_9(final String world) throws NoSuchMethodException, RuntimeException {
        super(world);
        this.methodGetHandleChunk = this.classCraftChunk.getMethod("getHandle");
        this.methodInitLighting = this.classChunk.getMethod("initLighting");
        this.classBlockPositionConstructor = this.classBlockPosition.getConstructor(int.class, int.class, int.class);
        this.methodW = this.classWorld.getMethod("w", this.classBlockPosition.getRealClass());
        this.fieldSections = this.classChunk.getField("sections");
        this.fieldWorld = this.classChunk.getField("world");
        this.methodGetByCombinedId = this.classBlock.getMethod("getByCombinedId", int.class);
        this.methodGetBlocks = this.classChunkSection.getMethod("getBlocks");
        this.methodSetType = this.classChunkSection.getMethod("setType", int.class, int.class, int.class, this.classIBlockData.getRealClass());
        this.methodGetType = this.classChunk.getMethod("a", int.class, int.class, int.class);
        this.classChunkSectionConstructor = this.classChunkSection.getConstructor(int.class, boolean.class, char[].class);
        this.air = this.methodGetByCombinedId.call(0);
        this.tileEntityListTick = this.classWorld.getField("tileEntityListTick");
        this.methodGetWorld = this.classChunk.getMethod("getWorld");
        this.methodGetCombinedId = classBlock.getMethod("getCombinedId", classIBlockData.getRealClass());
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                synchronized (loadQueue) {
                    if (loadQueue.size() > 0) {
                        Fawe.debug("Loading " + loadQueue.size() + " chunks.");
                    }
                    while (loadQueue.size() > 0) {
                        IntegerPair loc = loadQueue.poll();
                        if (bukkitWorld == null) {
                            bukkitWorld = Bukkit.getServer().getWorld(world);
                        }
                        if (!bukkitWorld.isChunkLoaded(loc.x, loc.z)) {
                            bukkitWorld.loadChunk(loc.x, loc.z);
                        }
                    }
                    loadQueue.notifyAll();
                }
            }
        }, 1);
    }

    private LinkedBlockingDeque<IntegerPair> loadQueue = new LinkedBlockingDeque<>();

    @Override
    public int getCombinedId4Data(int x, int y, int z) {
        if (y < 0 || y > 255) {
            return 0;
        }
        try {
            int cx = x >> 4;
            int cz = z >> 4;
            int cy = y >> 4;
            if (cx != lcx || cz != lcz) {
                if (bukkitWorld == null) {
                    bukkitWorld = Bukkit.getServer().getWorld(world);
                }
                lcx = cx;
                lcz = cz;
                if (!bukkitWorld.isChunkLoaded(cx, cz)) {
                    boolean sync = Thread.currentThread() == Fawe.get().getMainThread();
                    if (sync) {
                        bukkitWorld.loadChunk(cx, cz, true);
                    } else if (Settings.CHUNK_WAIT > 0) {
                        synchronized (loadQueue) {
                            loadQueue.add(new IntegerPair(cx, cz));
                            try {
                                loadQueue.wait(Settings.CHUNK_WAIT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (!bukkitWorld.isChunkLoaded(cx, cz)) {
                            return 0;
                        }
                    } else {
                        return 0;
                    }
                }
                lc = methodGetType.of(methodGetHandleChunk.of(bukkitWorld.getChunkAt(cx, cz)).call());
            }
            if (lc == null) {
                return 0;
            }
            int combined = (int) methodGetCombinedId.call(lc.call(x & 15, y, z & 15));
            return ((combined & 4095) << 4) + (combined >> 12);
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public Collection<FaweChunk<Chunk>> sendChunk(final Collection<FaweChunk<Chunk>> fcs) {
        for (final FaweChunk<Chunk> fc : fcs) {
            sendChunk(fc);
        }
        return new ArrayList<>();
    }

    public void sendChunk(FaweChunk<Chunk> fc) {
        fixLighting(fc, Settings.FIX_ALL_LIGHTING);
        final Chunk chunk = fc.getChunk();
        chunk.getWorld().refreshChunk(fc.getX(), fc.getZ());
    }

    @Override
    public boolean fixLighting(final FaweChunk<?> pc, final boolean fixAll) {
        try {
            final BukkitChunk_1_9 bc = (BukkitChunk_1_9) pc;
            final Chunk chunk = bc.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load(false);
            }
            // Initialize lighting
            final Object c = this.methodGetHandleChunk.of(chunk).call();

            this.methodInitLighting.of(c).call();

            if (((bc.getTotalRelight() == 0) && !fixAll)) {
                return true;
            }

            final Object[] sections = (Object[]) this.fieldSections.of(c).get();
            final Object w = this.fieldWorld.of(c).get();

            final int X = chunk.getX() << 4;
            final int Z = chunk.getZ() << 4;

            final RefExecutor relight = this.methodW.of(w);
            for (int j = 0; j < sections.length; j++) {
                final Object section = sections[j];
                if (section == null) {
                    continue;
                }
                if (((bc.getRelight(j) == 0) && !fixAll) || (bc.getCount(j) == 0) || ((bc.getCount(j) >= 4096) && (bc.getAir(j) == 0))) {
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
                    final short id = (short) (i & 0xFFF);
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
                            if (this.isSurrounded(bc.getIdArrays(), x, y, z)) {
                                continue;
                            }
                            final Object pos = this.classBlockPositionConstructor.create(X + x, y, Z + z);
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

    public boolean isSurrounded(final int[][] sections, final int x, final int y, final int z) {
        return this.isSolid(this.getId(sections, x, y + 1, z))
                && this.isSolid(this.getId(sections, x + 1, y - 1, z))
                && this.isSolid(this.getId(sections, x - 1, y, z))
                && this.isSolid(this.getId(sections, x, y, z + 1))
                && this.isSolid(this.getId(sections, x, y, z - 1));
    }

    public boolean isSolid(final int i) {
        if (i != 0) {
            final Material material = Material.getMaterial(i);
            return (material != null) && Material.getMaterial(i).isOccluding();
        }
        return false;
    }

    public int getId(final int[][] sections, final int x, final int y, final int z) {
        if ((x < 0) || (x > 15) || (z < 0) || (z > 15)) {
            return 1;
        }
        if ((y < 0) || (y > 255)) {
            return 1;
        }
        final int i = FaweCache.CACHE_I[y][x][z];
        final int[] section = sections[i];
        if (section == null) {
            return 0;
        }
        final int j = FaweCache.CACHE_J[y][x][z];
        return section[j];
    }

    public Object getBlocks(final Object obj) {
        return this.methodGetBlocks.of(obj).call();
    }

    private int lcx = Integer.MIN_VALUE;
    private int lcz = Integer.MIN_VALUE;
    private int lcy = Integer.MIN_VALUE;
    private RefExecutor lc;
    private World bukkitWorld;

    @Override
    public boolean setComponents(final FaweChunk<Chunk> pc) {
        final BukkitChunk_1_9 fs = (BukkitChunk_1_9) pc;
        final Chunk chunk = pc.getChunk();
        final World world = chunk.getWorld();
        chunk.load(true);
        try {
            final boolean flag = world.getEnvironment() == Environment.NORMAL;

            // Sections
            final Method getHandele = chunk.getClass().getDeclaredMethod("getHandle");
            final Object c = getHandele.invoke(chunk);
            final Object w = this.methodGetWorld.of(c).call();
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
                    final char[] array = new char[4096];
                    for (int i = 0; i < newArray.length; i++) {
                        final int combined = newArray[i];
                        final int id = combined & 4095;
                        final int data = combined >> 12;
                        array[i] = (char) ((id << 4) + data);
                    }
                    section = sections[j] = this.newChunkSection(j << 4, flag, array);
                    continue;
                }
                this.getBlocks(section);
                final RefExecutor setType = this.methodSetType.of(section);
                boolean fill = true;
                for (int k = 0; k < newArray.length; k++) {
                    final int n = newArray[k];
                    switch (n) {
                        case 0:
                            fill = false;
                            continue;
                        case -1: {
                            fill = false;
                            final int x = FaweCache.CACHE_X[j][k];
                            final int y = FaweCache.CACHE_Y[j][k];
                            final int z = FaweCache.CACHE_Z[j][k];
                            setType.call(x, y & 15, z, this.air);
                            continue;
                        }
                        default: {
                            final int x = FaweCache.CACHE_X[j][k];
                            final int y = FaweCache.CACHE_Y[j][k];
                            final int z = FaweCache.CACHE_Z[j][k];
                            final Object iblock = this.methodGetByCombinedId.call(n);
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
        final int[][] biomes = fs.biomes;
        final Biome[] values = Biome.values();
        if (biomes != null) {
            for (int x = 0; x < 16; x++) {
                final int[] array = biomes[x];
                if (array == null) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    final int biome = array[z];
                    if (biome == 0) {
                        continue;
                    }
                    chunk.getBlock(x, 0, z).setBiome(values[biome]);
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
     * This method is called when the server is < 1% available memory (i.e. likely to crash)<br>
     *  - You can disable this in the conifg<br>
     *  - Will try to free up some memory<br>
     *  - Clears the queue<br>
     *  - Clears worldedit history<br>
     *  - Clears entities<br>
     *  - Unloads chunks in vacant worlds<br>
     *  - Unloads non visible chunks<br>
     */
    @Override
    public void clear() {
        // Clear the queue
        super.clear();
        ArrayDeque<Chunk> toUnload = new ArrayDeque<>();
        final int distance = Bukkit.getViewDistance() + 2;
        HashMap<String, HashMap<IntegerPair, Integer>> players = new HashMap<>();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            // Clear history
            final FawePlayer<Object> fp = FawePlayer.wrap(player);
            final LocalSession s = fp.getSession();
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
                    this.unloadChunk(name, chunk);
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
                this.unloadChunk(chunk.getWorld().getName(), chunk);
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
    }

    public Object newChunkSection(final int i, final boolean flag, final char[] ids) {
        return this.classChunkSectionConstructor.create(i, flag, ids);
    }

    @Override
    public FaweChunk<Chunk> getChunk(int x, int z) {
        return new BukkitChunk_1_9(this, x, z);
    }

    public boolean unloadChunk(final String world, final Chunk chunk) {
        final Object c = this.methodGetHandleChunk.of(chunk).call();
        this.mustSave.of(c).set(false);
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

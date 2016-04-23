package com.boydti.fawe.bukkit.v1_8;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_All;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.ReflectionUtils.RefClass;
import com.boydti.fawe.util.ReflectionUtils.RefConstructor;
import com.boydti.fawe.util.ReflectionUtils.RefField;
import com.boydti.fawe.util.ReflectionUtils.RefMethod;
import com.boydti.fawe.util.ReflectionUtils.RefMethod.RefExecutor;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.lang.reflect.Field;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;


import static com.boydti.fawe.util.ReflectionUtils.getRefClass;

public class BukkitQueue_1_8 extends BukkitQueue_All {

    private final RefClass classEntityPlayer = getRefClass("{nms}.EntityPlayer");
    private final RefClass classMapChunk = getRefClass("{nms}.PacketPlayOutMapChunk");
    private final RefClass classPacket = getRefClass("{nms}.Packet");
    private final RefClass classConnection = getRefClass("{nms}.PlayerConnection");
    private final RefClass classChunk = getRefClass("{nms}.Chunk");
    private final RefClass classCraftPlayer = getRefClass("{cb}.entity.CraftPlayer");
    private final RefClass classCraftChunk = getRefClass("{cb}.CraftChunk");
    private final RefClass classWorld = getRefClass("{nms}.World");
    private final RefField mustSave = this.classChunk.getField("mustSave");
    private final RefClass classBlockPosition = getRefClass("{nms}.BlockPosition");
    private final RefClass classChunkSection = getRefClass("{nms}.ChunkSection");

    private RefMethod methodRecalcBlockCounts;
    private RefMethod methodGetHandlePlayer;
    private RefMethod methodGetHandleChunk;
    private RefConstructor MapChunk;
    private RefField connection;
    private RefMethod send;
    private RefMethod methodInitLighting;
    private RefConstructor classBlockPositionConstructor;
    private RefConstructor classChunkSectionConstructor;
    private RefMethod methodX;
    private RefMethod methodAreNeighborsLoaded;
    private RefField fieldSections;
    private RefField fieldWorld;
    private RefMethod methodGetIdArray;
    private RefMethod methodGetWorld;
    private RefField tileEntityListTick;

    public BukkitQueue_1_8(final String world) {
        super(world);
        try {
            this.methodGetHandlePlayer = this.classCraftPlayer.getMethod("getHandle");
            this.methodGetHandleChunk = this.classCraftChunk.getMethod("getHandle");
            this.methodInitLighting = this.classChunk.getMethod("initLighting");
            this.MapChunk = this.classMapChunk.getConstructor(this.classChunk.getRealClass(), boolean.class, int.class);
            this.connection = this.classEntityPlayer.getField("playerConnection");
            this.send = this.classConnection.getMethod("sendPacket", this.classPacket.getRealClass());
            this.classBlockPositionConstructor = this.classBlockPosition.getConstructor(int.class, int.class, int.class);
            this.methodX = this.classWorld.getMethod("x", this.classBlockPosition.getRealClass());
            this.fieldSections = this.classChunk.getField("sections");
            this.fieldWorld = this.classChunk.getField("world");
            this.methodGetIdArray = this.classChunkSection.getMethod("getIdArray");
            this.methodAreNeighborsLoaded = this.classChunk.getMethod("areNeighborsLoaded", int.class);
            this.classChunkSectionConstructor = this.classChunkSection.getConstructor(int.class, boolean.class, char[].class);
            this.tileEntityListTick = this.classWorld.getField("tileEntityList");
            this.methodGetWorld = this.classChunk.getMethod("getWorld");
            this.methodRecalcBlockCounts = this.classChunkSection.getMethod("recalcBlockCounts");
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public Object getCachedChunk(int cx, int cz) {
        return methodGetHandleChunk.of(bukkitWorld.getChunkAt(cx, cz)).call();
    }

    public Object getCachedSection(Object chunk, int cy) {
        Object storage = ((Object[]) fieldSections.of(chunk).get())[cy];
        if (storage == null) {
            return null;
        }
        return getIdArray(storage);
    }

    public int getCombinedId4Data(Object section, int x, int y, int z) {
        char[] ls = (char[]) section;
        return ls != null ? ls[FaweCache.CACHE_J[y][x & 15][z & 15]] : 0;
    }

    @Override
    public Collection<FaweChunk<Chunk>> sendChunk(final Collection<FaweChunk<Chunk>> fcs) {
        for (final FaweChunk<Chunk> fc : fcs) {
            sendChunk(fc);
        }
        return new ArrayList<>();
    }

    public void sendChunk(final FaweChunk<Chunk> fc) {
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                final boolean result = fixLighting(fc, Settings.FIX_ALL_LIGHTING) || !Settings.ASYNC_LIGHTING;
                TaskManager.IMP.task(new Runnable() {
                    @Override
                    public void run() {
                        if (!result) {
                            fixLighting(fc, Settings.FIX_ALL_LIGHTING);
                        }
                        Chunk chunk = fc.getChunk();
                        World world = chunk.getWorld();
                        final int view = Bukkit.getServer().getViewDistance();
                        int cx = chunk.getX();
                        int cz = chunk.getZ();
                        for (final Player player : Bukkit.getOnlinePlayers()) {
                            if (!player.getWorld().equals(world)) {
                                continue;
                            }
                            final Location loc = player.getLocation();
                            final int px = loc.getBlockX() >> 4;
                            final int pz = loc.getBlockZ() >> 4;
                            if ((Math.abs(cx - px) > view) || (Math.abs(cz - pz) > view)) {
                                continue;
                            }
                            final Object entity = methodGetHandlePlayer.of(player).call();
                            final RefExecutor con = send.of(connection.of(entity).get());
                            final Object c = methodGetHandleChunk.of(fc.getChunk()).call();
                            Object packet = MapChunk.create(c, false, 65535);
                            con.call(packet);
                        }
                    }
                }, false);
            }
        }, Settings.ASYNC_LIGHTING);
    }

    @Override
    public boolean fixLighting(final FaweChunk<?> fc, final boolean fixAll) {
        try {
            final BukkitChunk_1_8 bc = (BukkitChunk_1_8) fc;
            final Chunk chunk = bc.getChunk();
            if (!chunk.isLoaded()) {
                if (Fawe.get().getMainThread() != Thread.currentThread()) {
                    return false;
                }
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

            final RefExecutor relight = this.methodX.of(w);
            for (int j = 0; j < sections.length; j++) {
                final Object section = sections[j];
                if (section == null) {
                    continue;
                }
                if (((bc.getRelight(j) == 0) && !fixAll) || (bc.getCount(j) == 0) || ((bc.getCount(j) >= 4096) && (bc.getAir(j) == 0))) {
                    continue;
                }
                final char[] array = this.getIdArray(section);
                if (array == null) {
                    continue;
                }
                int l = FaweCache.RANDOM.random(2);
                for (int k = 0; k < array.length; k++) {
                    final int i = array[k];
                    if (i < 16) {
                        continue;
                    }
                    final short id = FaweCache.CACHE_ID[i];
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
                            if (this.isSurrounded(sections, x, y, z)) {
                                continue;
                            }
                            final Object pos = this.classBlockPositionConstructor.create(X + x, y, Z + z);
                            relight.call(pos);
                    }
                }
            }
            return true;
        } catch (final Throwable e) {
            if (Thread.currentThread() == Fawe.get().getMainThread()) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isSurrounded(final Object[] sections, final int x, final int y, final int z) {
        return this.isSolid(this.getId(sections, x, y + 1, z))
        && this.isSolid(this.getId(sections, x + 1, y - 1, z))
        && this.isSolid(this.getId(sections, x - 1, y, z))
        && this.isSolid(this.getId(sections, x, y, z + 1))
        && this.isSolid(this.getId(sections, x, y, z - 1));
    }

    public boolean isSolid(final int i) {
        if (i == 0) {
            return false;
        }
        return Material.getMaterial(i).isOccluding();
    }

    public int getId(final Object[] sections, final int x, final int y, final int z) {
        if ((x < 0) || (x > 15) || (z < 0) || (z > 15)) {
            return 1;
        }
        if ((y < 0) || (y > 255)) {
            return 1;
        }
        final int i = FaweCache.CACHE_I[y][x][z];
        final Object section = sections[i];
        if (section == null) {
            return 0;
        }
        final char[] array = this.getIdArray(section);
        final int j = FaweCache.CACHE_J[y][x][z];
        return array[j] >> 4;
    }

    @Override
    public boolean setComponents(final FaweChunk<Chunk> fc) {
        try {
            final BukkitChunk_1_8 fs = ((BukkitChunk_1_8) fc);
            final Chunk chunk = fs.getChunk();
            final World world = chunk.getWorld();

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
                final char[] array = fs.getIdArray(j);
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
                final char[] newArray = fs.getIdArray(j);
                if (newArray == null) {
                    continue;
                }
                Object section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    section = sections[j] = this.newChunkSection(j << 4, flag, newArray);
                    continue;
                }
                final char[] currentArray = this.getIdArray(section);
                boolean fill = true;
                for (int k = 0; k < newArray.length; k++) {
                    final char n = newArray[k];
                    if (n == 0) {
                        fill = false;
                        continue;
                    }
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
                methodRecalcBlockCounts.of(section).call();
            }

            // Biomes
            final int[][] biomes = fs.getBiomeArray();
            if (biomes != null) {
                final LocalWorld lw = BukkitUtil.getLocalWorld(world);
                final int X = fs.getX() << 4;
                final int Z = fs.getZ() << 4;
                final BaseBiome bb = new BaseBiome(0);
                int last = 0;
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
                        if (last != biome) {
                            last = biome;
                            bb.setId(biome);
                        }
                        lw.setBiome(new Vector2D(X + x, Z + z), bb);
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
        } catch (final Exception e) {
            if (Thread.currentThread() == Fawe.get().getMainThread()) {
                e.printStackTrace();
            }
        }
        return false;
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

    public char[] getIdArray(final Object obj) {
        return (char[]) this.methodGetIdArray.of(obj).call();
    }

    @Override
    public FaweChunk<Chunk> getChunk(int x, int z) {
        return new BukkitChunk_1_8(this, x, z);
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

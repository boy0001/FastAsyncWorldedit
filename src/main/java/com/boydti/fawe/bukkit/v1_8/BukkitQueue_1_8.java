package com.boydti.fawe.bukkit.v1_8;

import static com.boydti.fawe.util.ReflectionUtils.getRefClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.ChunkLoc;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.ReflectionUtils.RefClass;
import com.boydti.fawe.util.ReflectionUtils.RefConstructor;
import com.boydti.fawe.util.ReflectionUtils.RefField;
import com.boydti.fawe.util.ReflectionUtils.RefMethod;
import com.boydti.fawe.util.ReflectionUtils.RefMethod.RefExecutor;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class BukkitQueue_1_8 extends BukkitQueue_0 {
    
    private final RefClass classEntityPlayer = getRefClass("{nms}.EntityPlayer");
    private final RefClass classMapChunk = getRefClass("{nms}.PacketPlayOutMapChunk");
    private final RefClass classPacket = getRefClass("{nms}.Packet");
    private final RefClass classConnection = getRefClass("{nms}.PlayerConnection");
    private final RefClass classChunk = getRefClass("{nms}.Chunk");
    private final RefClass classCraftPlayer = getRefClass("{cb}.entity.CraftPlayer");
    private final RefClass classCraftChunk = getRefClass("{cb}.CraftChunk");
    private final RefClass classWorld = getRefClass("{nms}.World");
    private final RefClass classCraftWorld = getRefClass("{cb}.CraftWorld");
    private final RefField mustSave = classChunk.getField("mustSave");
    private final RefClass classBlockPosition = getRefClass("{nms}.BlockPosition");
    private final RefClass classChunkSection = getRefClass("{nms}.ChunkSection");
    
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
    private RefMethod methodD;
    private RefField fieldSections;
    private RefField fieldWorld;
    private RefMethod methodGetIdArray;
    
    private final HashMap<String, FaweGenerator_1_8> worldMap = new HashMap<>();
    
    public BukkitQueue_1_8() {
        try {
            methodGetHandlePlayer = classCraftPlayer.getMethod("getHandle");
            methodGetHandleChunk = classCraftChunk.getMethod("getHandle");
            methodInitLighting = classChunk.getMethod("initLighting");
            MapChunk = classMapChunk.getConstructor(classChunk.getRealClass(), boolean.class, int.class);
            connection = classEntityPlayer.getField("playerConnection");
            send = classConnection.getMethod("sendPacket", classPacket.getRealClass());
            classBlockPositionConstructor = classBlockPosition.getConstructor(int.class, int.class, int.class);
            methodX = classWorld.getMethod("x", classBlockPosition.getRealClass());
            methodD = classChunk.getMethod("d", int.class, int.class, int.class);
            fieldSections = classChunk.getField("sections");
            fieldWorld = classChunk.getField("world");
            methodGetIdArray = classChunkSection.getMethod("getIdArray");
            methodAreNeighborsLoaded = classChunk.getMethod("areNeighborsLoaded", int.class);
            classChunkSectionConstructor = classChunkSection.getConstructor(int.class, boolean.class, char[].class);
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    
    public FaweGenerator_1_8 getFaweGenerator(final World world) {
        final ChunkGenerator gen = world.getGenerator();
        if ((gen != null) && (gen instanceof FaweGenerator_1_8)) {
            return (FaweGenerator_1_8) gen;
        }
        FaweGenerator_1_8 faweGen = worldMap.get(world.getName());
        if (faweGen != null) {
            return faweGen;
        }
        faweGen = new FaweGenerator_1_8(this, world);
        worldMap.put(world.getName(), faweGen);
        return faweGen;
    }
    
    @Override
    public Collection<FaweChunk<Chunk>> sendChunk(final Collection<FaweChunk<Chunk>> fcs) {
        final HashMap<FaweChunk<Chunk>, Object> packets = new HashMap<>();
        final HashMap<String, ArrayList<FaweChunk<Chunk>>> map = new HashMap<>();

        for (final FaweChunk<Chunk> fc : fcs) {
            String world = fc.getChunkLoc().world;
            ArrayList<FaweChunk<Chunk>> list = map.get(world);
            if (list == null) {
                list = new ArrayList<>();
                map.put(world, list);
            }
            list.add(fc);
        }
        final int view = Bukkit.getServer().getViewDistance();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final String world = player.getWorld().getName();
            final ArrayList<FaweChunk<Chunk>> list = map.get(world);
            if (list == null) {
                continue;
            }
            final Location loc = player.getLocation();
            final int cx = loc.getBlockX() >> 4;
            final int cz = loc.getBlockZ() >> 4;
            final Object entity = methodGetHandlePlayer.of(player).call();
            
            for (final FaweChunk<Chunk> fc : list) {
                final int dx = Math.abs(cx - fc.getChunkLoc().x);
                final int dz = Math.abs(cz - fc.getChunkLoc().z);
                if ((dx > view) || (dz > view)) {
                    continue;
                }
                RefExecutor con = send.of(connection.of(entity).get());
                Object packet = packets.get(fc);
                if (packet == null) {
                    final Object c = methodGetHandleChunk.of(fc.getChunk()).call();
                    packet = MapChunk.create(c, true, 65535);
                    packets.put(fc, packet);
                    con.call(packet);
                } else {
                    con.call(packet);
                }
            }
        }
        final HashSet<FaweChunk<Chunk>> chunks = new HashSet<FaweChunk<Chunk>>();
        for (FaweChunk<Chunk> fc : fcs) {
            Chunk chunk = fc.getChunk();
            chunk.unload(true, false);
            chunk.load();
            ChunkLoc loc = fc.getChunkLoc();
            chunk.getWorld().refreshChunk(loc.x, loc.z);
            if (!fixLighting(fc, Settings.FIX_ALL_LIGHTING)) {
                chunks.add(fc);
            }
        }
        return chunks;
    }
    
    @Override
    public boolean fixLighting(final FaweChunk<?> fc, boolean fixAll) {
        try {
            BukkitChunk_1_8 bc = (BukkitChunk_1_8) fc;
            final Chunk chunk = bc.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load(false);
            }
            
            // Initialize lighting
            final Object c = methodGetHandleChunk.of(chunk).call();
            
            if (!(boolean) methodAreNeighborsLoaded.of(c).call(1)) {
                return false;
            }
            
            methodInitLighting.of(c).call();

            if ((bc.getTotalRelight() == 0 && !fixAll)) {
                return true;
            }

            final Object[] sections = (Object[]) fieldSections.of(c).get();
            final Object w = fieldWorld.of(c).get();
            
            final int X = chunk.getX() << 4;
            final int Z = chunk.getZ() << 4;
            
            RefExecutor relight = methodX.of(w);
            for (int j = 0; j < sections.length; j++) {
                final Object section = sections[j];
                if (section == null) {
                    continue;
                }
                if ((bc.getRelight(j) == 0 && !fixAll) || bc.getCount(j) == 0 || (bc.getCount(j) >= 4096 && bc.getAir(j) == 0)) {
                    continue;
                }
                final char[] array = getIdArray(section);
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
                            if (isSurrounded(sections, x, y, z)) {
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
    
    public boolean isSurrounded(Object[] sections, int x, int y, int z) {
        return isSolid(getId(sections, x, y + 1, z))
        && isSolid(getId(sections, x + 1, y - 1, z))
        && isSolid(getId(sections, x - 1, y, z))
        && isSolid(getId(sections, x, y, z + 1))
        && isSolid(getId(sections, x, y, z - 1));
    }
    
    public boolean isSolid(int i) {
        if (i == 0) {
            return false;
        }
        return Material.getMaterial(i).isOccluding();
    }

    public int getId(Object[] sections, int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            return 1;
        }
        if (y < 0 || y > 255) {
            return 1;
        }
        int i = FaweCache.CACHE_I[y][x][z];
        Object section = sections[i];
        if (section == null) {
            return 0;
        }
        char[] array = getIdArray(section);
        int j = FaweCache.CACHE_J[y][x][z];
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
            
            // Trim tiles
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
                    iter.remove();
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
                final char[] newArray = fs.getIdArray(j);
                if (newArray == null) {
                    continue;
                }
                Object section = sections[j];
                if ((section == null) || (fs.getCount(j) >= 4096)) {
                    section = sections[j] = newChunkSection(j << 4, flag, newArray);
                    continue;
                }
                final char[] currentArray = getIdArray(section);
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
            }
            
            // Biomes
            int[][] biomes = fs.getBiomeArray();
            if (biomes != null) {
                LocalWorld lw = BukkitUtil.getLocalWorld(world);
                int X = fs.getChunkLoc().x << 4;
                int Z = fs.getChunkLoc().z << 4;
                BaseBiome bb = new BaseBiome(0);
                int last = 0;
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
                        if (last != biome) {
                            last = biome;
                            bb.setId(biome);
                        }
                        lw.setBiome(new Vector2D(X + x, Z + z), bb);
                    }
                }
            }
            
            // Clear
            fs.clear();
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
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
    
    public char[] getIdArray(final Object obj) {
        return (char[]) methodGetIdArray.of(obj).call();
    }
    
    @Override
    public FaweChunk<Chunk> getChunk(final ChunkLoc wrap) {
        return new BukkitChunk_1_8(wrap);
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

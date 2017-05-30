package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.BukkitPlayer;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldInitEvent;

public abstract class BukkitQueue_0<CHUNK, CHUNKSECTIONS, SECTION> extends NMSMappedFaweQueue<World, CHUNK, CHUNKSECTIONS, SECTION> implements Listener {

    public static BukkitImplAdapter adapter;
    public static Method methodToNative;
    public static Method methodFromNative;
    private static boolean setupAdapter = false;

    public BukkitQueue_0(final com.sk89q.worldedit.world.World world) {
        super(world);
        setupAdapter(null);
        if (!registered) {
            registered = true;
            Bukkit.getServer().getPluginManager().registerEvents(this, ((FaweBukkit) Fawe.imp()).getPlugin());
        }
    }

    public BukkitQueue_0(String world) {
        super(world);
        setupAdapter(null);
        if (!registered) {
            registered = true;
            Bukkit.getServer().getPluginManager().registerEvents(this, ((FaweBukkit) Fawe.imp()).getPlugin());
        }
    }

    @Override
    public File getSaveFolder() {
        return new File(Bukkit.getWorldContainer(), getWorldName() + File.separator + "region");
    }

    @Override
    public void setFullbright(CHUNKSECTIONS sections) {}

    @Override
    public void relight(int x, int y, int z) {}

    @Override
    public void relightBlock(int x, int y, int z) {}

    @Override
    public void relightSky(int x, int y, int z) {}

    @Override
    public boolean removeLighting(CHUNKSECTIONS sections, RelightMode mode, boolean hasSky) {
        return false;
    }

    public static void checkVersion(String supported) {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        if (!version.contains(supported)) {
            throw new IllegalStateException("Unsupported version: " + version + " (supports: " + supported + ")");
        }
    }

    private static boolean registered = false;
    private static boolean disableChunkLoad = false;

    @EventHandler
    public static void onWorldLoad(WorldInitEvent event) {
        if (disableChunkLoad) {
            World world = event.getWorld();
            world.setKeepSpawnInMemory(false);
        }
    }

    public static ConcurrentHashMap<Long, Long> keepLoaded = new ConcurrentHashMap<>(8, 0.9f, 1);

    @EventHandler
    public static void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        long pair = MathMan.pairInt(chunk.getX(), chunk.getZ());
        Long lastLoad = keepLoaded.get(pair);
        if (lastLoad != null) {
            if (System.currentTimeMillis() - lastLoad < 10000) {
                event.setCancelled(true);
            } else {
                keepLoaded.remove(pair);
            }
        }
    }

    @Override
    public boolean queueChunkLoad(int cx, int cz) {
        if (super.queueChunkLoad(cx, cz)) {
            keepLoaded.put(MathMan.pairInt(cx, cz), System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public World createWorld(final WorldCreator creator) {
        World world = TaskManager.IMP.sync(new RunnableVal<World>() {
            @Override
            public void run(World value) {
                disableChunkLoad = true;
                this.value = creator.createWorld();
                disableChunkLoad = false;
            }
        });
        return world;
    }

    public static void setupAdapter(BukkitImplAdapter adapter) {
        try {
            if (setupAdapter == (setupAdapter = true)) {
                return;
            }
            WorldEditPlugin instance = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
            Field fieldAdapter = WorldEditPlugin.class.getDeclaredField("bukkitAdapter");
            fieldAdapter.setAccessible(true);
            if ((BukkitQueue_0.adapter = adapter) != null) {
                fieldAdapter.set(instance, adapter);
            } else {
                BukkitQueue_0.adapter = adapter = (BukkitImplAdapter) fieldAdapter.get(instance);
            }
            for (Method method : adapter.getClass().getDeclaredMethods()) {
                switch (method.getName()) {
                    case "toNative":
                        methodToNative = method;
                        methodToNative.setAccessible(true);
                        break;
                    case "fromNative":
                        methodFromNative = method;
                        methodFromNative.setAccessible(true);
                        break;
                }
            }
        } catch (Throwable e) {
            Fawe.debug("====== NO NATIVE WORLDEDIT ADAPTER ======");
            e.printStackTrace();
            Fawe.debug("Try updating WorldEdit: ");
            Fawe.debug(" - http://builds.enginehub.org/job/worldedit?branch=master");
            Fawe.debug("See also: http://wiki.sk89q.com/wiki/WorldEdit/Bukkit_adapters");
            Fawe.debug("=========================================");
        }
    }

    @Override
    public World getImpWorld() {
        return getWorldName() != null ? Bukkit.getWorld(getWorldName()) : null;
    }

    @Override
    public void sendChunk(int x, int z, int bitMask) {}

    @Override
    public void refreshChunk(FaweChunk fs) {}

    @Override
    public boolean regenerateChunk(World world, int x, int z, BaseBiome biome, Long seed) {
        return world.regenerateChunk(x, z);
    }


    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, CHUNKSECTIONS sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception {
        return fs;
    }

    @Override
    public boolean hasSky() {
        World world = getWorld();
        return world == null || world.getEnvironment() == World.Environment.NORMAL;
    }

    private volatile boolean timingsEnabled;
    private static boolean alertTimingsChange = true;
    private static Field fieldTimingsEnabled;
    private static Field fieldAsyncCatcherEnabled;
    private static Method methodCheck;
    static {
        try {
            fieldAsyncCatcherEnabled = Class.forName("org.spigotmc.AsyncCatcher").getField("enabled");
            fieldAsyncCatcherEnabled.setAccessible(true);
        } catch (Throwable ignore) {}
        try {
            fieldTimingsEnabled = Class.forName("co.aikar.timings.Timings").getDeclaredField("timingsEnabled");
            fieldTimingsEnabled.setAccessible(true);
            methodCheck = Class.forName("co.aikar.timings.TimingsManager").getDeclaredMethod("recheckEnabled");
            methodCheck.setAccessible(true);
        } catch (Throwable ignore){}
    }

    @Override
    public void startSet(boolean parallel) {
        ChunkListener.physicsFreeze = true;
        if (parallel) {
            try {
                if (fieldAsyncCatcherEnabled != null) {
                    fieldAsyncCatcherEnabled.set(null, false);
                }
                if (fieldTimingsEnabled != null) {
                    timingsEnabled = (boolean) fieldTimingsEnabled.get(null);
                    if (timingsEnabled) {
                        if (alertTimingsChange) {
                            alertTimingsChange = false;
                            Fawe.debug("Having `parallel-threads` > 1 interferes with the timings.");
                        }
                        fieldTimingsEnabled.set(null, false);
                        methodCheck.invoke(null);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendBlockUpdate(final FaweChunk chunk, FawePlayer... players) {
        if (players.length == 0) {
            return;
        }
        int cx = chunk.getX();
        int cz = chunk.getZ();
        int view = Bukkit.getServer().getViewDistance();
        boolean sendAny = false;
        boolean[] send = new boolean[players.length];
        for (int i = 0; i < players.length; i++) {
            FawePlayer player = players[i];
            Player bp = ((BukkitPlayer) player).parent;
            Location loc = bp.getLocation();
            if (Math.abs((loc.getBlockX() >> 4) - cx) <= view && Math.abs((loc.getBlockZ() >> 4) - cz) <= view) {
                sendAny = true;
                send[i] = true;
            }
        }
        if (!sendAny) {
            return;
        }
        final World world = getWorld();
        final int bx = cx << 4;
        final int bz = cz << 4;
        chunk.forEachQueuedBlock(new FaweChunkVisitor() {
            @Override
            public void run(int localX, int y, int localZ, int combined) {
                Location loc = new Location(world, bx + localX, y, bz + localZ);
                for (int i = 0; i < players.length; i++) {
                    if (send[i]) {
                        ((BukkitPlayer) players[i]).parent.sendBlockChange(loc, FaweCache.getId(combined), (byte) FaweCache.getData(combined));
                    }
                }
            }
        });
    }

    @Override
    public void endSet(boolean parallel) {
        ChunkListener.physicsFreeze = false;
        if (parallel) {
            try {
                if (fieldAsyncCatcherEnabled != null) {
                    fieldAsyncCatcherEnabled.set(null, true);
                }
                if (fieldTimingsEnabled != null && timingsEnabled) {
                    fieldTimingsEnabled.set(null, true);
                    methodCheck.invoke(null);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}

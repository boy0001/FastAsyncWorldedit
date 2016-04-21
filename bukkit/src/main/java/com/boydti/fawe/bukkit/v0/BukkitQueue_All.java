package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v1_8.BukkitChunk_1_8;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingDeque;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.spigotmc.AsyncCatcher;

public class BukkitQueue_All extends BukkitQueue_0 {
    public final LinkedBlockingDeque<IntegerPair> loadQueue = new LinkedBlockingDeque<>();

    public BukkitQueue_All(final String world) {
        super(world);
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                synchronized (loadQueue) {
                    while (loadQueue.size() > 0) {
                        IntegerPair loc = loadQueue.poll();
                        if (bukkitWorld == null) {
                            bukkitWorld = Bukkit.getServer().getWorld(world);
                        }
                        if (!bukkitWorld.isChunkLoaded(loc.x, loc.z)) {
                            bukkitWorld.loadChunk(loc.x, loc.z, true);
                        }
                    }
                    loadQueue.notifyAll();
                }
            }
        }, 1);
        if (getClass() == BukkitQueue_All.class) {
            TaskManager.IMP.task(new Runnable() {
                @Override
                public void run() {
                    Bukkit.getPluginManager().registerEvents(BukkitQueue_All.this, (Plugin) Fawe.imp());
                }
            });
        }
    }


    private boolean physicsFreeze = false;

    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
        }
    }

    @Override
    public Collection<FaweChunk<Chunk>> sendChunk(Collection<FaweChunk<Chunk>> fcs) {
        return new ArrayList<>();
    }

    @Override
    public boolean setComponents(FaweChunk<Chunk> fc) {
        try {
            startSet();
            final BukkitChunk_1_8 fs = ((BukkitChunk_1_8) fc);
            final Chunk chunk = fs.getChunk();
            final World world = chunk.getWorld();
            char[][] sections = fs.getIdArrays();
            boolean done = false;
            boolean more = false;
            // Efficiently merge sections
            for (int j = 0; j < sections.length; j++) {
                final int jf = j;
                int changes = fs.getCount(j);
                int lighting = fs.getRelight(j);
                if (changes == 0) {
                    continue;
                }
                final char[] newArray = sections[j];
                if (newArray == null) {
                    continue;
                }
                if (done) {
                    more = true;
                    break;
                }
                done = true;
                sections[j] = null;
                ArrayList<Thread> threads = new ArrayList<Thread>();
                for (int k = 0; k < 16; k++) {
                    final int l = k << 8;
                    final int y = FaweCache.CACHE_Y[j][l];
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int m = l; m < l + 256; m++) {
                                int combined = newArray[m];
                                switch (combined) {
                                    case 0:
                                        continue;
                                    case 1:
                                        int x = FaweCache.CACHE_X[jf][m];
                                        int z = FaweCache.CACHE_Z[jf][m];
                                        chunk.getBlock(x, y, z).setTypeId(0, false);
                                        continue;
                                    default:
                                        x = FaweCache.CACHE_X[jf][m];
                                        z = FaweCache.CACHE_Z[jf][m];
                                        int id = combined >> 4;
                                        int data = combined & 0xF;
                                        Block block = chunk.getBlock(x, y, z);
                                        if (data == 0) {
                                            block.setTypeId(id, false);
                                        } else {
                                            block.setTypeIdAndData(id, (byte) data, false);
                                        }
                                        continue;
                                }
                            }
                        }
                    });
                    threads.add(thread);
                    thread.start();
                }
                for (Thread  thread : threads) {
                    thread.join();
                }
            }
            if (more) {
                fc.addToQueue();
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
            endSet();
            return true;
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        endSet();
        return false;
    }

    public void startSet() {
        physicsFreeze = true;
        try {
            // Need to temporarily disable the async catcher since it can't discern safe/unsafe async calls
            // The main thread will be locked until it is enabled again (if anything fails it will be enabled again)
            AsyncCatcher.enabled = false;
        } catch (Throwable ignore) {}
    }

    public void endSet() {
        physicsFreeze = false;
        try {
            AsyncCatcher.enabled = true;
        } catch (Throwable ignore) {}
    }

    @Override
    public FaweChunk<Chunk> getChunk(int x, int z) {
        return new BukkitChunk_1_8(this, x, z);
    }

    @Override
    public boolean fixLighting(FaweChunk<?> fc, boolean fixAll) {
        return true;
    }

    public void loadChunk(IntegerPair chunk) {
        loadQueue.add(chunk);
    }

    public int lastChunkX = Integer.MIN_VALUE;
    public int lastChunkZ = Integer.MIN_VALUE;
    public int lastChunkY = Integer.MIN_VALUE;

    private Object lastChunk;
    private Object lastSection;

    public Object getCachedChunk(int cx, int cz) {
        return bukkitWorld.getChunkAt(cx, cz);
    }

    public Object getCachedSection(Object chunk, int cy) {
        return lastChunk;
    }

    public int getCombinedId4Data(Object section, int x, int y, int z) {
        Block block = ((Chunk) lastChunk).getBlock(x & 15, y, z & 15);
        int combined = block.getTypeId() << 4;
        if (FaweCache.hasData(combined)) {
            combined += block.getData();
        }
        return combined;
    }

    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        if (y < 0 || y > 255) {
            return 0;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastChunkX || cz != lastChunkZ) {
            if (bukkitWorld == null) {
                bukkitWorld = Bukkit.getServer().getWorld(world);
            }
            lastChunkX = cx;
            lastChunkZ = cz;
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
                        throw new FaweException.FaweChunkLoadException();
                    }
                } else {
                    return 0;
                }
            }
            lastChunk = getCachedChunk(cx, cz);
            lastSection = getCachedSection(lastChunk, cy);
        } else if (cy != lastChunkY) {
            if (lastChunk == null) {
                return 0;
            }
            lastSection = getCachedSection(lastChunk, cy);
        }

        if (lastChunk == null) {
            return 0;
        }
        return getCombinedId4Data(lastSection, x, y, z);
    }
}

package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.FaweChunk;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

public abstract class BukkitQueue_0<CHUNK, CHUNKSECTIONS, SECTION> extends NMSMappedFaweQueue<World, CHUNK, CHUNKSECTIONS, SECTION> {

    public BukkitQueue_0(final String world) {
        super(world);
    }

    @Override
    public World getWorld(String world) {
        return Bukkit.getWorld(world);
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public void refreshChunk(World world, CHUNK chunk) {
        return;
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z) {
        return world.regenerateChunk(x, z);
    }

    @Override
    public boolean fixLighting(FaweChunk fc, boolean fixAll) {
        // Not implemented
        return true;
    }

    @Override
    public boolean loadChunk(World impWorld, int x, int z, boolean generate) {
        return impWorld.loadChunk(x, z, generate);
    }

    private volatile boolean timingsEnabled;

    @Override
    public void startSet(boolean parallel) {
        ChunkListener.physicsFreeze = true;
        if (parallel) {
            try {
                Field fieldEnabled = Class.forName("co.aikar.timings.Timings").getDeclaredField("timingsEnabled");
                fieldEnabled.setAccessible(true);
                timingsEnabled = (boolean) fieldEnabled.get(null);
                if (timingsEnabled) {
                    fieldEnabled.set(null, false);
                    Method methodCheck = Class.forName("co.aikar.timings.TimingsManager").getDeclaredMethod("recheckEnabled");
                    methodCheck.setAccessible(true);
                    methodCheck.invoke(null);
                }
            } catch (Throwable ignore) {}
            try { Class.forName("org.spigotmc.AsyncCatcher").getField("enabled").set(null, false); } catch (Throwable ignore) {}
        }
    }

    @Override
    public void endSet(boolean parallel) {
        ChunkListener.physicsFreeze = false;
        if (parallel) {
            try {Field fieldEnabled = Class.forName("co.aikar.timings.Timings").getDeclaredField("timingsEnabled");fieldEnabled.setAccessible(true);fieldEnabled.set(null, timingsEnabled);
            } catch (Throwable ignore) {}
            try { Class.forName("org.spigotmc.AsyncCatcher").getField("enabled").set(null, true); } catch (Throwable ignore) {}
        }
    }

    @Override
    public boolean setComponents(FaweChunk fc) {
        try {
            final CharFaweChunk<Chunk> fs = ((CharFaweChunk<Chunk>) fc);
            final Chunk chunk = fs.getChunk();
            chunk.load(true);
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
            return true;
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public FaweChunk getChunk(int x, int z) {
        return new CharFaweChunk<Chunk>(this, x, z) {
            @Override
            public Chunk getNewChunk() {
                return BukkitQueue_0.this.getWorld().getChunkAt(getX(), getZ());
            }
        };
    }
}

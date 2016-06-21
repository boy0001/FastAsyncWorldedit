package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BukkitChunk_All extends CharFaweChunk<Chunk> {

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public BukkitChunk_All(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public Chunk getNewChunk() {
        return Bukkit.getWorld(getParent().getWorldName()).getChunkAt(getX(), getZ());
    }

    private int layer = -1;
    private int index;
    private boolean place = true;

    /**
     *
     * @return
     */
    public void execute(long start) {
        int recommended = 25 + BukkitQueue_All.ALLOCATE;
        boolean more = true;
        FaweQueue parent = getParent();
        final Chunk chunk = getChunk();
        chunk.load(true);
        final World world = chunk.getWorld();
        char[][] sections = getCombinedIdArrays();
        if (layer == -1) {
            // Biomes
            if (layer == 0) {
                final int[][] biomes = getBiomeArray();
                if (biomes != null) {
                    final LocalWorld lw = BukkitUtil.getLocalWorld(world);
                    final int X = getX() << 4;
                    final int Z = getZ() << 4;
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
            }
        } else if (index != 0) {
            if (place) {
                layer--;
            } else {
                layer++;
            }
        }
        mainloop:
        do {
            if (place) {
                if (++layer >= sections.length) {
                    place = false;
                    layer = sections.length - 1;
                }
            } else if (--layer < 0) {
                more = false;
                break;
            }
            try {
                // Efficiently merge sections
                int changes = getCount(layer);
                int lighting = getRelight(layer);
                if (changes == 0) {
                    continue;
                }
                final char[] newArray = sections[layer];
                if (newArray == null) {
                    continue;
                }
                boolean checkTime = !((getAir(layer) == 4096 || (getCount(layer) == 4096 && getAir(layer) == 0) || (getCount(layer) == getAir(layer))) && getRelight(layer) == 0);
                if (!checkTime || Settings.PARALLEL_THREADS > 1) {
                    ArrayList<Thread> threads = new ArrayList<Thread>();
                    for (int k = 0; k < 16; k++) {
                        final int l = k << 8;
                        final int y = FaweCache.CACHE_Y[layer][l];
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (int m = l; m < l + 256; m++) {
                                    char combined = newArray[m];
                                    switch (combined) {
                                        case 0:
                                            continue;
                                        case 1:
                                            if (!place) {
                                                int x = FaweCache.CACHE_X[layer][m];
                                                int z = FaweCache.CACHE_Z[layer][m];
                                                chunk.getBlock(x, y, z).setTypeId(0, false);
                                            }
                                            continue;
                                        default:
                                            if (place) {
                                                int x = FaweCache.CACHE_X[layer][m];
                                                int z = FaweCache.CACHE_Z[layer][m];
                                                int id = combined >> 4;
                                                int data = combined & 0xF;
                                                Block block = chunk.getBlock(x, y, z);
                                                if (data == 0) {
                                                    block.setTypeId(id, false);
                                                } else {
                                                    block.setTypeIdAndData(id, (byte) data, false);
                                                }
                                            }
                                            continue;
                                    }
                                }
                            }
                        });
                        threads.add(thread);
                        thread.start();
                    }
                    for (Thread thread : threads) {
                        thread.join();
                    }
                } else {
                    for (;index < 4096; index++) {
                        int j = place ? index : 4095 - index;
                        char combined = newArray[j];
                        switch (combined) {
                            case 0:
                                break;
                            case 1:
                                if (!place) {
                                    int x = FaweCache.CACHE_X[layer][j];
                                    int z = FaweCache.CACHE_Z[layer][j];
                                    int y = FaweCache.CACHE_Y[layer][j];
                                    chunk.getBlock(x, y, z).setTypeId(0, false);
                                }
                                break;
                            default:
                                if (place) {
                                    int id = combined >> 4;
                                    int data = combined & 0xF;
                                    int x = FaweCache.CACHE_X[layer][j];
                                    int z = FaweCache.CACHE_Z[layer][j];
                                    int y = FaweCache.CACHE_Y[layer][j];
                                    Block block = chunk.getBlock(x, y, z);
                                    if (data == 0) {
                                        block.setTypeId(id, false);
                                    } else {
                                        block.setTypeIdAndData(id, (byte) data, false);
                                    }
                                }
                                break;
                        }
                        if (checkTime && System.currentTimeMillis() - start > recommended) {
                            index++;
                            break mainloop;
                        }
                    }
                    index = 0;
                }
            } catch (final Throwable e) {
                MainUtil.handleError(e);
            }
        } while (System.currentTimeMillis() - start < recommended);
        if (more || place) {
            this.addToQueue();
        }
    }
}

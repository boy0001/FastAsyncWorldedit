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
        BukkitQueue_All parent = (BukkitQueue_All) getParent();
        final Chunk chunk = getChunk();
        Object[] disableResult = parent.disableLighting(chunk);
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
                final byte[] cacheX = FaweCache.CACHE_X[layer];
                final short[] cacheY = FaweCache.CACHE_Y[layer];
                final byte[] cacheZ = FaweCache.CACHE_Z[layer];
                boolean checkTime = !((getAir(layer) == 4096 || (getCount(layer) == 4096 && getAir(layer) == 0) || (getCount(layer) == getAir(layer))) && getRelight(layer) == 0);
                if (!checkTime) {
                    ArrayList<Thread> threads = new ArrayList<Thread>();
                    for (int k = 0; k < 16; k++) {
                        final int l = k << 8;
                        final int y = cacheY[l];
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
                                                int x = cacheX[m];
                                                int z = cacheZ[m];
                                                setBlock(chunk.getBlock(x, y, z), 0, (byte) 0);
                                            }
                                            continue;
                                        default:
                                            if (place) {
                                                int x = cacheX[m];
                                                int z = cacheZ[m];
                                                int id = combined >> 4;
                                                Block block = chunk.getBlock(x, y, z);
                                                setBlock(block, id, (byte) (combined & 0xF));
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
                                continue;
                            case 1:
                                if (!place) {
                                    int x = cacheX[j];
                                    int z = cacheZ[j];
                                    int y = cacheY[j];
                                    setBlock(chunk.getBlock(x, y, z), 0, (byte) 0);
                                }
                                break;
                            default:
                                int id = combined >> 4;
                                boolean light = FaweCache.hasLight(id);
                                if (light) {
                                    if (place) {
                                        continue;
                                    }
                                } else if (!place) {
                                    continue;
                                }
                                if (light != place) {
                                    light = light && Settings.LIGHTING.MODE != 0;
                                    if (light) {
                                        parent.enableLighting(disableResult);
                                    }
                                    int data = combined & 0xF;
                                    int x = cacheX[j];
                                    int z = cacheZ[j];
                                    int y = cacheY[j];
                                    Block block = chunk.getBlock(x, y, z);
                                    setBlock(block, id, (byte) data);
                                    if (light) {
                                        parent.disableLighting(disableResult);
                                    }
                                } else {
                                    continue;
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
        parent.resetLighting(disableResult);
    }

    public void setBlock(Block block, int id, byte data) {
        block.setTypeIdAndData(id, data, false);
    }
}

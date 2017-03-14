package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BukkitChunk_All extends CharFaweChunk<Chunk, BukkitQueue_All> {

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

    public BukkitChunk_All(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z, ids, count, air, heightMap);
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        BukkitChunk_All copy;
        if (shallow) {
            copy = new BukkitChunk_All(getParent(), getX(), getZ(), ids, count, air, heightMap);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new BukkitChunk_All(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
            copy.biomes = biomes;
            copy.chunk = chunk;
            copy.biomes = biomes.clone();
            copy.chunk = chunk;
        }
        return copy;
    }

    @Override
    public Chunk getNewChunk() {
        return Bukkit.getWorld(getParent().getWorldName()).getChunkAt(getX(), getZ());
    }

    private int layer = -1;
    private int index;
    private boolean place = true;

    @Override
    public void start() {
        getChunk().load(true);
    }

    /**
     *
     * @return
     */
    @Override
    public FaweChunk call() {
        long start = System.currentTimeMillis();
        int recommended = 25 + BukkitQueue_All.ALLOCATE;
        boolean more = true;
        final BukkitQueue_All parent = (BukkitQueue_All) getParent();
        final Chunk chunk = getChunk();
        Object[] disableResult = parent.disableLighting(chunk);
        final World world = chunk.getWorld();
        char[][] sections = getCombinedIdArrays();
        final int bx = getX() << 4;
        final int bz = getZ() << 4;
        if (layer == -1) {
            // Biomes
            if (layer == 0) {
                final byte[] biomes = getBiomeArray();
                if (biomes != null) {
                    final LocalWorld lw = BukkitUtil.getLocalWorld(world);
                    int index = 0;
                    for (int z = 0; z < 16; z++) {
                        int zz = bx + z;
                        for (int x = 0; x < 16; x++) {
                            int xx = bz + x;
                            lw.setBiome(MutableBlockVector2D.get(xx, zz), FaweCache.getBiome(biomes[index++] & 0xFF));
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
                boolean checkTime = !((getAir(layer) == 4096 || (getCount(layer) == 4096 && getAir(layer) == 0) || (getCount(layer) == getAir(layer))));
                if (!checkTime) {
                    final ArrayList<Thread> threads = new ArrayList<Thread>();
                    for (int k = 0; k < 16; k++) {
                        final int l = k << 8;
                        final int y = cacheY[l];
                        final Thread thread = new Thread(new Runnable() {
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
                                                if (FaweCache.hasNBT(id) && parent.adapter != null) {
                                                    CompoundTag nbt = getTile(x, y, z);
                                                    if (nbt != null) {
                                                        parent.adapter.setBlock(new Location(world, bx + x, y, bz + z), new BaseBlock(id, combined & 0xF, nbt), false);
                                                        continue;
                                                    }
                                                }
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
                                    light = light && Settings.IMP.LIGHTING.MODE != 0;
                                    if (light) {
                                        parent.enableLighting(disableResult);
                                    }
                                    int data = combined & 0xF;
                                    int x = cacheX[j];
                                    int z = cacheZ[j];
                                    int y = cacheY[j];
                                    if (FaweCache.hasNBT(id) && parent.adapter != null) {
                                        CompoundTag tile = getTile(x, y, z);
                                        if (tile != null) {
                                            parent.adapter.setBlock(new Location(world, bx + x, y, bz + z), new BaseBlock(id, combined & 0xF, tile), false);
                                            break;
                                        }
                                    }
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
        return this;
    }

    public void setBlock(Block block, int id, byte data) {
        block.setTypeIdAndData(id, data, false);
    }
}

package com.boydti.fawe.nukkit.optimization.queue;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.nukkit.core.NBTConverter;
import com.boydti.fawe.nukkit.core.NukkitUtil;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;

public class NukkitChunk extends CharFaweChunk<BaseFullChunk> {


    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public NukkitChunk(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public BaseFullChunk getNewChunk() {
        return ((NukkitQueue) getParent()).getWorld().getChunk(getX(), getZ());
    }

    private int layer = -1;
    private int index;
    private boolean place = true;

    public void execute(long start) {
        int recommended = 25 + NukkitQueue.ALLOCATE;
        boolean more = true;
        NukkitQueue parent = (NukkitQueue) getParent();
        Level world = ((NukkitQueue) getParent()).getWorld();
        world.clearCache(true);
        final BaseFullChunk chunk = (world.getChunk(getX(), getZ(), true));
        char[][] sections = getCombinedIdArrays();
        if (layer == -1) {
            // Biomes
            if (layer == 0) {
                final int[][] biomes = getBiomeArray();
                if (biomes != null) {
                    final LocalWorld lw = NukkitUtil.getLocalWorld(world);
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
                                                chunk.setBlockId(x, y, z, 0);
                                            }
                                            continue;
                                        default:
                                            if (place) {
                                                int x = cacheX[m];
                                                int z = cacheZ[m];
                                                int id = combined >> 4;
                                                chunk.setBlockId(x, y, z, id);
                                                chunk.setBlockData(x, y, z, (combined & 0xF));
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
                                    chunk.setBlockId(x, y, z, 0);
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
                                    int data = combined & 0xF;
                                    int x = cacheX[j];
                                    int z = cacheZ[j];
                                    int y = cacheY[j];
                                    chunk.setBlockId(x, y, z, id);
                                    chunk.setBlockData(x, y, z, data);
                                    if (FaweCache.hasNBT(id)) {
                                        CompoundTag tile = getTile(x, y, z);
                                        if (tile != null) {
                                            cn.nukkit.nbt.tag.CompoundTag tag = (cn.nukkit.nbt.tag.CompoundTag) NBTConverter.toNative(tile);
                                            chunk.addBlockEntity(new BlockEntity(chunk, tag) {
                                                @Override
                                                public boolean isBlockEntityValid() {
                                                    return getBlock().getId() == id;
                                                }
                                            });
                                            break;
                                        }
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
    }
}
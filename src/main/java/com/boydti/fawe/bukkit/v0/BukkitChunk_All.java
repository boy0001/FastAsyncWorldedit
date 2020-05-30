package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;

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
        } else {
            copy = new BukkitChunk_All(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
            copy.biomes = biomes != null ? biomes.clone() : null;
        }
        copy.chunk = chunk;
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

    private static boolean canTick(int id) {
        switch (id) {
            case BlockID.VINE:
            case BlockID.FIRE:
            case BlockID.ICE:
            case BlockID.PACKED_ICE:
            case BlockID.FROSTED_ICE:
            case BlockID.LEAVES:
            case BlockID.LEAVES2:
            case BlockID.SOIL:
            case BlockID.CACTUS:
            case BlockID.REED:
            case BlockID.CHORUS_FLOWER:
            case BlockID.CHORUS_PLANT:
            case BlockID.GRASS:
            case BlockID.MYCELIUM:
            case BlockID.SAPLING:
            case BlockID.WATER:
            case BlockID.STATIONARY_WATER:
            case BlockID.LAVA:
            case BlockID.STATIONARY_LAVA:
            case BlockID.GLOWING_REDSTONE_ORE:
            case BlockID.REDSTONE_ORE:
            case BlockID.PORTAL:
            case BlockID.END_PORTAL:
            case BlockID.REDSTONE_BLOCK:
            case BlockID.REDSTONE_LAMP_OFF:
            case BlockID.REDSTONE_LAMP_ON:
            case BlockID.REDSTONE_REPEATER_OFF:
            case BlockID.REDSTONE_REPEATER_ON:
            case BlockID.COMMAND_BLOCK:
            case BlockID.CHAIN_COMMAND_BLOCK:
            case BlockID.REPEATING_COMMAND_BLOCK:
            case BlockID.REDSTONE_TORCH_OFF:
            case BlockID.REDSTONE_TORCH_ON:
            case BlockID.REDSTONE_WIRE:
            case BlockID.CROPS:
            case BlockID.MELON_STEM:
            case BlockID.PUMPKIN_STEM:
            case BlockID.POTATOES:
            case BlockID.CARROTS:
            case BlockID.COCOA_PLANT:
            case BlockID.BEETROOTS:
            case BlockID.NETHER_WART:
            case BlockID.NETHER_WART_BLOCK:
            case BlockID.BROWN_MUSHROOM:
            case BlockID.RED_MUSHROOM:
                return true;
            default: return false;
        }
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
            BukkitImplAdapter adapter = BukkitQueue_0.getAdapter();
            if (adapter != null)
            {
                // Run change task
                RunnableVal2<FaweChunk, FaweChunk> task = parent.getChangeTask();
                BukkitChunk_All_ReadonlySnapshot previous;
                if (task != null){
                    ChunkSnapshot snapshot = parent.ensureChunkLoaded(getX(), getZ());
                    previous = new BukkitChunk_All_ReadonlySnapshot(parent, snapshot, biomes != null);
                    for (BlockState tile : chunk.getTileEntities()) {
                        int x = tile.getX();
                        int y = tile.getY();
                        int z = tile.getZ();
                        if (getBlockCombinedId(x & 15, y, z & 15) != 0) {
                            CompoundTag nbt = adapter.getBlock(new Location(world, x, y, z)).getNbtData();
                            if (nbt != null) {
                                previous.setTile(x & 15, y, z & 15, nbt);
                            }
                        }
                    }
                } else {
                    previous = null;
                }
                // Set entities
                if (adapter != null) {
                    Set<CompoundTag> entitiesToSpawn = this.getEntities();
                    if (!entitiesToSpawn.isEmpty()) {
                        for (CompoundTag tag : entitiesToSpawn) {
                            String id = tag.getString("Id");
                            ListTag posTag = tag.getListTag("Pos");
                            ListTag rotTag = tag.getListTag("Rotation");
                            if (id == null || posTag == null || rotTag == null) {
                                Fawe.debug("Unknown entity tag: " + tag);
                                continue;
                            }
                            double x = posTag.getDouble(0);
                            double y = posTag.getDouble(1);
                            double z = posTag.getDouble(2);
                            float yaw = rotTag.getFloat(0);
                            float pitch = rotTag.getFloat(1);
                            Location loc = new Location(world, x, y, z, yaw, pitch);
                            Entity created = adapter.createEntity(loc, new BaseEntity(id, tag));
                            if (previous != null) {
                                UUID uuid = created.getUniqueId();
                                Map<String, Tag> map = ReflectionUtils.getMap(tag.getValue());
                                map.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                                map.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                            }
                        }
                    }
                    HashSet<UUID> entsToRemove = this.getEntityRemoves();
                    if (!entsToRemove.isEmpty()) {
                        for (Entity entity : chunk.getEntities()) {
                            if (entsToRemove.contains(entity.getUniqueId())) {
                                entity.remove();
                            }
                        }
                    }
                }
                if (previous != null) {
                    task.run(previous, this);
                }
            }

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
                                Location mutableLoc = null;
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
                                                if (FaweCache.hasNBT(id) && parent.getAdapter() != null) {
                                                    CompoundTag nbt = getTile(x, y, z);
                                                    if (nbt != null) {
                                                        if (mutableLoc == null) mutableLoc = new Location(world, 0, 0, 0);
                                                        mutableLoc.setX(bx + x);
                                                        mutableLoc.setY(y);
                                                        mutableLoc.setZ(bz + z);
                                                        synchronized (BukkitChunk_All.this) {
                                                            parent.getAdapter().setBlock(mutableLoc, new BaseBlock(id, combined & 0xF, nbt), false);
                                                        }
                                                        continue;
                                                    }
                                                }
                                                Block block = chunk.getBlock(x, y, z);
                                                byte data = (byte) (combined & 0xF);
                                                if (canTick(id)) {
                                                    synchronized (BukkitChunk_All.this) {
                                                        setBlock(block, id, data);
                                                    }
                                                } else {
                                                    setBlock(block, id, data);
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
                                    light = light && getParent().getSettings().LIGHTING.MODE != 0;
                                    if (light) {
                                        parent.enableLighting(disableResult);
                                    }
                                    int data = combined & 0xF;
                                    int x = cacheX[j];
                                    int z = cacheZ[j];
                                    int y = cacheY[j];
                                    if (FaweCache.hasNBT(id) && parent.getAdapter() != null) {
                                        CompoundTag tile = getTile(x, y, z);
                                        if (tile != null) {
                                            parent.getAdapter().setBlock(new Location(world, bx + x, y, bz + z), new BaseBlock(id, combined & 0xF, tile), false);
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

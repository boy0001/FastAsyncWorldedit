package com.boydti.fawe.nukkit.optimization.queue;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.nbt.tag.Tag;
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
import java.io.IOException;
import java.util.Map;

public class NukkitChunk extends CharFaweChunk<BaseFullChunk, NukkitQueue> {


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

    public NukkitChunk(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z, ids, count, air, heightMap);
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        NukkitChunk copy;
        if (shallow) {
            copy = new NukkitChunk(getParent(), getX(), getZ(), ids, count, air, heightMap);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new NukkitChunk(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
            copy.biomes = biomes;
            copy.chunk = chunk;
            copy.biomes = biomes.clone();
            copy.chunk = chunk;
        }
        return copy;
    }

    @Override
    public BaseFullChunk getNewChunk() {
        return ((NukkitQueue) getParent()).getWorld().getChunk(getX(), getZ(), true);
    }

    private int layer = -1;
    private int index;
    private boolean place = true;

    @Override
    public void start() {
        try {
            getChunk().load(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public NukkitChunk call() {
        // Set heightmap
        NukkitQueue parent = (NukkitQueue) getParent();
        Level world = ((NukkitQueue) getParent()).getWorld();
        world.clearCache(true);
        final BaseFullChunk chunk = getChunk();
        getParent().setHeightMap(this, heightMap);
        char[][] sections = getCombinedIdArrays();
        final int[][] biomes = getBiomeArray();
        final int X = getX() << 4;
        final int Z = getZ() << 4;
        if (biomes != null) {
            final LocalWorld lw = NukkitUtil.getLocalWorld(world);
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
        for (int layer = 0; layer < sections.length; layer++) {
            char[] ids = sections[layer];
            if (ids == null) {
                continue;
            }
            int by = layer << 4;
            int index = 0;
            for (int y = by; y < by + 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++,index++) {
                        char combined = ids[index];
                        switch (combined) {
                            case 0:
                                continue;
                            case 1:
                                chunk.setBlockId(x, y, z, 0);
                                continue;
                            default:
                                int id = FaweCache.getId(combined);
                                int data = FaweCache.getData(combined);
                                chunk.setBlockId(x, y, z, id);
                                chunk.setBlockData(x, y, z, data);
                                if (FaweCache.hasNBT(id)) {
                                    CompoundTag tile = getTile(x, y, z);
                                    if (tile != null) {
                                        cn.nukkit.nbt.tag.CompoundTag tag = (cn.nukkit.nbt.tag.CompoundTag) NBTConverter.toNative(tile);
                                        String tileId = tag.getString("id");
                                        Map<String, Tag> map = NBTConverter.getMap(tag);
                                        map.put("x", new IntTag("x", X + x));
                                        map.put("y", new IntTag("y", y));
                                        map.put("z", new IntTag("z", Z + z));
                                        BlockEntity ent = BlockEntity.createBlockEntity(tileId, chunk, tag);
                                        if (ent != null) {
                                            chunk.addBlockEntity(ent);
                                        }
                                        continue;
                                    }
                                }
                        }
                    }
                }
            }
        }
        return this;
    }
}
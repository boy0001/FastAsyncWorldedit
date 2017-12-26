package com.boydti.fawe.object.queue;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BaseBiome;


import static com.google.common.base.Preconditions.checkNotNull;

public class FaweQueueDelegateExtent extends DelegateFaweQueue {
    private final FaweQueue parentQueue;
    private final Extent parentExtent;
    private final MutableBlockVector2D mutable2d = new MutableBlockVector2D();

    public FaweQueueDelegateExtent(FaweQueue parentQueue, Extent parentExtent) {
        super(parentQueue);
        checkNotNull(parentExtent);
        checkNotNull(parentQueue);
        this.parentQueue = parentQueue;
        this.parentExtent = parentExtent;
    }

    @Override
    public boolean setBlock(int x, int y, int z, int id, int data) {
        try {
            return setBlock(x, y, z, FaweCache.getBlock(id, data));
        } catch (WorldEditException e) {
            return false;
        }
    }

    @Override
    public boolean setBlock(int x, int y, int z, int id, int data, CompoundTag nbt) {
        BaseBlock block;
        if (nbt != null) {
            block = new BaseBlock(id, data, nbt);
        } else {
            block = FaweCache.getBlock(id, data);
        }
        try {
            return setBlock(x, y, z, block);
        } catch (WorldEditException e) {
            return false;
        }
    }

    @Override
    public int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getCombinedId4Data(x, y, z);
    }

    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getLazyBlock(x, y, z).getCombined();
    }

    @Override
    public CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getLazyBlock(x, y, z).getNbtData();
    }

    @Override
    public int getBiomeId(int x, int z) throws FaweException.FaweChunkLoadException {
        return parentExtent.getBiome(mutable2d.setComponents(x, z)).getId();
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return parentExtent.setBiome(position, biome);
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return parentExtent.getBlock(position);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return parentExtent.getBiome(position);
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block) throws WorldEditException {
        return parentExtent.setBlock(position, block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        return parentExtent.setBlock(x, y, z, block);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return parentExtent.getLazyBlock(position);
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        return parentExtent.getLazyBlock(x, y, z);
    }


}

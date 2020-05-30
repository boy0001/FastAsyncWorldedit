package com.sk89q.worldedit.extent.inventory;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Applies a {@link BlockBag} to operations.
 */
public class BlockBagExtent extends AbstractDelegateExtent {

    private final boolean mine;
    private int[] missingBlocks = new int[4096];
    private BlockBag blockBag;

    /**
     * Create a new instance.
     *
     * @param extent   the extent
     * @param blockBag the block bag
     */
    public BlockBagExtent(Extent extent, @Nonnull BlockBag blockBag) {
        this(extent, blockBag, false);
    }

    public BlockBagExtent(Extent extent, @Nonnull BlockBag blockBag, boolean mine) {
        super(extent);
        checkNotNull(blockBag);
        this.blockBag = blockBag;
        this.mine = mine;
    }

    /**
     * Get the block bag.
     *
     * @return a block bag, which may be null if none is used
     */
    public
    @Nullable
    BlockBag getBlockBag() {
        return blockBag;
    }

    /**
     * Set the block bag.
     *
     * @param blockBag a block bag, which may be null if none is used
     */
    public void setBlockBag(@Nullable BlockBag blockBag) {
        this.blockBag = blockBag;
    }

    /**
     * Gets the list of missing blocks and clears the list for the next
     * operation.
     *
     * @return a map of missing blocks
     */
    public Map<Integer, Integer> popMissing() {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < missingBlocks.length; i++) {
            int count = missingBlocks[i];
            if (count > 0) {
                map.put(i, count);
            }
        }
        Arrays.fill(missingBlocks, 0);
        return map;
    }

    @Override
    public boolean setBlock(Vector pos, BaseBlock block) throws WorldEditException {
        return setBlock(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        CompoundTag nbt = block.getNbtData();
        int combinedTo = block.getCombined();
        if (combinedTo != 0) {
            try {
                blockBag.fetchPlacedBlock(FaweCache.getId(combinedTo), FaweCache.getData(combinedTo));
            } catch (UnplaceableBlockException e) {
                throw new FaweException.FaweBlockBagException();
            } catch (BlockBagException e) {
                missingBlocks[combinedTo]++;
                throw new FaweException.FaweBlockBagException();
            }
        }
        if (mine) {
            BaseBlock lazyBlock = getExtent().getLazyBlock(x, y, z);
            int combinedFrom = lazyBlock.getCombined();
            if (combinedFrom != 0) {
                try {
                    blockBag.storeDroppedBlock(FaweCache.getId(combinedFrom), FaweCache.getData(combinedFrom));
                } catch (BlockBagException ignored) {
                }
            }
        }
        return getExtent().setBlock(x, y, z, block);
    }

    public static Class<?> inject() {
        return BlockBagExtent.class;
    }
}
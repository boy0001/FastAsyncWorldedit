package com.sk89q.worldedit.extent.inventory;

import com.boydti.fawe.FaweCache;
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
     * @param extent the extent
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
    public @Nullable BlockBag getBlockBag() {
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
    public boolean setBlock(Vector position, BaseBlock block) throws WorldEditException {
        CompoundTag nbt = block.getNbtData();
        final int type = block.getType();
        if (type != 0) {
            try {
                blockBag.fetchPlacedBlock(block.getId(), block.getData());
                if (nbt != null) {
                    // Remove items (to avoid duplication)
                    if (nbt.containsKey("items")) {
                        block.setNbtData(null);
                    }
                }
            } catch (UnplaceableBlockException e) {
                return false;
            } catch (BlockBagException e) {
                missingBlocks[type]++;
                return false;
            }
        }
        if (mine) {
            BaseBlock lazyBlock = getExtent().getLazyBlock(position);
            int existing = lazyBlock.getType();
            if (existing != 0) {
                try {
                    blockBag.storeDroppedBlock(existing, lazyBlock.getData());
                } catch (BlockBagException ignored) {}
            }
        }
        return super.setBlock(position, block);
    }

    public static Class<?> inject() {
        return BlockBagExtent.class;
    }
}
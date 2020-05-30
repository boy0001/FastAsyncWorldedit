package com.sk89q.worldedit.function.mask;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mask that checks whether blocks at the given positions are matched by
 * a block in a list.
 * <p>
 * <p>This mask checks for both an exact block ID and data value match, as well
 * for a block with the same ID but a data value of -1.</p>
 */
public class BlockMask extends AbstractExtentMask {

    public final boolean[] blocks = new boolean[Character.MAX_VALUE + 1];
    public final boolean[] blockIds = new boolean[4096];
    public Collection<BaseBlock> computedLegacyList;

    /**
     * Create a new block mask.
     *
     * @param extent the extent
     * @param blocks a list of blocks to match
     */
    public BlockMask(Extent extent, Collection<BaseBlock> blocks) {
        super(extent);
        checkNotNull(blocks);
        add(blocks);
    }

    /**
     * Create a new block mask.
     *
     * @param extent the extent
     * @param block  an array of blocks to match
     */
    public BlockMask(Extent extent, BaseBlock... block) {
        this(extent, Arrays.asList(checkNotNull(block)));
    }

    /**
     * Add the given blocks to the list of criteria.
     *
     * @param blocks a list of blocks
     */
    public void add(Collection<BaseBlock> blocks) {
        checkNotNull(blocks);
        for (BaseBlock block : blocks) {
            add(block);
        }
    }

    /**
     * Add the given blocks to the list of criteria.
     *
     * @param blocks an array of blocks
     */
    public void add(BaseBlock... blocks) {
        for (BaseBlock block : blocks) {
            add(block);
        }
    }

    public void add(BaseBlock block) {
        blockIds[block.getId()] = true;
        if (block.getData() == -1) {
            for (int data = 0; data < 16; data++) {
                blocks[FaweCache.getCombined(block.getId(), data)] = true;
            }
        } else {
            blocks[FaweCache.getCombined(block)] = true;
        }
        computedLegacyList = null;
    }

    public boolean[] getBlockArray() {
        return blockIds;
    }

    /**
     * Get the list of blocks that are tested with.
     *
     * @return a list of blocks
     */
    public Collection<BaseBlock> getBlocks() {
        if (computedLegacyList != null) {
            return computedLegacyList;
        }
        computedLegacyList = new LinkedHashSet<>();
        for (int id = 0; id < FaweCache.getId(blocks.length); id++) {
            boolean all = true;
            ArrayList<BaseBlock> tmp = new ArrayList<BaseBlock>(16);
            for (int data = 0; data < 16; data++) {
                if (blocks[FaweCache.getCombined(id, data)]) {
                    tmp.add(FaweCache.getBlock(id, data));
                }
            }
            if (tmp.size() == 16) {
                computedLegacyList.add(new BaseBlock(id, -1));
            } else {
                computedLegacyList.addAll(tmp);
            }
        }
        return computedLegacyList;
    }

    public Collection<BaseBlock> getInverseBlocks() {
        if (computedLegacyList != null) {
            return computedLegacyList;
        }
        computedLegacyList = new LinkedHashSet<>();
        for (int id = 0; id < FaweCache.getId(blocks.length); id++) {
            boolean all = true;
            ArrayList<BaseBlock> tmp = new ArrayList<BaseBlock>(16);
            for (int data = 0; data < 16; data++) {
                if (!blocks[FaweCache.getCombined(id, data)]) {
                    tmp.add(FaweCache.getBlock(id, data));
                }
            }
            if (tmp.size() == 16) {
                computedLegacyList.add(new BaseBlock(id, -1));
            } else {
                computedLegacyList.addAll(tmp);
            }
        }
        return computedLegacyList;
    }

    @Override
    public String toString() {
        return StringMan.getString(getBlocks());
    }

    public boolean test(int blockId) {
        return blockIds[blockId];
    }

    public boolean test(int blockId, int data) {
        return blocks[FaweCache.getCombined(blockId, data)];
    }

    @Override
    public boolean test(Vector vector) {
        BaseBlock block = getExtent().getBlock(vector);
        return blocks[FaweCache.getCombined(block)];
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }

    public static Class<?> inject() {
        return BlockMask.class;
    }
}
package com.sk89q.worldedit.function.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @deprecated Just use BaseBlock directly
 */
@Deprecated
public class BlockPattern implements Pattern {

    private BaseBlock block;

    public BlockPattern(BaseBlock block) {
        this.block = block;
    }

    public BlockPattern(int id) {
        this.block = FaweCache.getBlock(id, 0);
    }

    public BlockPattern(int id, int data) {
        this.block = FaweCache.getBlock(id, data);
    }

    @Override
    public BaseBlock apply(Vector position) {
        return block;
    }

    @Override
    public BaseBlock apply(int x, int y, int z) {
        return block;
    }

    /**
     * Get the block.
     *
     * @return the block that is always returned
     */
    public BaseBlock getBlock() {
        return block;
    }

    /**
     * Set the block that is returned.
     *
     * @param block the block
     */
    public void setBlock(BaseBlock block) {
        checkNotNull(block);
        this.block = block;
    }

    public static Class<BlockPattern> inject() {
        return BlockPattern.class;
    }
}

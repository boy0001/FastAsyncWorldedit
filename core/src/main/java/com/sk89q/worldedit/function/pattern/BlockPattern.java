package com.sk89q.worldedit.function.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;


import static com.google.common.base.Preconditions.checkNotNull;

public class BlockPattern extends AbstractPattern {

    private BaseBlock block;

    public BlockPattern(BaseBlock block) {
        this.block = block;
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

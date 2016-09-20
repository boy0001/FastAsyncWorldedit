package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import java.util.Collection;

public class FuzzyBlockMask extends BlockMask {

    public FuzzyBlockMask(Extent extent, Collection<BaseBlock> blocks) {
        super(extent, blocks);
    }

    public FuzzyBlockMask(Extent extent, BaseBlock... block) {
        super(extent, block);
    }

    @Override
    public boolean test(Vector vector) {
        Extent extent = getExtent();
        Collection<BaseBlock> blocks = getBlocks();
        BaseBlock lazyBlock = extent.getBlock(vector);
        if (lazyBlock.getData() == -1) {
            return test(lazyBlock.getId());
        } else {
            return test(lazyBlock.getId(), lazyBlock.getData());
        }
    }

    public static Class<?> inject() {
        return FuzzyBlockMask.class;
    }
}
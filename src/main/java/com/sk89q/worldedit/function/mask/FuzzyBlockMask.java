package com.sk89q.worldedit.function.mask;

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

    public static Class<?> inject() {
        return FuzzyBlockMask.class;
    }
}
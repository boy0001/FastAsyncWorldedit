package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.pattern.AbstractPattern;

public class LinearBlockPattern extends AbstractPattern {

    private final BaseBlock[] blocks;
    private int index;

    public LinearBlockPattern(BaseBlock[] blocks) {
        this.blocks = blocks;
    }

    @Override
    public BaseBlock apply(Vector position) {
        if (index == blocks.length) {
            index = 0;
        }
        return blocks[index++];
    }
}

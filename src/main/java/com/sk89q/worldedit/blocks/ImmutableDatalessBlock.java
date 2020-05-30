package com.sk89q.worldedit.blocks;

public class ImmutableDatalessBlock extends ImmutableBlock {
    public ImmutableDatalessBlock(int id) {
        super(id, 0);
    }

    @Override
    public int getData() {
        return 0;
    }

    @Override
    public boolean equals(BaseBlock block) {
        return block.getId() == this.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BaseBlock) {
            return ((BaseBlock) o).getId() == this.getId();
        } else {
            return false;
        }
    }

    @Override
    public boolean canStoreNBTData() {
        return false;
    }
}

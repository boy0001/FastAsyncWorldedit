package com.sk89q.worldedit.blocks;

public class ImmutableNBTBlock extends ImmutableBlock {
    public ImmutableNBTBlock(int id, int data) {
        super(id, data);
    }

    @Override
    public boolean canStoreNBTData() {
        return true;
    }
}

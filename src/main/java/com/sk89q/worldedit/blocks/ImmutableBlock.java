package com.sk89q.worldedit.blocks;

import com.sk89q.worldedit.CuboidClipboard;

public class ImmutableBlock extends BaseBlock {
    private final int combined;

    public ImmutableBlock(int id, int data) {
        super(id, data);
        this.combined = super.getCombined();
    }

    @Override
    public final int getCombined() {
        return combined;
    }

    @Override
    public void setData(int data) {
        throw new IllegalStateException("Cannot set data");
    }

    @Override
    public void setId(int id) {
        throw new IllegalStateException("Cannot set id");
    }

    @Override
    public BaseBlock flip() {
        BaseBlock clone = new BaseBlock(getId(), getData(), getNbtData());
        return clone.flip();
    }

    @Override
    public BaseBlock flip(CuboidClipboard.FlipDirection direction) {
        BaseBlock clone = new BaseBlock(getId(), getData(), getNbtData());
        return clone.flip(direction);
    }

    @Override
    public boolean canStoreNBTData() {
        return false;
    }

    @Override
    public boolean isImmutable() {
        return true;
    }
}

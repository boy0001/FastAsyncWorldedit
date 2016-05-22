package com.boydti.fawe.object;

import com.boydti.fawe.util.DelegateFaweQueue;
import com.boydti.fawe.util.WEManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class MaskedFaweQueue extends DelegateFaweQueue {
    private RegionWrapper[] mask;

    public MaskedFaweQueue(FaweQueue parent, RegionWrapper[] mask) {
        super(parent);
        this.mask = mask;
    }

    public void setMask(RegionWrapper[] mask) {
        this.mask = mask;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        if (WEManager.IMP.maskContains(mask, x, z)) {
            super.setTile(x, y, z, tag);
        }
    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {
        if (WEManager.IMP.maskContains(mask, x, z)) {
            super.setEntity(x, y, z, tag);
        }
    }

    @Override
    public boolean setBlock(int x, int y, int z, short id, byte data) {
        if (WEManager.IMP.maskContains(mask, x, z)) {
            return super.setBlock(x, y, z, id, data);
        }
        return false;
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        if (WEManager.IMP.maskContains(mask, x, z)) {
            return super.setBiome(x, z, biome);
        }
        return false;
    }
}

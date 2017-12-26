package com.boydti.fawe.object;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.queue.DelegateFaweQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class ChangeSetFaweQueue extends DelegateFaweQueue {
    private FaweChangeSet set;

    public ChangeSetFaweQueue(FaweChangeSet set, FaweQueue parent) {
        super(parent);
        this.set = set;
    }

    public FaweChangeSet getChangeSet() {
        return set;
    }

    public void setChangeSet(FaweChangeSet set) {
        this.set = set;
    }

    @Override
    public boolean setBlock(int x, int y, int z, int id, int data) {
        if (super.setBlock(x, y, z, id, data)) {
            int combinedFrom = getParent().getCombinedId4Data(x, y, z);
            if (FaweCache.hasNBT(combinedFrom >> 4)) {
                CompoundTag nbt = getParent().getTileEntity(x, y, z);
                if (nbt != null) {
                    set.addTileRemove(nbt);
                }
            }
            int combinedTo = (id << 4) + data;
            set.add(x, y, z, combinedFrom, combinedTo);
            return true;
        }
        return false;
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        if (super.setBiome(x, z, biome)) {
            int oldBiome = getParent().getBiomeId(x, z);
            if (oldBiome != biome.getId()) {
                set.addBiomeChange(x, z, FaweCache.getBiome(oldBiome), biome);
                return true;
            }
        }
        return false;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        super.setTile(x, y, z, tag);
        set.addTileCreate(tag);
    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {
        super.setEntity(x, y, z, tag);
        set.addEntityCreate(tag);
    }
}

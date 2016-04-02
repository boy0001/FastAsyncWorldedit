package com.boydti.fawe.object;

import com.boydti.fawe.util.SetQueue;

/**
 */
public class FaweLocation {

    public final int x;
    public final int y;
    public final int z;
    public final String world;

    public FaweLocation(final String world, final int x, final int y, final int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final FaweLocation other = (FaweLocation) obj;
        return ((this.x == other.x) && (this.y == other.y) && (this.z == other.z) && (this.world.equals(other.world)));
    }

    @Override
    public int hashCode() {
        return this.x << (8 + this.z) << (4 + this.y);
    }

    public void setBlockAsync(final short id, final byte data) {
        SetQueue.IMP.setBlock(this.world, this.x, this.y, this.z, id, data);
    }
}

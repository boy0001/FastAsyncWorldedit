package com.boydti.fawe.object;

import com.boydti.fawe.util.SetBlockQueue;

/**
 */
public class FaweLocation {
    
    
    public final int x;
    public final int y;
    public final int z;
    public final String world;
    
    public FaweLocation(String world, int x, int y, int z) {
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FaweLocation other = (FaweLocation) obj;
        return ((x == other.x) && (y == other.y) && (z == other.z) && (world.equals(other.world)));
    }
    
    @Override
    public int hashCode() {
        return x << 8 + z << 4 + y;
    }
    
    public void setBlockAsync(short id, byte data) {
        SetBlockQueue.IMP.setBlock(world, x, y, z, id, data);
    }
}

package com.boydti.fawe.object;

public class ChunkLoc {
    public final int x;
    public final int z;
    public final String world;
    
    public ChunkLoc(final String world, final int x, final int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }
    
    @Override
    public int hashCode() {
        int result;
        if (x >= 0) {
            if (z >= 0) {
                result = (x * x) + (3 * x) + (2 * x * z) + z + (z * z);
            } else {
                final int y1 = -z;
                result = (x * x) + (3 * x) + (2 * x * y1) + y1 + (y1 * y1) + 1;
            }
        } else {
            final int x1 = -x;
            if (z >= 0) {
                result = -((x1 * x1) + (3 * x1) + (2 * x1 * z) + z + (z * z));
            } else {
                final int y1 = -z;
                result = -((x1 * x1) + (3 * x1) + (2 * x1 * y1) + y1 + (y1 * y1) + 1);
            }
        }
        result = (result * 31) + world.hashCode();
        return result;
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
        final ChunkLoc other = (ChunkLoc) obj;
        return ((x == other.x) && (z == other.z) && (world.equals(other.world)));
    }
    
    @Override
    public String toString() {
        return world + ":" + x + "," + z;
    }
}

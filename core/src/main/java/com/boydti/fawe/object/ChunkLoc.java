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
        if (this.x >= 0) {
            if (this.z >= 0) {
                result = (this.x * this.x) + (3 * this.x) + (2 * this.x * this.z) + this.z + (this.z * this.z);
            } else {
                final int y1 = -this.z;
                result = (this.x * this.x) + (3 * this.x) + (2 * this.x * y1) + y1 + (y1 * y1) + 1;
            }
        } else {
            final int x1 = -this.x;
            if (this.z >= 0) {
                result = -((x1 * x1) + (3 * x1) + (2 * x1 * this.z) + this.z + (this.z * this.z));
            } else {
                final int y1 = -this.z;
                result = -((x1 * x1) + (3 * x1) + (2 * x1 * y1) + y1 + (y1 * y1) + 1);
            }
        }
        result = (result * 31) + this.world.hashCode();
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
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final ChunkLoc other = (ChunkLoc) obj;
        return ((this.x == other.x) && (this.z == other.z) && (this.world.equals(other.world)));
    }

    @Override
    public String toString() {
        return this.world + ":" + this.x + "," + this.z;
    }
}

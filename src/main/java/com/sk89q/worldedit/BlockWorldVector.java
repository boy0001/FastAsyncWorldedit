package com.sk89q.worldedit;

/**
 * @deprecated
 */
@Deprecated
public class BlockWorldVector extends WorldVector {
    public BlockWorldVector(WorldVector position) {
        super(position.getWorld(), position);
    }

    public BlockWorldVector(LocalWorld world, Vector position) {
        super(world, position);
    }

    public BlockWorldVector(WorldVector world, int x, int y, int z) {
        super(world.getWorld(), x, y, z);
    }

    public BlockWorldVector(WorldVector world, Vector v) {
        super(world.getWorld(), v.getX(), v.getY(), v.getZ());
    }

    public BlockWorldVector(LocalWorld world, int x, int y, int z) {
        super(world, x, y, z);
    }

    public BlockWorldVector(LocalWorld world, float x, float y, float z) {
        super(world, x, y, z);
    }

    public BlockWorldVector(LocalWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Vector)) {
            return false;
        } else {
            Vector other = (Vector) obj;
            return (int) other.getX() == (int) this.getX() && (int) other.getY() == (int) this.getY() && (int) other.getZ() == (int) this.getZ();
        }
    }

    @Override
    public int hashCode() {
        return ((int) getX() ^ ((int) getZ() << 16)) ^ ((int) getY() << 30);
    }

    public static Class<?> inject() {
        return BlockWorldVector.class;
    }
}

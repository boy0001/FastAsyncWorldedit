package com.sk89q.worldedit;

public class MutableBlockVector extends BlockVector {
    private int x,y,z;

    public MutableBlockVector(int x, int y, int z) {
        super(0, 0, 0);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableBlockVector() {
        super(0, 0, 0);
    }

    @Override
    public Vector setComponents(double x, double y, double z) {
        return this.setComponents((int) x, (int) y, (int) z);
    }

    @Override
    public final void mutX(double x) {
        this.x = (int) x;
    }

    @Override
    public final void mutY(double y) {
        this.y = (int) y;
    }

    @Override
    public final void mutZ(double z) {
        this.z = (int) z;
    }

    @Override
    public final void mutX(int x) {
        this.x = x;
    }

    @Override
    public final void mutY(int y) {
        this.y = y;
    }

    @Override
    public final void mutZ(int z) {
        this.z = z;
    }

    @Override
    public final double getX() {
        return x;
    }

    @Override
    public final double getY() {
        return y;
    }

    @Override
    public final double getZ() {
        return z;
    }

    @Override
    public int getBlockX() {
        return this.x;
    }

    @Override
    public int getBlockY() {
        return this.y;
    }

    @Override
    public int getBlockZ() {
        return this.z;
    }
}

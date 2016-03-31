package com.boydti.fawe.object;

import com.sk89q.worldedit.Vector;

public class RegionWrapper {
    public int minX;
    public int maxX;
    public int minZ;
    public int maxZ;

    public RegionWrapper(final int minX, final int maxX, final int minZ, final int maxZ) {
        this.maxX = maxX;
        this.minX = minX;
        this.maxZ = maxZ;
        this.minZ = minZ;
    }

    public RegionWrapper(final Vector pos1, final Vector pos2) {
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public boolean isIn(final int x, final int z) {
        return ((x >= this.minX) && (x <= this.maxX) && (z >= this.minZ) && (z <= this.maxZ));
    }

    @Override
    public String toString() {
        return this.minX + "," + this.minZ + "->" + this.maxX + "," + this.maxZ;
    }

    public Vector getBottomVector() {
        return new Vector(this.minX, 1, this.minZ);
    }

    public Vector getTopVector() {
        return new Vector(this.maxX, 255, this.maxZ);
    }
}

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
    
    public RegionWrapper(Vector pos1, Vector pos2) {
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public boolean isIn(final int x, final int z) {
        return ((x >= minX) && (x <= maxX) && (z >= minZ) && (z <= maxZ));
    }
    
    @Override
    public String toString() {
        return minX + "," + minZ + "->" + maxX + "," + maxZ;
    }
    
    public Vector getBottomVector() {
        return new Vector(minX, 1, minZ);
    }
    
    public Vector getTopVector() {
        return new Vector(maxX, 255, maxZ);
    }
}

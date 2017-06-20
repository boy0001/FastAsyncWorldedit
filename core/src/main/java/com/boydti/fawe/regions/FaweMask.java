package com.boydti.fawe.regions;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.sk89q.worldedit.BlockVector;
import java.util.Arrays;
import java.util.HashSet;

public class FaweMask {
    private String description = null;
    private BlockVector position1;
    private BlockVector position2;

    public FaweMask(final BlockVector pos1, final BlockVector pos2, final String id) {
        if ((pos1 == null) || (pos2 == null)) {
            throw new IllegalArgumentException("BlockVectors cannot be null!");
        }
        this.description = id;
        this.position1 = new BlockVector(Math.min(pos1.getBlockX(), pos2.getBlockX()), 0, Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
        this.position2 = new BlockVector(Math.max(pos1.getBlockX(), pos2.getBlockX()), 256, Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    public FaweMask(final BlockVector pos1, final BlockVector pos2) {
        if ((pos1 == null) || (pos2 == null)) {
            throw new IllegalArgumentException("BlockVectors cannot be null!");
        }
        this.position1 = new BlockVector(Math.min(pos1.getBlockX(), pos2.getBlockX()), 0, Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
        this.position2 = new BlockVector(Math.max(pos1.getBlockX(), pos2.getBlockX()), 256, Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    public HashSet<RegionWrapper> getRegions() {
        final BlockVector lower = this.getLowerBound();
        final BlockVector upper = this.getUpperBound();
        return new HashSet<>(Arrays.asList(new RegionWrapper(lower.getBlockX(), upper.getBlockX(), lower.getBlockY(), upper.getBlockY(), lower.getBlockZ(), upper.getBlockZ())));
    }

    public String getName() {
        return this.description;
    }

    public BlockVector getLowerBound() {
        return this.position1;
    }

    public BlockVector getUpperBound() {
        return this.position2;
    }

    public void setBounds(final BlockVector pos1, final BlockVector pos2) {
        if ((pos1 == null) || (pos2 == null)) {
            throw new IllegalArgumentException("BlockVectors cannot be null!");
        }
        this.position1 = new BlockVector(Math.min(pos1.getBlockX(), pos2.getBlockX()), 0, Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
        this.position2 = new BlockVector(Math.max(pos1.getBlockX(), pos2.getBlockX()), 256, Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    public boolean isValid(FawePlayer player, FaweMaskManager.MaskType type) {
        return false;
    }

    ;

    public BlockVector[] getBounds() {
        final BlockVector[] BlockVectors = {this.position1, this.position2};
        return BlockVectors;
    }

    public boolean contains(final BlockVector loc) {
        if (loc.getBlockX() < this.position1.getBlockX()) {
            return false;
        }
        if (loc.getBlockX() > this.position2.getBlockX()) {
            return false;
        }
        if (loc.getBlockZ() < this.position1.getBlockZ()) {
            return false;
        }
        if (loc.getBlockZ() > this.position2.getBlockZ()) {
            return false;
        }
        if (loc.getBlockY() < this.position1.getBlockY()) {
            return false;
        }
        if (loc.getBlockY() > this.position2.getBlockY()) {
            return false;
        }
        return true;
    }
}
package com.boydti.fawe.bukkit.regions;

import java.util.Arrays;
import java.util.HashSet;

import org.bukkit.Location;

import com.boydti.fawe.object.RegionWrapper;

public class FaweMask {
    private String description = null;
    private Location position1;
    private Location position2;

    public FaweMask(final Location pos1, final Location pos2, final String id) {
        if ((pos1 == null) || (pos2 == null)) {
            throw new IllegalArgumentException("Locations cannot be null!");
        }
        if (pos1.getWorld().equals(pos2.getWorld()) == false) {
            throw new IllegalArgumentException("Locations must be in the same world!");
        }
        this.description = id;
        this.position1 = new Location(pos1.getWorld(), Math.min(pos1.getBlockX(), pos2.getBlockX()), 0, Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
        this.position2 = new Location(pos1.getWorld(), Math.max(pos1.getBlockX(), pos2.getBlockX()), 256, Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    public FaweMask(final Location pos1, final Location pos2) {
        if ((pos1 == null) || (pos2 == null)) {
            throw new IllegalArgumentException("Locations cannot be null!");
        }
        if (pos1.getWorld().equals(pos2.getWorld()) == false) {
            throw new IllegalArgumentException("Locations must be in the same world!");
        }
        this.position1 = new Location(pos1.getWorld(), Math.min(pos1.getBlockX(), pos2.getBlockX()), 0, Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
        this.position2 = new Location(pos1.getWorld(), Math.max(pos1.getBlockX(), pos2.getBlockX()), 256, Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    public HashSet<RegionWrapper> getRegions() {
        final Location lower = this.getLowerBound();
        final Location upper = this.getUpperBound();
        return new HashSet<>(Arrays.asList(new RegionWrapper(lower.getBlockX(), upper.getBlockX(), lower.getBlockZ(), upper.getBlockZ())));
    }

    public String getName() {
        return this.description;
    }

    public Location getLowerBound() {
        return this.position1;
    }

    public Location getUpperBound() {
        return this.position2;
    }

    public void setBounds(final Location pos1, final Location pos2) {
        if ((pos1 == null) || (pos2 == null)) {
            throw new IllegalArgumentException("Locations cannot be null!");
        }
        if (pos1.getWorld().equals(pos2.getWorld()) == false) {
            throw new IllegalArgumentException("Locations must be in the same world!");
        }
        this.position1 = new Location(pos1.getWorld(), Math.min(pos1.getBlockX(), pos2.getBlockX()), 0, Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
        this.position2 = new Location(pos1.getWorld(), Math.max(pos1.getBlockX(), pos2.getBlockX()), 256, Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    public Location[] getBounds() {
        final Location[] locations = { this.position1, this.position2 };
        return locations;
    }

    public boolean contains(final Location loc) {
        if (this.position1.getWorld().equals(loc.getWorld())) {
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
        } else {
            return false;
        }
        return true;
    }
}

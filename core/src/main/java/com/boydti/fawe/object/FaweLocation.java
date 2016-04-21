package com.boydti.fawe.object;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.world.World;

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

    public World getWorld() {
        for (World world : WorldEdit.getInstance().getServer().getWorlds()) {
            if (world.getName().equals(this.world)) {
                return world;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return this.x << (8 + this.z) << (4 + this.y);
    }
}

package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import javax.annotation.Nullable;

public class ScaleTransform extends TransformExtent {
    private final Vector mutable = new Vector();
    private final double dx,dy,dz;
    private int maxy;

    private Vector min;

    public ScaleTransform(Extent parent, double dx, double dy, double dz) {
        super(parent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.maxy = parent.getMaximumPoint().getBlockY();
    }

    @Override
    public TransformExtent setExtent(Extent extent) {
        min = null;
        maxy = extent.getMaximumPoint().getBlockY();
        return super.setExtent(extent);
    }

    private Vector getPos(Vector pos) {
        if (min == null) {
            min = new Vector(pos);
        }
        mutable.x = min.x + (pos.x - min.x) * dx;
        mutable.y = min.y + (pos.y - min.y) * dy;
        mutable.z = min.z + (pos.z - min.z) * dz;
        return mutable;
    }

    private Vector getPos(int x, int y, int z) {
        if (min == null) {
            min = new Vector(x, y, z);
        }
        mutable.x = min.x + (x - min.x) * dx;
        mutable.y = min.y + (y - min.y) * dy;
        mutable.z = min.z + (z - min.z) * dz;
        return mutable;
    }


    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        boolean result = false;
        Vector pos = getPos(location);
        double sx = pos.x;
        double sy = pos.y;
        double sz = pos.z;
        double ex = sx + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = sz + dz;
        for (pos.y = sy; pos.y < ey; pos.y++) {
            for (pos.z = sz; pos.z < ez; pos.z++) {
                for (pos.x = sx; pos.x < ex; pos.x++) {
                    result |= super.setBlock(pos, block);
                }
            }
        }
        return result;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        boolean result = false;
        Vector pos = getPos(position.getBlockX(), 0, position.getBlockZ());
        double sx = pos.x;
        double sz = pos.z;
        double ex = pos.x + dx;
        double ez = pos.z + dz;
            for (pos.z = sz; pos.z < ez; pos.z++) {
                for (pos.x = sx; pos.x < ex; pos.x++) {
                result |= super.setBiome(pos.toVector2D(), biome);
            }
        }
        return result;
    }

    @Override
    public boolean setBlock(int x1, int y1, int z1, BaseBlock block) throws WorldEditException {
        boolean result = false;
        Vector pos = getPos(x1, y1, z1);
        double sx = pos.x;
        double sy = pos.y;
        double sz = pos.z;
        double ex = pos.x + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = pos.z + dz;
        for (pos.y = sy; pos.y < ey; pos.y++) {
            for (pos.z = sz; pos.z < ez; pos.z++) {
                for (pos.x = sx; pos.x < ex; pos.x++) {
                    result |= super.setBlock(pos, block);
                }
            }
        }
        return result;
    }



    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        Location newLoc = new Location(location.getExtent(), getPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), location.getYaw(), location.getPitch());
        return super.createEntity(newLoc, entity);
    }
}

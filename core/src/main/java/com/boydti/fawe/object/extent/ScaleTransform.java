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

public class ScaleTransform extends ResettableExtent {
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
    public ResettableExtent setExtent(Extent extent) {
        min = null;
        maxy = extent.getMaximumPoint().getBlockY();
        return super.setExtent(extent);
    }

    private Vector getPos(Vector pos) {
        if (min == null) {
            min = new Vector(pos);
        }
        mutable.x = (min.getX() + (pos.getX() - min.getX()) * dx);
        mutable.y = (min.getY() + (pos.getY() - min.getY()) * dy);
        mutable.z = (min.getZ() + (pos.getZ() - min.getZ()) * dz);
        return mutable;
    }

    private Vector getPos(int x, int y, int z) {
        if (min == null) {
            min = new Vector(x, y, z);
        }
        mutable.x = (min.getX() + (x - min.getX()) * dx);
        mutable.y = (min.getY() + (y - min.getY()) * dy);
        mutable.z = (min.getZ() + (z - min.getZ()) * dz);
        return mutable;
    }


    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        boolean result = false;
        Vector pos = getPos(location);
        double sx = pos.getX();
        double sy = pos.getY();
        double sz = pos.getZ();
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
        double sx = pos.getX();
        double sz = pos.getZ();
        double ex = pos.getX() + dx;
        double ez = pos.getZ() + dz;
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
        double sx = pos.getX();
        double sy = pos.getY();
        double sz = pos.getZ();
        double ex = pos.getX() + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = pos.getZ() + dz;
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

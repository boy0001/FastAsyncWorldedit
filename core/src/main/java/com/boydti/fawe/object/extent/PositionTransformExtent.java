package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class PositionTransformExtent extends ResettableExtent {

    private final Vector mutable = new Vector();
    private Transform transform;
    private Vector min;

    public PositionTransformExtent(Extent parent, Transform transform) {
        super(parent);
        this.transform = transform;
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        min = null;
        return super.setExtent(extent);
    }

    public void setOrigin(Vector pos) {
        this.min = pos;
    }

    private Vector getPos(Vector pos) {
        if (min == null) {
            min = new Vector(pos);
        }
        mutable.x = ((pos.getX() - min.getX()));
        mutable.y = ((pos.getY() - min.getY()));
        mutable.z = ((pos.getZ() - min.getZ()));
        Vector tmp = transform.apply(mutable);
        tmp.x = (tmp.getX() + min.getX());
        tmp.y = (tmp.getY() + min.getY());
        tmp.z = (tmp.getZ() + min.getZ());
        return tmp;
    }

    private Vector getPos(int x, int y, int z) {
        if (min == null) {
            min = new Vector(x, y, z);
        }
        mutable.x = ((x - min.getX()));
        mutable.y = ((y - min.getY()));
        mutable.z = ((z - min.getZ()));
        Vector tmp = transform.apply(mutable);
        tmp.x = (tmp.getX() + min.getX());
        tmp.y = (tmp.getY() + min.getY());
        tmp.z = (tmp.getZ() + min.getZ());
        return tmp;
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        return super.getLazyBlock(getPos(x, y, z));
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return super.getLazyBlock(getPos(position));
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return super.getBlock(getPos(position));
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        mutable.x = position.getBlockX();
        mutable.z = position.getBlockZ();
        mutable.y = 0;
        return super.getBiome(getPos(mutable).toVector2D());
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        return super.setBlock(getPos(x, y, z), block);
    }


    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        return super.setBlock(getPos(location), block);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        mutable.x = position.getBlockX();
        mutable.z = position.getBlockZ();
        mutable.y = 0;
        return super.setBiome(getPos(mutable).toVector2D(), biome);
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }
}

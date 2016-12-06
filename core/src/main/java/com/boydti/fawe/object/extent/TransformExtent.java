package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BlockRegistry;

public class TransformExtent extends BlockTransformExtent {

    private final Vector mutable = new Vector();
    private Vector min;
    private int maxy;

    public TransformExtent(Extent parent, BlockRegistry registry) {
        super(parent, registry);
        this.maxy = parent.getMaximumPoint().getBlockY();
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        min = null;
        maxy = extent.getMaximumPoint().getBlockY();
        return super.setExtent(extent);
    }

    public void setOrigin(Vector pos) {
        this.min = pos;
    }

    private Vector getPos(Vector pos) {
        if (min == null) {
            min = new Vector(pos);
        }
        mutable.x = (pos.x - min.x);
        mutable.y = (pos.y - min.y);
        mutable.z = (pos.z - min.z);
        Vector tmp = getTransform().apply(mutable);
        tmp.x += min.x;
        tmp.y += min.y;
        tmp.z += min.z;
        return tmp;
    }

    private Vector getPos(int x, int y, int z) {
        if (min == null) {
            min = new Vector(x, y, z);
        }
        mutable.x = (x - min.x);
        mutable.y = (y - min.y);
        mutable.z = (z - min.z);
        Vector tmp = getTransform().apply(mutable);
        tmp.x += min.x;
        tmp.y += min.y;
        tmp.z += min.z;
        return tmp;
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        return transformFast(super.getLazyBlock(getPos(x, y, z)));
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return transformFast(super.getLazyBlock(getPos(position)));
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return transformFast(super.getBlock(getPos(position)));
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
        return super.setBlock(getPos(x, y, z), transformFastInverse(block));
    }


    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        return super.setBlock(getPos(location), transformFastInverse(block));
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        mutable.x = position.getBlockX();
        mutable.z = position.getBlockZ();
        mutable.y = 0;
        return super.setBiome(getPos(mutable).toVector2D(), biome);
    }
}

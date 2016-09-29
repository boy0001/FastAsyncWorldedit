package com.boydti.fawe.object.extent;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BlockRegistry;

public class AffineTransformExtent extends TransformExtent {
    private final Vector mutable = new Vector();
    private final BlockRegistry registry;
    private int maxy;
    private AffineTransform affine;
    private BaseBlock[] BLOCK_TRANSFORM;
    private BaseBlock[] BLOCK_TRANSFORM_INVERSE;

    private Vector min;

    public AffineTransformExtent(Extent parent, BlockRegistry registry) {
        super(parent);
        this.maxy = parent.getMaximumPoint().getBlockY();
        this.affine = new AffineTransform();
        this.registry = registry;
    }

    private void cache() {
        BLOCK_TRANSFORM = new BaseBlock[FaweCache.CACHE_BLOCK.length];
        BLOCK_TRANSFORM_INVERSE = new BaseBlock[FaweCache.CACHE_BLOCK.length];
        Transform inverse = affine.inverse();
        for (int i = 0; i < BLOCK_TRANSFORM.length; i++) {
            BaseBlock block = FaweCache.CACHE_BLOCK[i];
            if (block != null) {
                BLOCK_TRANSFORM[i] = BlockTransformExtent.transform(new BaseBlock(block), affine, registry);
                BLOCK_TRANSFORM_INVERSE[i] = BlockTransformExtent.transform(new BaseBlock(block), inverse, registry);
            }
        }
    }

    @Override
    public TransformExtent setExtent(Extent extent) {
        min = null;
        maxy = extent.getMaximumPoint().getBlockY();
        return super.setExtent(extent);
    }

    public AffineTransform getAffine() {
        return affine;
    }

    public void setAffine(AffineTransform affine) {
        this.affine = affine;
        cache();
    }

    private Vector getPos(Vector pos) {
        if (min == null) {
            min = new Vector(pos);
        }
        mutable.x = (pos.x - min.x);
        mutable.y = (pos.y - min.y);
        mutable.z = (pos.z - min.z);
        Vector tmp = affine.apply(mutable);
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
        Vector tmp = affine.apply(mutable);
        tmp.x += min.x;
        tmp.y += min.y;
        tmp.z += min.z;
        return tmp;
    }

    private final BaseBlock transformFast(BaseBlock block) {
        return BLOCK_TRANSFORM[FaweCache.getCombined(block)];
    }

    private final BaseBlock transformFastInverse(BaseBlock block) {
        return BLOCK_TRANSFORM_INVERSE[FaweCache.getCombined(block)];
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

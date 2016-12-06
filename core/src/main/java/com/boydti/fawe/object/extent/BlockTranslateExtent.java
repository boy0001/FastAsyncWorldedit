package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class BlockTranslateExtent extends AbstractDelegateExtent {
    private final int dx,dy,dz;
    private final Extent extent;
    private Vector mutable = new Vector();

    public BlockTranslateExtent(Extent extent, int dx, int dy, int dz) {
        super(extent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.extent = extent;
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        mutable.x = location.x + dx;
        mutable.y = location.y + dy;
        mutable.z = location.z + dz;
        return extent.setBlock(mutable, block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        mutable.x = x + dx;
        mutable.y = y + dy;
        mutable.z = z + dz;
        return extent.setBlock(mutable, block);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return super.setBiome(position.add(dx, dz), biome);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return super.getBiome(position.add(dx, dz));
    }

    @Override
    public BaseBlock getBlock(Vector location) {
        return getLazyBlock((int) location.x, (int) location.y, (int) location.z);
    }

    @Override
    public BaseBlock getLazyBlock(Vector location) {
        return getLazyBlock((int) location.x, (int) location.y, (int) location.z);
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        return super.getLazyBlock(x + dx, y + dy, z + dz);
    }
}

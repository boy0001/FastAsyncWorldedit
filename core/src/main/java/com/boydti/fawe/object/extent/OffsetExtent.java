package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class OffsetExtent extends ResettableExtent {
    private final int dx, dy, dz;
    private MutableBlockVector2D mutable = new MutableBlockVector2D();

    public OffsetExtent(Extent parent, int dx, int dy, int dz) {
        super(parent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return getExtent().setBiome(mutable.setComponents(position.getBlockX() + dx, position.getBlockZ() + dz), biome);
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        return getExtent().setBlock(location.getBlockX() + dx, location.getBlockY() + dy, location.getBlockZ() + dz, block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        return getExtent().setBlock(x + dx, y + dy, z + dz, block);
    }
}

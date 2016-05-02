package com.boydti.fawe.object.extent;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class NullExtent implements Extent {

    private final BBC reason;

    /**
     * Create a new instance.
     */
    public NullExtent(BBC failReason) {
        this.reason = failReason;
    }

    @Override
    public BaseBiome getBiome(final Vector2D arg0) {
        throw new FaweException(reason);
    }

    @Override
    public BaseBlock getBlock(final Vector arg0) {
        throw new FaweException(reason);
    }

    @Override
    public BaseBlock getLazyBlock(final Vector arg0) {
        throw new FaweException(reason);
    }

    @Override
    public boolean setBiome(final Vector2D arg0, final BaseBiome arg1) {
        throw new FaweException(reason);
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    @Override
    public boolean setBlock(final Vector arg0, final BaseBlock arg1) throws WorldEditException {
        throw new FaweException(reason);
    }

    @Override
    public Entity createEntity(final Location arg0, final BaseEntity arg1) {
        throw new FaweException(reason);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Entity> getEntities(final Region arg0) {
        return new ArrayList<>();
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(0, 0, 0);
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(0, 0, 0);
    }
}

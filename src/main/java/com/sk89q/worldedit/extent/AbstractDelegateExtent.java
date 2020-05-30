/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent;

import com.boydti.fawe.object.extent.LightingExtent;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.util.List;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base class for {@link Extent}s that merely passes extents onto another.
 */
public class AbstractDelegateExtent implements LightingExtent {

    private transient final Extent extent;
    private MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public AbstractDelegateExtent(Extent extent) {
        checkNotNull(extent);
        this.extent = extent;
    }

    public int getSkyLight(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getSkyLight(x, y, z);
        }
        return 0;
    }

    @Override
    public int getMaxY() {
        return extent.getMaxY();
    }

    public int getBlockLight(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getBlockLight(x, y, z);
        }
        return getBrightness(x, y, z);
    }

    public int getOpacity(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getOpacity(x, y, z);
        }
        BlockMaterial block = BundledBlockData.getInstance().getMaterialById(getLazyBlock(x, y, z).getId());
        if (block == null) {
            return 15;
        }
        return Math.min(15, block.getLightOpacity());
    }

    @Override
    public int getLight(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getLight(x, y, z);
        }
        return 0;
    }

    public int getBrightness(int x, int y, int z) {
        if (extent instanceof LightingExtent) {
            return ((LightingExtent) extent).getBrightness(x, y, z);
        }
        BlockMaterial block = BundledBlockData.getInstance().getMaterialById(getLazyBlock(x, y, z).getId());
        if (block == null) {
            return 15;
        }
        return Math.min(15, block.getLightValue());
    }

    /**
     * Get the extent.
     *
     * @return the extent
     */
    public Extent getExtent() {
        return extent;
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return extent.getLazyBlock(position);
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        mutable.mutX(x);
        mutable.mutY(y);
        mutable.mutZ(z);
        return extent.getLazyBlock(mutable);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return extent.getLazyBlock(position);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        mutable.mutX(x);
        mutable.mutY(y);
        mutable.mutZ(z);
        return setBlock(mutable, block);
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        return extent.setBlock(location, block);
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return extent.createEntity(location, entity);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return extent.getEntities();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return extent.getEntities(region);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return extent.getBiome(position);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return extent.setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BaseBiome biome) {
        return extent.setBiome(x, y, z, biome);
    }

    @Override
    public Vector getMinimumPoint() {
        return extent.getMinimumPoint();
    }

    @Override
    public Vector getMaximumPoint() {
        return extent.getMaximumPoint();
    }

    protected Operation commitBefore() {
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + extent.toString();
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        return extent.getNearestSurfaceLayer(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax);
    }

    @Override
    public @Nullable Operation commit() {
        Operation ours = commitBefore();
        Operation other = null;
        if (extent != this) other = extent.commit();
        if (ours != null && other != null) {
            return new OperationQueue(ours, other);
        } else if (ours != null) {
            return ours;
        } else if (other != null) {
            return other;
        } else {
            return null;
        }
    }

    public static Class<?> inject() {
        return AbstractDelegateExtent.class;
    }
}

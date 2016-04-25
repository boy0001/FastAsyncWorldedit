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

package com.sk89q.worldedit.extent.clipboard;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores block data as a multi-dimensional array of {@link BaseBlock}s and
 * other data as lists or maps.
 */
public class BlockArrayClipboard implements Clipboard {

    private final Region region;

    public FaweClipboard IMP;

    private final Vector size;


    private int mx;
    private int my;
    private int mz;

    private int dx;
    private int dxz;

    private Vector origin;

    public BlockArrayClipboard(Region region) {
        checkNotNull(region);
        this.region = region.clone();
        this.size = getDimensions();
        this.IMP = Settings.STORE_CLIPBOARD_ON_DISK ? new DiskOptimizedClipboard(size.getBlockX(), size.getBlockY(), size.getBlockZ()) : new MemoryOptimizedClipboard(size.getBlockX(), size.getBlockY(), size.getBlockZ());
        this.origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
    }

    /**
     * Create a new instance.
     *
     * <p>The origin will be placed at the region's lowest minimum point.</p>
     *
     * @param region the bounding region
     */
    public BlockArrayClipboard(Region region, UUID clipboardId) {
        checkNotNull(region);
        this.region = region.clone();
        this.size = getDimensions();
        this.IMP = Settings.STORE_CLIPBOARD_ON_DISK ? new DiskOptimizedClipboard(size.getBlockX(), size.getBlockY(), size.getBlockZ(), clipboardId) : new MemoryOptimizedClipboard(size.getBlockX(), size.getBlockY(), size.getBlockZ());
        this.origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
    }

    public BlockArrayClipboard(Region region, FaweClipboard clipboard) {
        checkNotNull(region);
        this.region = region.clone();
        this.size = getDimensions();
        this.IMP = clipboard;
        this.origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
    }

    @Override
    public Region getRegion() {
        return region.clone();
    }

    @Override
    public Vector getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(Vector origin) {
        this.origin = origin;
        IMP.setOrigin(origin.subtract(region.getMinimumPoint()));
    }

    @Override
    public Vector getDimensions() {
        return region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
    }

    @Override
    public Vector getMinimumPoint() {
        return region.getMinimumPoint();
    }

    @Override
    public Vector getMaximumPoint() {
        return region.getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        List<Entity> filtered = new ArrayList<Entity>();
        for (Entity entity : getEntities()) {
            if (region.contains(entity.getLocation().toVector())) {
                filtered.add(entity);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return IMP.getEntities();
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return IMP.createEntity(location.getExtent(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), entity);
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        if (region.contains(position)) {
            int x = position.getBlockX() - mx;
            int y = position.getBlockY() - my;
            int z = position.getBlockZ() - mz;
            return IMP.getBlock(x, y, z);
        }
        return EditSession.nullBlock;
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return getBlock(position);
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        if (region.contains(location)) {
            final int x = location.getBlockX();
            final int y = location.getBlockY();
            final int z = location.getBlockZ();
            return setBlock(x, y, z, block);
        }
        return false;
    }

    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        x -= mx;
        y -= my;
        z -= mz;
        return IMP.setBlock(x, y, z, block);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return new BaseBiome(0);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    public static Class<?> inject() {
        return BlockArrayClipboard.class;
    }
}
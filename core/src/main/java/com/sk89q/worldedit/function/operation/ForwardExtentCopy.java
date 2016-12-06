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

package com.sk89q.worldedit.function.operation;

import com.boydti.fawe.object.extent.BlockTranslateExtent;
import com.boydti.fawe.object.extent.TransformExtent;
import com.boydti.fawe.object.function.block.SimpleBlockCopy;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.CombinedRegionFunction;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.entity.ExtentEntityCopy;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.WorldData;
import java.util.List;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Makes a copy of a portion of one extent to another extent or another point.
 *
 * <p>This is a forward extent copy, meaning that it iterates over the blocks
 * in the source extent, and will copy as many blocks as there are in the
 * source. Therefore, interpolation will not occur to fill in the gaps.</p>
 */
public class ForwardExtentCopy implements Operation {

    private final Extent source;
    private final Extent destination;
    private final Region region;
    private final Vector from;
    private final Vector to;
    private int repetitions = 1;
    private Mask sourceMask = Masks.alwaysTrue();
    private boolean removingEntities;
    private RegionFunction sourceFunction = null;
    private Transform transform = new Identity();
    private Transform currentTransform = null;
    private int affected;

    /**
     * Create a new copy using the region's lowest minimum point as the
     * "from" position.
     *
     * @param source the source extent
     * @param region the region to copy
     * @param destination the destination extent
     * @param to the destination position
     * @see #ForwardExtentCopy(Extent, Region, Vector, Extent, Vector) the main constructor
     */
    public ForwardExtentCopy(Extent source, Region region, Extent destination, Vector to) {
        this(source, region, region.getMinimumPoint(), destination, to);
    }

    /**
     * Create a new copy.
     *
     * @param source the source extent
     * @param region the region to copy
     * @param from the source position
     * @param destination the destination extent
     * @param to the destination position
     */
    public ForwardExtentCopy(Extent source, Region region, Vector from, Extent destination, Vector to) {
        checkNotNull(source);
        checkNotNull(region);
        checkNotNull(from);
        checkNotNull(destination);
        checkNotNull(to);
        this.source = source;
        this.destination = destination;
        this.region = region;
        this.from = from;
        this.to = to;
    }

    /**
     * Get the transformation that will occur on every point.
     *
     * <p>The transformation will stack with each repetition.</p>
     *
     * @return a transformation
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Set the transformation that will occur on every point.
     *
     * @param transform a transformation
     * @see #getTransform()
     */
    public void setTransform(Transform transform) {
        checkNotNull(transform);
        this.transform = transform;
    }

    /**
     * Get the mask that gets applied to the source extent.
     *
     * <p>This mask can be used to filter what will be copied from the source.</p>
     *
     * @return a source mask
     */
    public Mask getSourceMask() {
        return sourceMask;
    }

    /**
     * Set a mask that gets applied to the source extent.
     *
     * @param sourceMask a source mask
     * @see #getSourceMask()
     */
    public void setSourceMask(Mask sourceMask) {
        checkNotNull(sourceMask);
        this.sourceMask = sourceMask;
    }

    /**
     * Get the function that gets applied to all source blocks <em>after</em>
     * the copy has been made.
     *
     * @return a source function, or null if none is to be applied
     */
    public RegionFunction getSourceFunction() {
        return sourceFunction;
    }

    /**
     * Set the function that gets applied to all source blocks <em>after</em>
     * the copy has been made.
     *
     * @param function a source function, or null if none is to be applied
     */
    public void setSourceFunction(RegionFunction function) {
        this.sourceFunction = function;
    }

    /**
     * Get the number of repetitions left.
     *
     * @return the number of repetitions
     */
    public int getRepetitions() {
        return repetitions;
    }

    /**
     * Set the number of repetitions left.
     *
     * @param repetitions the number of repetitions
     */
    public void setRepetitions(int repetitions) {
        checkArgument(repetitions >= 0, "number of repetitions must be non-negative");
        this.repetitions = repetitions;
    }

    /**
     * Return whether entities that are copied should be removed.
     *
     * @return true if removing
     */
    public boolean isRemovingEntities() {
        return removingEntities;
    }

    /**
     * Set whether entities that are copied should be removed.
     *
     * @param removingEntities true if removing
     */
    public void setRemovingEntities(boolean removingEntities) {
        this.removingEntities = removingEntities;
    }

    /**
     * Get the number of affected objects.
     *
     * @return the number of affected
     */
    public int getAffected() {
        return affected;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        if (currentTransform == null) {
            currentTransform = transform;
        }

        Extent finalDest = destination;
        TransformExtent transExt;
        if (!currentTransform.isIdentity()) {
            WorldData wd;
            if (destination instanceof World) {
                wd = ((World) destination).getWorldData();
            } else if (source instanceof World) {
                wd = ((World) source).getWorldData();
            } else {
                wd = WorldEdit.getInstance().getServer().getWorlds().get(0).getWorldData();
            }
            BlockRegistry registry = wd.getBlockRegistry();
            transExt = new TransformExtent(finalDest, registry);
            transExt.setTransform(currentTransform);
            transExt.setOrigin(from);
            finalDest = transExt;
        } else {
            transExt = null;
        }
        Vector translation = to.subtract(from);
        if (!translation.equals(Vector.ZERO)) {
            finalDest = new BlockTranslateExtent(finalDest, translation.getBlockX(), translation.getBlockY(), translation.getBlockZ());
        }
        RegionFunction copy = new SimpleBlockCopy(source, finalDest);
        if (sourceMask != Masks.alwaysTrue()) {
            copy = new RegionMaskingFilter(sourceMask, copy);
        }
        if (sourceFunction != null) {
            copy = new CombinedRegionFunction(copy, sourceFunction);
        }
        RegionVisitor blockVisitor = new RegionVisitor(region, copy);

        List<? extends Entity> entities = source.getEntities(region);

        for (int i = 0; i < repetitions; i++) {
            ExtentEntityCopy entityCopy = new ExtentEntityCopy(from, destination, to, currentTransform);
            entityCopy.setRemoving(removingEntities);
            EntityVisitor entityVisitor = new EntityVisitor(entities.iterator(), entityCopy);

            Operations.completeBlindly(blockVisitor);
            Operations.completeBlindly(entityVisitor);

            if (transExt != null) {
                currentTransform = currentTransform.combine(transform);
                transExt.setTransform(currentTransform);
            }

            affected += blockVisitor.getAffected();
        }
        return null;
    }

    @Override
    public void cancel() {
    }

    @Override
    public void addStatusMessages(List<String> messages) {
    }

    public static Class<?> inject() {
        return ForwardExtentCopy.class;
    }
}

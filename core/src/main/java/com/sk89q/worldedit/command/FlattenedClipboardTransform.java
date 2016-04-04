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

package com.sk89q.worldedit.command;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.CombinedTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.registry.WorldData;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class to 'bake' a transform into a clipboard.
 *
 * <p>This class needs a better name and may need to be made more generic.</p>
 *
 * @see Clipboard
 * @see Transform
 */
public class FlattenedClipboardTransform {

    private final Clipboard original;
    private final Transform transform;
    private final WorldData worldData;

    /**
     * Create a new instance.
     *
     * @param original the original clipboard
     * @param transform the transform
     * @param worldData the world data instance
     */
    private FlattenedClipboardTransform(final Clipboard original, final Transform transform, final WorldData worldData) {
        checkNotNull(original);
        checkNotNull(transform);
        checkNotNull(worldData);
        this.original = original;
        this.transform = transform;
        this.worldData = worldData;
    }

    /**
     * Get the transformed region.
     *
     * @return the transformed region
     */
    public Region getTransformedRegion() {
        final Region region = this.original.getRegion();
        final Vector minimum = region.getMinimumPoint();
        final Vector maximum = region.getMaximumPoint();

        final Transform transformAround = new CombinedTransform(new AffineTransform().translate(this.original.getOrigin().multiply(-1)), this.transform, new AffineTransform().translate(this.original
        .getOrigin()));

        final Vector[] corners = new Vector[] {
        minimum,
        maximum,
        minimum.setX(maximum.getX()),
        minimum.setY(maximum.getY()),
        minimum.setZ(maximum.getZ()),
        maximum.setX(minimum.getX()),
        maximum.setY(minimum.getY()),
        maximum.setZ(minimum.getZ()) };

        for (int i = 0; i < corners.length; i++) {
            corners[i] = transformAround.apply(corners[i]);
        }

        Vector newMinimum = corners[0];
        Vector newMaximum = corners[0];

        for (int i = 1; i < corners.length; i++) {
            newMinimum = Vector.getMinimum(newMinimum, corners[i]);
            newMaximum = Vector.getMaximum(newMaximum, corners[i]);
        }

        // After transformation, the points may not really sit on a block,
        // so we should expand the region for edge cases
        newMinimum = newMinimum.setX(Math.floor(newMinimum.getX()));
        newMinimum = newMinimum.setY(Math.floor(newMinimum.getY()));
        newMinimum = newMinimum.setZ(Math.floor(newMinimum.getZ()));

        newMaximum = newMaximum.setX(Math.ceil(newMaximum.getX()));
        newMaximum = newMaximum.setY(Math.ceil(newMaximum.getY()));
        newMaximum = newMaximum.setZ(Math.ceil(newMaximum.getZ()));

        return new CuboidRegion(newMinimum, newMaximum);
    }

    /**
     * Create an operation to copy from the original clipboard to the given extent.
     *
     * @param target the target
     * @return the operation
     */
    public Operation copyTo(final Extent target) {
        final BlockTransformExtent extent = new BlockTransformExtent(this.original, this.transform, this.worldData.getBlockRegistry());
        final ForwardExtentCopy copy = new ForwardExtentCopy(extent, this.original.getRegion(), this.original.getOrigin(), target, this.original.getOrigin());
        copy.setTransform(this.transform);
        return copy;
    }

    /**
     * Create a new instance to bake the transform with.
     *
     * @param original the original clipboard
     * @param transform the transform
     * @param worldData the world data instance
     * @return a builder
     */
    public static FlattenedClipboardTransform transform(final Clipboard original, final Transform transform, final WorldData worldData) {
        return new FlattenedClipboardTransform(original, transform, worldData);
    }

}

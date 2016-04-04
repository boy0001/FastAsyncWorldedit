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

package com.sk89q.worldedit.function.visitor;

import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.regions.FlatRegion;
import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Applies region functions to columns in a {@link FlatRegion}.
 */
public class FlatRegionVisitor implements Operation {

    private final FlatRegionFunction function;
    private int affected = 0;
    private final Iterable<Vector2D> iterator;

    /**
     * Create a new visitor.
     *
     * @param flatRegion a flat region
     * @param function a function to apply to columns
     */
    public FlatRegionVisitor(final FlatRegion flatRegion, final FlatRegionFunction function) {
        checkNotNull(flatRegion);
        checkNotNull(function);
        this.function = function;
        this.iterator = flatRegion.asFlatRegion();
    }

    /**
     * Get the number of affected objects.
     *
     * @return the number of affected
     */
    public int getAffected() {
        return this.affected;
    }

    @Override
    public Operation resume(final RunContext run) throws WorldEditException {
        for (final Vector2D pt : this.iterator) {
            if (this.function.apply(pt)) {
                affected++;
            }
        }
        return null;
    }

    @Override
    public void cancel() {}

    @Override
    public void addStatusMessages(final List<String> messages) {
        messages.add(this.getAffected() + " columns affected");
    }

    public static Class<?> inject() {
        return Operations.class;
    }

}

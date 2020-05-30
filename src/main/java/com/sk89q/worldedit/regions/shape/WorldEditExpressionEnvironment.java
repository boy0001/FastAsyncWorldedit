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

package com.sk89q.worldedit.regions.shape;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.expression.runtime.ExpressionEnvironment;

public class WorldEditExpressionEnvironment implements ExpressionEnvironment {

    private final Vector unit;
    private final Vector zero2;
    private Vector current = new Vector();
    private Extent extent;

    public WorldEditExpressionEnvironment(EditSession editSession, Vector unit, Vector zero) {
        this((Extent) editSession, unit, zero);
    }

    public WorldEditExpressionEnvironment(Extent extent, Vector unit, Vector zero) {
        this.extent = extent;
        this.unit = unit;
        this.zero2 = zero.add(0.5, 0.5, 0.5);
    }

    @Override
    public BaseBlock getBlock(double x, double y, double z) {
        x = x * unit.getX() + zero2.getX();
        y = y * unit.getY() + zero2.getY();
        z = z * unit.getZ() + zero2.getZ();
        return extent.getLazyBlock((int) x, (int) y, (int) z);
    }

    @Override
    public BaseBlock getBlockAbs(double x, double y, double z) {
        return extent.getLazyBlock((int) x, (int) y, (int) z);
    }

    @Override
    public BaseBlock getBlockRel(double x, double y, double z) {
        x = x + current.getBlockX();
        y = y + current.getBlockY();
        z = z + current.getBlockZ();
        return extent.getLazyBlock((int) x, (int) y, (int) z);
    }

    public void setCurrentBlock(Vector current) {
        this.current = current;
    }

    public static Class<?> inject() {
        return WorldEditExpressionEnvironment.class;
    }
}

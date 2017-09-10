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

package com.sk89q.worldedit.patterns;

import com.sk89q.minecraft.util.commands.Link;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.extent.Extent;
import java.io.Serializable;

/**
 * @deprecated See {@link com.sk89q.worldedit.function.pattern.Pattern}
 */
@Deprecated
@Link(clazz = UtilityCommands.class, value = "patterns")
public interface Pattern extends Serializable {

    /**
     * Get a block for a position. This return value of this method does
     * not have to be consistent for the same position.
     *
     * @param position the position where a block is needed
     * @return a block
     */
    public BaseBlock next(Vector position);

    default boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        return extent.setBlock(setPosition, next(getPosition));
    }

    /**
     * Get a block for a position. This return value of this method does
     * not have to be consistent for the same position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return a block
     */
    public BaseBlock next(int x, int y, int z);

    public static Class<?> inject() {
        return Pattern.class;
    }
}

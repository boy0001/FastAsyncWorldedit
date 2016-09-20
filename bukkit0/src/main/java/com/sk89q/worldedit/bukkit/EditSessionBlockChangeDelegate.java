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

package com.sk89q.worldedit.bukkit;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BlockID;
import org.bukkit.BlockChangeDelegate;

/**
 * Proxy class to catch calls to set blocks.
 */
public class EditSessionBlockChangeDelegate implements BlockChangeDelegate {

    private EditSession editSession;

    public EditSessionBlockChangeDelegate(EditSession editSession) {
        this.editSession = editSession;
    }

    @Override
    public boolean setRawTypeId(int x, int y, int z, int typeId) {
        return editSession.setBlock(x, y, z, FaweCache.getBlock(typeId, 0));
    }

    @Override
    public boolean setRawTypeIdAndData(int x, int y, int z, int typeId, int data) {
        return editSession.setBlock(x, y, z, FaweCache.getBlock(typeId, data));
    }

    @Override
    public boolean setTypeId(int x, int y, int z, int typeId) {
        return setRawTypeId(x, y, z, typeId);
    }

    @Override
    public boolean setTypeIdAndData(int x, int y, int z, int typeId, int data) {
        return setRawTypeIdAndData(x, y, z, typeId, data);
    }

    @Override
    public int getTypeId(int x, int y, int z) {
        return editSession.getBlock(x, y, z).getId();
    }

    @Override
    public int getHeight() {
        return editSession.getMaxY() + 1;
    }

    @Override
    public boolean isEmpty(int x, int y, int z) {
        return editSession.getBlock(x, y, z).getId() == BlockID.AIR;
    }

    public static Class<?> inject() {
        return EditSessionBlockChangeDelegate.class;
    }

}

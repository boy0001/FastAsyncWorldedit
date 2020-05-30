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

package com.sk89q.worldedit.command.tool;

import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldVectorFace;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;

/**
 * A tool that can place (or remove) blocks at a distance.
 */
public class LongRangeBuildTool extends BrushTool implements DoubleActionTraceTool {

    BaseBlock primary;
    BaseBlock secondary;

    public LongRangeBuildTool(BaseBlock primary, BaseBlock secondary) {
        super("worldedit.tool.lrbuild");
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.tool.lrbuild");
    }

    @Override
    public boolean actSecondary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        WorldVectorFace pos = getTargetFace(player);
        if (pos == null) return false;
        EditSession eS = session.createEditSession(player);
        if (secondary.getType() == BlockID.AIR) {
            eS.setBlockFast(pos, secondary);
        } else {
            eS.setBlockFast(pos.getFaceVector(), secondary);
        }
        eS.flushQueue();
        return true;

    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        WorldVectorFace pos = getTargetFace(player);
        if (pos == null) return false;
        EditSession eS = session.createEditSession(player);
        if (primary.getType() == BlockID.AIR) {
            eS.setBlockFast(pos, primary);
        } else {
            eS.setBlockFast(pos.getFaceVector(), primary);
        }
        eS.flushQueue();
        return true;
    }

    public WorldVectorFace getTargetFace(Player player) {
        WorldVectorFace target = null;
        target = player.getBlockTraceFace(getRange(), true);

        if (target == null) {
            BBC.NO_BLOCK.send(player);
            return null;
        }

        return target;
    }

    public static Class<?> inject() {
        return LongRangeBuildTool.class;
    }

}
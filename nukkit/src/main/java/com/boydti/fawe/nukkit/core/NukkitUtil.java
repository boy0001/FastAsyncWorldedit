/*
 * LevelEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) LevelEdit team and contributors
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

package com.boydti.fawe.nukkit.core;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockWorldVector;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.util.Location;

public final class NukkitUtil {

    private NukkitUtil() {
    }

    public static LocalWorld getLocalWorld(Level w) {
        return new NukkitWorld(w);
    }

    public static BlockVector toVector(Block block) {
        return new BlockVector(block.getX(), block.getY(), block.getZ());
    }

    public static BlockWorldVector toWorldVector(Block block) {
        return new BlockWorldVector(getLocalWorld(block.getLevel()), block.getX(), block.getY(), block.getZ());
    }

    public static Vector toVector(cn.nukkit.level.Location loc) {
        return new Vector(loc.getX(), loc.getY(), loc.getZ());
    }

    public static Location toLocation(cn.nukkit.level.Location loc) {
        return new Location(
                getLocalWorld(loc.getLevel()),
                new Vector(loc.getX(), loc.getY(), loc.getZ()),
                (float) loc.getYaw(), (float) loc.getPitch()
        );
    }

    public static Vector toVector(Vector3 vector) {
        return new Vector(vector.getX(), vector.getY(), vector.getZ());
    }

    public static cn.nukkit.level.Location toLocation(WorldVector pt) {
        return new cn.nukkit.level.Location(pt.getX(), pt.getY(), pt.getZ(), 0, 0, toLevel(pt));
    }

    public static cn.nukkit.level.Location toLocation(Level world, Vector pt) {
        return new cn.nukkit.level.Location(pt.getX(), pt.getY(), pt.getZ(), 0, 0, world);
    }

    public static cn.nukkit.level.Location center(cn.nukkit.level.Location loc) {
        return new cn.nukkit.level.Location(
                loc.getFloorX() + 0.5,
                loc.getFloorY() + 0.5,
                loc.getFloorZ() + 0.5,
                loc.getPitch(),
                loc.getYaw(),
                loc.getLevel()
        );
    }

    public static Player matchSinglePlayer(Server server, String name) {
        Player[] players = server.matchPlayer(name);
        return players.length > 0 ? players[0] : null;
    }

    public static Block toBlock(BlockWorldVector pt) {
        return toLevel(pt).getBlock(toLocation(pt));
    }

    public static Level toLevel(WorldVector pt) {
        return ((NukkitWorld) pt.getWorld()).getLevel();
    }

    /**
     * Nukkit's Location class has serious problems with floating point
     * precision.
     */
    @SuppressWarnings("RedundantIfStatement")
    public static boolean equals(cn.nukkit.level.Location a, cn.nukkit.level.Location b) {
        if (Math.abs(a.getX() - b.getX()) > EQUALS_PRECISION) return false;
        if (Math.abs(a.getY() - b.getY()) > EQUALS_PRECISION) return false;
        if (Math.abs(a.getZ() - b.getZ()) > EQUALS_PRECISION) return false;
        return true;
    }

    public static final double EQUALS_PRECISION = 0.0001;

    public static cn.nukkit.level.Location toLocation(Location location) {
        return new cn.nukkit.level.Location(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), toLevel((LocalWorld) location.getExtent()));
    }

    public static Level toLevel(final LocalWorld world) {
        return ((NukkitWorld) world).getLevel();
    }

    public static NukkitEntity toLocalEntity(Entity e) {
        return new NukkitEntity(e);
    }

    public static com.sk89q.worldedit.entity.Entity createEntity(Level level, Location location, BaseEntity entity) {
        // TODO
        return null;
    }

    public static BaseBlock getBlock(Level level, Vector position) {
        Vector3 pos = new Vector3(position.getX(), position.getY(), position.getZ());
        Block block = level.getBlock(pos);
        int id = block.getId();
        int data = block.getDamage();
        return new BaseBlock(id, data);
    }

    public static boolean setBlock(Level level, Vector pos, BaseBlock block) {
        int x = pos.getBlockX();
        int y = pos.getBlockY();
        int z = pos.getBlockZ();
        level.setBlockIdAt(x, y, z, block.getId());
        level.setBlockDataAt(x, y, z, block.getData());
        return true;

    }
}
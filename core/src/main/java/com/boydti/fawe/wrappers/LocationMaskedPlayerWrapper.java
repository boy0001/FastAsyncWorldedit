package com.boydti.fawe.wrappers;

import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.internal.LocalWorldAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;

public class LocationMaskedPlayerWrapper extends PlayerWrapper {
    private final boolean allowTeleport;
    private Location position;

    public LocationMaskedPlayerWrapper(Player parent, Location position) {
        this(parent, position, false);
    }

    public LocationMaskedPlayerWrapper(Player parent, Location position, boolean allowTeleport) {
        super(parent);
        this.position = position;
        this.allowTeleport = allowTeleport;
    }

    public static Player unwrap(Player object) {
        if (object instanceof LocationMaskedPlayerWrapper) {
            return ((LocationMaskedPlayerWrapper) object).getParent();
        } else {
            return object;
        }
    }

    @Override
    public double getYaw() {
        return position.getYaw();
    }

    @Override
    public double getPitch() {
        return position.getPitch();
    }

    @Override
    public WorldVector getBlockIn() {
        WorldVector pos = getPosition();
        return WorldVector.toBlockPoint(pos.getWorld(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public WorldVector getBlockOn() {
        WorldVector pos = getPosition();
        return WorldVector.toBlockPoint(pos.getWorld(), pos.getX(), pos.getY() - 1, pos.getZ());
    }

    @Override
    public WorldVector getPosition() {
        LocalWorld world;
        if (position.getExtent() instanceof LocalWorld) {
            world = (LocalWorld) position.getExtent();
        } else {
            world = LocalWorldAdapter.adapt((World) position.getExtent());
        }
        return new WorldVector(world, position.toVector());
    }

    @Override
    public Location getLocation() {
        return position;
    }

    @Override
    public void setPosition(Vector pos, float pitch, float yaw) {
        this.position = new Location(position.getExtent(), pos, pitch, yaw);
        if (allowTeleport) {
            super.setPosition(pos, pitch, yaw);
        }
    }
}

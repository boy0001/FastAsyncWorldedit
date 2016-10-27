package com.boydti.fawe.wrappers;

import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;

public class LocationMaskedPlayerWrapper extends PlayerWrapper {
    private Location position;

    public LocationMaskedPlayerWrapper(Player parent, Location position) {
        super(parent);
        this.position = position;
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
        return new WorldVector((LocalWorld) position.getExtent(), position.toVector());
    }

    @Override
    public Location getLocation() {
        return position;
    }

    @Override
    public void setPosition(Vector pos, float pitch, float yaw) {
        this.position = new Location(position.getExtent(), pos, pitch, yaw);
    }
}

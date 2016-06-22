package com.boydti.fawe.wrappers;

import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;

public class LocationMaskedPlayerWrapper extends PlayerWrapper {
    private Vector position;

    public LocationMaskedPlayerWrapper(Player parent, Vector position) {
        super(parent);
        this.position = position;
    }

    @Override
    public WorldVector getBlockIn() {
        return new WorldVector((LocalWorld) getWorld(), position);
    }

    @Override
    public WorldVector getBlockOn() {
        return new WorldVector((LocalWorld) getWorld(), position.subtract(0, 1, 0));
    }

    @Override
    public WorldVector getPosition() {
        return new WorldVector((LocalWorld) getWorld(), position);
    }

    @Override
    public Location getLocation() {
        return new Location(getWorld(), position);
    }

    @Override
    public void setPosition(Vector pos, float pitch, float yaw) {
        this.position = pos;
    }
}

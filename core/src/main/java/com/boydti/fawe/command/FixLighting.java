package com.boydti.fawe.command;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

public class FixLighting extends FaweCommand {

    public FixLighting() {
        super("fawe.fixlighting");
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        if (player == null) {
            return false;
        }
        final FaweLocation loc = player.getLocation();
        final int cx = loc.x >> 4;
        final int cz = loc.z >> 4;

        Region selection = player.getSelection();
        if (selection == null) {
            selection = new CuboidRegion(new Vector(cx - 8, 0, cz - 8).multiply(16), new Vector(cx + 8, 0, cz + 8).multiply(16));
        }
        int count = FaweAPI.fixLighting(loc.world, selection);
        BBC.FIX_LIGHTING_SELECTION.send(player, count);
        return true;
    }
}

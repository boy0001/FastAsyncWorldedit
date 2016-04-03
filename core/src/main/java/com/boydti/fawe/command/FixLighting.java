package com.boydti.fawe.command;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.Vector;
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
        final Region selection = player.getSelection();
        if (selection == null) {
            FaweAPI.fixLighting(loc.world, loc.x >> 4, loc.z >> 4, Settings.FIX_ALL_LIGHTING);
            BBC.FIX_LIGHTING_CHUNK.send(player);
            return true;
        }
        final int cx = loc.x >> 4;
        final int cz = loc.z >> 4;
        final Vector bot = selection.getMinimumPoint();
        final Vector top = selection.getMaximumPoint();

        final int minX = Math.max(cx - 8, bot.getBlockX() >> 4);
        final int minZ = Math.max(cz - 8, bot.getBlockZ() >> 4);

        final int maxX = Math.min(cx + 8, top.getBlockX() >> 4);
        final int maxZ = Math.min(cz + 8, top.getBlockZ() >> 4);

        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                FaweAPI.fixLighting(loc.world, x, z, Settings.FIX_ALL_LIGHTING);
                count++;
            }
        }
        BBC.FIX_LIGHTING_SELECTION.send(player, count);

        return true;
    }
}

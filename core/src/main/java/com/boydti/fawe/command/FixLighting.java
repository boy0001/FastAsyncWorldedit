package com.boydti.fawe.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.SetQueue;
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
        final Vector bot = selection.getMinimumPoint();
        final Vector top = selection.getMaximumPoint();

        final int minX = bot.getBlockX() >> 4;
        final int minZ = bot.getBlockZ() >> 4;

        final int maxX = top.getBlockX() >> 4;
        final int maxZ = top.getBlockZ() >> 4;

        int count = 0;
        FaweQueue queue = SetQueue.IMP.getNewQueue(loc.world, true, false);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                queue.sendChunk(queue.getChunk(x, z), FaweQueue.RelightMode.ALL);
                count++;
            }
        }
        BBC.FIX_LIGHTING_SELECTION.send(player, count);

        return true;
    }
}

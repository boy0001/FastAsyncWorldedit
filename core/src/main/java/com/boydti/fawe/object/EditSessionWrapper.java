package com.boydti.fawe.object;

import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.util.FaweQueue;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.extent.Extent;

public class EditSessionWrapper {

    public final EditSession session;

    public EditSessionWrapper(final EditSession session) {
        this.session = session;
    }

    public int getHighestTerrainBlock(final int x, final int z, final int minY, final int maxY, final boolean naturalOnly) {
        for (int y = maxY; y >= minY; --y) {
            final Vector pt = new Vector(x, y, z);
            final int id = this.session.getBlockType(pt);
            final int data = this.session.getBlockData(pt);
            if (naturalOnly ? BlockType.isNaturalTerrainBlock(id, data) : !BlockType.canPassThrough(id, data)) {
                return y;
            }
        }
        return minY;
    }

    public Extent getHistoryExtent(String world, FaweLimit limit, Extent parent, FaweChangeSet set, FaweQueue queue, FawePlayer<?> player) {
        return new HistoryExtent(world, limit, parent, set, queue);
    }
}

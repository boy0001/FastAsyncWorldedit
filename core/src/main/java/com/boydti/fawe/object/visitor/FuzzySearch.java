package com.boydti.fawe.object.visitor;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.collection.SparseBitSet;
import com.boydti.fawe.object.regions.FuzzyRegion;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.regions.RegionOperationException;
import java.util.List;

public class FuzzySearch implements Operation {

    private final FuzzyRegion region;
    private final SparseBitSet visited;
    private final SparseBitSet queue;
    private final int offsetZ;
    private final int offsetX;
    private final Extent extent;
    private final int maxY;
    private int affected;

    public FuzzySearch(FuzzyRegion region, Extent extent, Vector origin) {
        this.region = region;
        this.queue = new SparseBitSet();
        this.visited = new SparseBitSet();
        this.offsetX = origin.getBlockX();
        this.offsetZ = origin.getBlockZ();
        this.queue.set(MathMan.tripleSearchCoords(0, origin.getBlockY(), 0));
        this.extent = extent;
        this.maxY = extent.getMaximumPoint().getBlockY();
    }

    private boolean hasVisited(int x, int y, int z) {
        return visited.get(MathMan.tripleSearchCoords(x - offsetX, y, z - offsetZ));
    }

    private void queueVisit(int x, int y, int z) throws RegionOperationException {
        if (y < 0 || y > maxY) {
            return;
        }
        int ox = x - offsetX;
        if (ox >= 1024 || ox < -1024) {
            throw new RegionOperationException("Selection is too large! (1024 blocks wide)");
        }
        int oz = z - offsetZ;
        if (oz >= 1024 || oz < -1024) {
            throw new RegionOperationException("Selection is too large! (1024 blocks wide)");
        }
        int index = MathMan.tripleSearchCoords(ox, y, oz);
        if (!visited.get(index)) {
            visited.set(index);
            queue.set(index);
        }
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        Vector pos = new Vector();
        Mask mask = region.getMask();
        int index = 0;
        while ((index = queue.nextSetBit(index)) != -1 || (index = queue.nextSetBit(0)) != -1) {
            queue.clear(index);
            int b1 = (index & 0xFF);
            int b2 = ((byte) (index >> 8)) & 0x7F;
            int b3 = ((byte)(index >> 15)) & 0xFF;
            int b4 = ((byte) (index >> 23)) & 0xFF;
            int x = offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21);
            int y = b1;
            int z = offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21);
            pos.mutX(x);
            pos.mutY(y);
            pos.mutZ(z);
            if (mask.test(pos)) {
                affected++;
                region.set(x, y, z);
                queueVisit(x + 1, y, z);
                queueVisit(x - 1, y, z);
                queueVisit(x, y + 1, z);
                queueVisit(x, y - 1, z);
                queueVisit(x, y, z + 1);
                queueVisit(x, y, z - 1);
            }
        }
        return null;
    }

    @Override
    public void cancel() {

    }

    public int getAffected() {
        return affected;
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }

}
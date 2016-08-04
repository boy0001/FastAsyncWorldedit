package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;

public class MutableFullBlockChange implements Change {

    public int z;
    public int y;
    public int x;
    public int from;
    public int to;


    public MutableFullBlockChange(int x, int y, int z, int combinedFrom, int combinedTo) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.from = combinedFrom;
        this.to = combinedTo;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        create(context);
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        create(context);
    }

    public void create(UndoContext context) {
        Extent extent = context.getExtent();
        ExtentTraverser<FastWorldEditExtent> find = new ExtentTraverser(extent).find(FastWorldEditExtent.class);
        if (find != null) {
            FastWorldEditExtent fwee = find.get();
            fwee.getQueue().setBlock(x, y, z, FaweCache.getId(from), FaweCache.getData(from));
        } else {
            Fawe.debug("FAWE doesn't support: " + extent + " for " + getClass() + " (bug Empire92)");
        }
    }
}

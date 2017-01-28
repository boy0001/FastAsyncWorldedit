package com.boydti.fawe.object.change;

import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class MutableBiomeChange implements Change {

    private Vector2D pos;
    private BaseBiome from;
    private BaseBiome to;
    public MutableBiomeChange() {
        this.from = new BaseBiome(0);
        this.to = new BaseBiome(0);
        this.pos = new Vector2D();
    }

    public void setBiome(int x, int z, int from, int to) {
        this.pos.x = x;
        this.pos.z = z;
        this.from.setId(from);
        this.to.setId(to);
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        context.getExtent().setBiome(pos, from);
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        context.getExtent().setBiome(pos, to);
    }
}

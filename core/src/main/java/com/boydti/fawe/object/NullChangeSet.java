package com.boydti.fawe.object;

import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.World;
import java.util.ArrayList;
import java.util.Iterator;

public class NullChangeSet extends FaweChangeSet {
    public NullChangeSet(World world) {
        super(world);
    }

    @Override
    public boolean flush() {
        return false;
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {

    }

    @Override
    public void addTileCreate(CompoundTag tag) {

    }

    @Override
    public void addTileRemove(CompoundTag tag) {

    }

    @Override
    public void addEntityRemove(CompoundTag tag) {

    }

    @Override
    public void addEntityCreate(CompoundTag tag) {

    }

    @Override
    public Iterator<Change> getIterator(boolean undo) {
        return new ArrayList<Change>().iterator();
    }

    @Override
    public int size() {
        return 0;
    }
}

package com.boydti.fawe.object;

import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.history.change.Change;
import java.util.ArrayList;
import java.util.Iterator;

public class NullChangeSet implements FaweChangeSet {
    
    @Override
    public void add(Change change) {}
    
    @Override
    public Iterator<Change> backwardIterator() {
        return new ArrayList<Change>().iterator();
    }
    
    @Override
    public Iterator<Change> forwardIterator() {
        return new ArrayList<Change>().iterator();
    }
    
    @Override
    public int size() {
        return 0;
    }

    @Override
    public void flush() {}

    @Override
    public void add(Vector location, BaseBlock from, BaseBlock to) {}

    @Override
    public void add(int x, int y, int z, int combinedId4DataFrom, BaseBlock to) {}

    @Override
    public void add(int x, int y, int z, int combinedId4DataFrom, int combinedId4DataTo) {}
}

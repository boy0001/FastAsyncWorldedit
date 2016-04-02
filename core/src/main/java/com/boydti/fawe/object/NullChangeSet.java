package com.boydti.fawe.object;

import java.util.ArrayList;
import java.util.Iterator;

import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;

public class NullChangeSet implements ChangeSet {
    
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
    
}

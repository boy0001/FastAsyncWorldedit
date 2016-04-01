package com.boydti.fawe.object.changeset;

import java.util.Iterator;

import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;

/**
 * ChangeSet optimized for low memory usage
 *  - No disk usage
 *  - High CPU usage
 *  - Low memory usage
 */
public class MemoryOptimizedHistory implements ChangeSet {
    
    @Override
    public void add(Change paramChange) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Iterator<Change> backwardIterator() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Iterator<Change> forwardIterator() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }
    
}

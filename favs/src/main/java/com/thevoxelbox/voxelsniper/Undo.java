package com.thevoxelbox.voxelsniper;

import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Holds {@link org.bukkit.block.BlockState}s that can be later on used to reset those block
 * locations back to the recorded states.
 */
public class Undo {

    int size;
    private World world;

    /**
     * Default constructor of a Undo container.
     */
    public Undo() {}

    /**
     * Get the number of blocks in the collection.
     *
     * @return size of the Undo collection
     */
    public int getSize() {
        return size;
    }

    /**
     * Adds a Block to the collection.
     *
     * @param block Block to be added
     */
    public void put(Block block) {
        size++;
    }


    /**
     * Set the blockstates of all recorded blocks back to the state when they
     * were inserted.
     */
    public void undo() {

    }
}
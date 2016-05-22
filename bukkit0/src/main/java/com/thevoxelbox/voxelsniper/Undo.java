package com.thevoxelbox.voxelsniper;

import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

/**
 * Holds {@link BlockState}s that can be later on used to reset those block
 * locations back to the recorded states.
 */
public class Undo {

    private final Set<Vector> containing = Sets.newHashSet();
    private final ArrayDeque<BlockState> all;

    private World world;

    /**
     * Default constructor of a Undo container.
     */
    public Undo() {
        all = new ArrayDeque<BlockState>();
    }

    /**
     * Get the number of blocks in the collection.
     *
     * @return size of the Undo collection
     */
    public int getSize() {
        return containing.size();
    }

    /**
     * Adds a Block to the collection.
     *
     * @param block Block to be added
     */
    public void put(Block block) {
        if (world == null) {
            world = block.getWorld();
        }
        Vector pos = block.getLocation().toVector();
        if (this.containing.contains(pos)) {
            return;
        }
        this.containing.add(pos);
        all.add(block.getState());
    }

    /**
     * Set the blockstates of all recorded blocks back to the state when they
     * were inserted.
     */
    public void undo() {
        for (BlockState blockState : all) {
            blockState.update(true, false);
        }
        if (world instanceof AsyncWorld) {
            ((AsyncWorld) world).commit();
        }
    }
}
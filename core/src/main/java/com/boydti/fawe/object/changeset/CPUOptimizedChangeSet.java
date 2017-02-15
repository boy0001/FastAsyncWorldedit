package com.boydti.fawe.object.changeset;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.change.MutableChunkChange;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import java.util.Iterator;

public class CPUOptimizedChangeSet extends FaweChangeSet {

    public CPUOptimizedChangeSet(World world) {
        super(world);
    }

    private ArrayList<Change> changes = new ArrayList<>();

    public void addChangeTask(FaweQueue queue) {
        queue.setChangeTask(new RunnableVal2<FaweChunk, FaweChunk>() {
            @Override
            public void run(final FaweChunk previous, final FaweChunk next) {
                char[][] previousIds = previous.getCombinedIdArrays();
                char[][] nextIds = next.getCombinedIdArrays();
                for (int i = 0; i < nextIds.length; i++) {
                    char[] nextArray = nextIds[i];
                    if (nextArray != null) {
                        char[] previousArray = previousIds[i];
                        if (previousArray == null) {
                            previous.fillCuboid(0, 15, i << 4, (i << 4) + 15, 0, 15, 0, (byte) 0);
                            continue;
                        }
                        for (int k = 0; k < nextArray.length; k++) {
                            int combinedNext = nextArray[k];
                            if (combinedNext > 0) {
                                int combinedPrevious = previousArray[k];
                                if (combinedPrevious == 0) {
                                    previousArray[k] = 1;
                                }
                            } else {
                                previousArray[k] = 0;
                            }
                        }
                    }
                }
                changes.add(new MutableChunkChange(previous, next));
            }
        });
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        throw new UnsupportedOperationException("Invalid mode");
    }

    @Override
    public void addBiomeChange(int x, int z, BaseBiome from, BaseBiome to) {
        throw new UnsupportedOperationException("Invalid mode");
    }

    @Override
    public void addTileCreate(CompoundTag tag) {
        throw new UnsupportedOperationException("Invalid mode");
    }

    @Override
    public void addTileRemove(CompoundTag tag) {
        throw new UnsupportedOperationException("Invalid mode");
    }

    @Override
    public void addEntityRemove(CompoundTag tag) {
        throw new UnsupportedOperationException("Invalid mode");
    }

    @Override
    public void addEntityCreate(CompoundTag tag) {
        throw new UnsupportedOperationException("Invalid mode");
    }

    @Override
    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return getIterator(redo);
    }

    @Override
    public Iterator<Change> getIterator(boolean redo) {
        return changes.iterator();
    }

    @Override
    public boolean isEmpty() {
        if (changes.isEmpty()) {
            close();
            return changes.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    public int size() {
        return changes.size() * 65536; // num chunks * 65536 (guess of 65536 changes per chunk)
    }
}

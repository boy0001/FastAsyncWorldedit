package com.boydti.fawe.object.changeset;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.UnplaceableBlockException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class BlockBagChangeSet extends AbstractDelegateChangeSet {

    private final boolean mine;
    private int[] missingBlocks = new int[Character.MAX_VALUE + 1];
    private BlockBag blockBag;

    public BlockBagChangeSet(FaweChangeSet parent, BlockBag blockBag, boolean mine) {
        super(parent);
        this.blockBag = blockBag;
        this.mine = mine;
    }

    /**
     * Get the block bag.
     *
     * @return a block bag, which may be null if none is used
     */
    public
    @Nullable
    BlockBag getBlockBag() {
        return blockBag;
    }

    /**
     * Set the block bag.
     *
     * @param blockBag a block bag, which may be null if none is used
     */
    public void setBlockBag(@Nullable BlockBag blockBag) {
        this.blockBag = blockBag;
    }


    /**
     * Gets the list of missing blocks and clears the list for the next
     * operation.
     *
     * @return a map of missing blocks
     */
    public Map<Integer, Integer> popMissing() {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < missingBlocks.length; i++) {
            int count = missingBlocks[i];
            if (count > 0) {
                map.put(i, count);
            }
        }
        Arrays.fill(missingBlocks, 0);
        return map;
    }

    @Override
    public void add(Vector loc, BaseBlock from, BaseBlock to) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        add(x, y, z, from, to);
    }

    @Override
    public void add(int x, int y, int z, BaseBlock from, BaseBlock to) {
        check(from.getCombined(), to.getCombined());
        super.add(x, y, z, from, to);
    }

    public void check(int combinedFrom, int combinedTo) {
        if (combinedTo != 0) {
            try {
                blockBag.fetchPlacedBlock(FaweCache.getId(combinedTo), FaweCache.getData(combinedTo));
            } catch (UnplaceableBlockException e) {
                throw new FaweException.FaweBlockBagException();
            } catch (BlockBagException e) {
                missingBlocks[combinedTo]++;
                throw new FaweException.FaweBlockBagException();
            }
        }
        if (mine) {
            if (combinedFrom != 0) {
                try {
                    blockBag.storeDroppedBlock(FaweCache.getId(combinedFrom), FaweCache.getData(combinedFrom));
                } catch (BlockBagException ignored) {
                }
            }
        }
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        check(combinedFrom, combinedTo);
        super.add(x, y, z, combinedFrom, combinedTo);
    }

    @Override
    public void addTileCreate(CompoundTag nbt) {
        if (nbt.containsKey("items")) {
            Map<String, Tag> map = ReflectionUtils.getMap(nbt.getValue());
            map.remove("items");
        }
        super.addTileCreate(nbt);
    }
}

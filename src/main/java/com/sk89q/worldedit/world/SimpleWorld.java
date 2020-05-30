package com.sk89q.worldedit.world;

import com.boydti.fawe.util.SetQueue;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.TreeGenerator;
import javax.annotation.Nullable;

public interface SimpleWorld extends World {
    @Override
    default boolean useItem(Vector position, BaseItem item, Direction face) {
        return false;
    }

    @Override
    default boolean setBlockType(Vector position, int type) {
        try {
            return setBlock(position, new BaseBlock(type));
        } catch (WorldEditException ignored) {
            return false;
        }
    }

    @Override
    default void setBlockData(Vector position, int data) {
        try {
            setBlock(position, new BaseBlock(getLazyBlock(position).getType(), data));
        } catch (WorldEditException ignored) {
        }
    }

    @Override
    default boolean setTypeIdAndData(Vector position, int type, int data) {
        try {
            return setBlock(position, new BaseBlock(type, data));
        } catch (WorldEditException ignored) {
            return false;
        }
    }

    @Override
    default boolean setBlock(Vector pt, BaseBlock block) throws WorldEditException {
        return setBlock(pt, block, true);
    }

    @Override
    default int getMaxY() {
        return getMaximumPoint().getBlockY();
    }

    @Override
    default boolean isValidBlockType(int type) {
        return BlockType.fromID(type) != null;
    }

    @Override
    default boolean usesBlockData(int type) {
        // We future proof here by assuming all unknown blocks use data
        return BlockType.usesData(type) || BlockType.fromID(type) == null;
    }

    @Override
    default Mask createLiquidMask() {
        return new BlockMask(this,
                new BaseBlock(BlockID.STATIONARY_LAVA, -1),
                new BaseBlock(BlockID.LAVA, -1),
                new BaseBlock(BlockID.STATIONARY_WATER, -1),
                new BaseBlock(BlockID.WATER, -1));
    }

    @Override
    default int getBlockType(Vector pt) {
        return getLazyBlock(pt).getType();
    }

    @Override
    default int getBlockData(Vector pt) {
        return getLazyBlock(pt).getData();
    }

    @Override
    default void dropItem(Vector pt, BaseItemStack item, int times) {
        for (int i = 0; i < times; ++i) {
            dropItem(pt, item);
        }
    }

    @Override
    default void simulateBlockMine(Vector pt) {
        BaseBlock block = getLazyBlock(pt);
        BaseItemStack stack = BlockType.getBlockDrop(block.getId(), (short) block.getData());

        if (stack != null) {
            final int amount = stack.getAmount();
            if (amount > 1) {
                dropItem(pt, new BaseItemStack(stack.getType(), 1, stack.getData()), amount);
            } else {
                dropItem(pt, stack, amount);
            }
        }

        try {
            setBlock(pt, new BaseBlock(BlockID.AIR));
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    default boolean generateTree(EditSession editSession, Vector pt) throws MaxChangedBlocksException {
        return generateTree(TreeGenerator.TreeType.TREE, editSession, pt);
    }

    @Override
    default boolean generateBigTree(EditSession editSession, Vector pt) throws MaxChangedBlocksException {
        return generateTree(TreeGenerator.TreeType.BIG_TREE, editSession, pt);
    }

    @Override
    default boolean generateBirchTree(EditSession editSession, Vector pt) throws MaxChangedBlocksException {
        return generateTree(TreeGenerator.TreeType.BIRCH, editSession, pt);
    }

    @Override
    default boolean generateRedwoodTree(EditSession editSession, Vector pt) throws MaxChangedBlocksException {
        return generateTree(TreeGenerator.TreeType.REDWOOD, editSession, pt);
    }

    @Override
    default boolean generateTallRedwoodTree(EditSession editSession, Vector pt) throws MaxChangedBlocksException {
        return generateTree(TreeGenerator.TreeType.TALL_REDWOOD, editSession, pt);
    }

    @Override
    default void checkLoadedChunk(Vector pt) {
    }

    @Override
    default void fixAfterFastMode(Iterable<BlockVector2D> chunks) {
    }

    @Override
    default void fixLighting(Iterable<BlockVector2D> chunks) {
    }

    @Override
    default boolean playEffect(Vector position, int type, int data) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    default boolean queueBlockBreakEffect(Platform server, Vector position, int blockId, double priority) {
        SetQueue.IMP.addTask(() -> playEffect(position, 2001, blockId));
        return true;
    }

    @Override
    default Vector getMinimumPoint() {
        return new Vector(-30000000, 0, -30000000);
    }

    @Override
    default Vector getMaximumPoint() {
        return new Vector(30000000, 255, 30000000);
    }

    @Override
    default @Nullable
    Operation commit() {
        return null;
    }
}

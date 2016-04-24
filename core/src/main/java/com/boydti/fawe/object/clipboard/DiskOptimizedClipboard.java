package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.IntegerTrio;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * // TODO
 * A clipboard with disk backed storage. (lower memory + loads on crash)
 *  - Uses an auto closable RandomAccessFile for getting / setting id / data
 *  - I don't know how to reduce nbt / entities to O(1) complexity, so it is stored in memory.
 */
public class DiskOptimizedClipboard extends FaweClipboard {

    private final HashMap<IntegerTrio, CompoundTag> nbtMap;
    private final HashSet<ClipboardEntity> entities;

    public DiskOptimizedClipboard(int width, int height, int length) {
        super(width, height, length);
        nbtMap = new HashMap<>();
        entities = new HashSet<ClipboardEntity>();
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED / WIP");
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED / WIP");
    }

    @Override
    public Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED / WIP");
    }

    @Override
    public List<? extends Entity> getEntities() {
        throw new UnsupportedOperationException("NOT IMPLEMENTED / WIP");
    }

    @Override
    public boolean remove(ClipboardEntity clipboardEntity) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED / WIP");
    }
}

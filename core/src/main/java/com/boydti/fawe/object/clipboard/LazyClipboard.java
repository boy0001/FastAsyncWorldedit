package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import java.util.List;

public abstract class LazyClipboard extends FaweClipboard {

//    private final int width, height, length;
//
//    public LazyClipboard(int width, int height, int length) {
//        this.width = width;
//        this.height = height;
//        this.length = length;
//    }
//
//    @Override
//    public void forEach(RunnableVal2<Vector, BaseBlock> task, boolean air) {
//        BlockVector pos = new BlockVector(0, 0, 0);
//        for (pos.x = 0; pos.x < width; pos.x++) {
//            for (pos.z = 0; pos.z < width; pos.z++) {
//                for (pos.y = 0; pos.y < width; pos.y++) {
//                    task.run(pos, getBlock((int) pos.x, (int) pos.y, (int) pos.z));
//                }
//            }
//        }
//    }

    @Override
    public abstract BaseBlock getBlock(int x, int y, int z);

    @Override
    public abstract List<? extends Entity> getEntities();

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public boolean remove(ClipboardEntity clipboardEntity) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }
}

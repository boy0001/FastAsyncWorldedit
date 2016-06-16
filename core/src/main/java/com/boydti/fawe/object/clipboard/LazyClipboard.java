package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.RunnableVal2;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import java.util.Iterator;
import java.util.List;

public abstract class LazyClipboard extends FaweClipboard {
    private final Region region;

    public LazyClipboard(Region region) {
        this.region = region;
    }

    public static LazyClipboard of(final EditSession editSession, Region region) {
        final Vector origin = region.getMinimumPoint();
        final int mx = origin.getBlockX();
        final int my = origin.getBlockY();
        final int mz = origin.getBlockZ();
        return new LazyClipboard(region) {
            @Override
            public BaseBlock getBlock(int x, int y, int z) {
                return editSession.getLazyBlock(mx + x, my + y, mz + z);
            }

            public BaseBlock getBlockAbs(int x, int y, int z) {
                return editSession.getLazyBlock(x, y, z);
            }

            @Override
            public List<? extends Entity> getEntities() {
                return editSession.getEntities(getRegion());
            }

            @Override
            public void forEach(RunnableVal2<Vector, BaseBlock> task, boolean air) {
                Iterator<BlockVector> iter = getRegion().iterator();
                while (iter.hasNext()) {
                    BlockVector pos = iter.next();
                    BaseBlock block = getBlockAbs((int) pos.x, (int) pos.y, (int) pos.z);
                    if (!air && block == EditSession.nullBlock) {
                        continue;
                    }
                    pos.x -= mx;
                    pos.y -= my;
                    pos.z -= mz;
                    task.run(pos, block);
                }
            }
        };
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public abstract BaseBlock getBlock(int x, int y, int z);

    @Override
    public abstract List<? extends Entity> getEntities();

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
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

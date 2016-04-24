package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import java.util.List;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public abstract class FaweClipboard {
    public final int length;
    public final int height;
    public final int width;
    public final int area;

    public FaweClipboard(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.area = width * length;
    }

    public abstract BaseBlock getBlock(int x, int y, int z);

    public abstract boolean setBlock(int x, int y, int z, BaseBlock block);

    public abstract Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity);

    public abstract List<? extends Entity> getEntities();

    public abstract boolean remove(ClipboardEntity clipboardEntity);

    /**
     * Stores entity data.
     */
    public class ClipboardEntity implements Entity {
        private final BaseEntity entity;
        private final Extent world;
        private final double x,y,z;
        private final float yaw,pitch;

        public ClipboardEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
            checkNotNull(entity);
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.entity = new BaseEntity(entity);
        }

        @Override
        public boolean remove() {
            return FaweClipboard.this.remove(this);
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return null;
        }

        /**
         * Get the entity state. This is not a copy.
         *
         * @return the entity
         */
        BaseEntity getEntity() {
            return entity;
        }

        @Override
        public BaseEntity getState() {
            return new BaseEntity(entity);
        }

        @Override
        public Location getLocation() {
            return new Location(world, x, y, z, yaw, pitch);
        }

        @Override
        public Extent getExtent() {
            return world;
        }
    }
}

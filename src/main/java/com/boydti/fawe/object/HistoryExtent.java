package com.boydti.fawe.object;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;

/**
 * Stores changes to a {@link ChangeSet}.
 */
public class HistoryExtent extends AbstractDelegateExtent {

    private final ChangeSet changeSet;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     * @param changeSet the change set
     * @param thread
     */
    public HistoryExtent(final Extent extent, final ChangeSet changeSet) {
        super(extent);
        checkNotNull(changeSet);
        this.changeSet = changeSet;
    }

    @Override
    public synchronized boolean setBlock(final Vector location, final BaseBlock block) throws WorldEditException {
        if (super.setBlock(location, block)) {
            BaseBlock previous;
            try {
                previous = this.getBlock(location);
            } catch (final Exception e) {
                previous = this.getBlock(location);
            }
            final int id_p = previous.getId();
            final int id_b = block.getId();
            if (id_p == id_b) {
                switch (id_p) {
                    case 0:
                    case 2:
                    case 4:
                    case 13:
                    case 14:
                    case 15:
                    case 20:
                    case 21:
                    case 22:
                    case 25:
                    case 30:
                    case 32:
                    case 37:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 51:
                    case 52:
                    case 54:
                    case 56:
                    case 57:
                    case 58:
                    case 60:
                    case 61:
                    case 62:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 73:
                    case 74:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 87:
                    case 88:
                    case 101:
                    case 102:
                    case 103:
                    case 110:
                    case 112:
                    case 113:
                    case 117:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 129:
                    case 133:
                    case 138:
                    case 137:
                    case 140:
                    case 165:
                    case 166:
                    case 169:
                    case 170:
                    case 172:
                    case 173:
                    case 174:
                    case 176:
                    case 177:
                    case 181:
                    case 182:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                    case 192:
                        return false;
                    default:
                        if (block.getData() == previous.getData()) {
                            return false;
                        }
                }
            }
            this.changeSet.add(new BlockChange(location.toBlockVector(), previous, block));
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public Entity createEntity(final Location location, final BaseEntity state) {
        final Entity entity = super.createEntity(location, state);
        if ((state != null) && (entity != null)) {
            this.changeSet.add(new EntityCreate(location, state, entity));
        }
        return entity;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return this.wrapEntities(super.getEntities());
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return this.wrapEntities(super.getEntities(region));
    }

    private List<? extends Entity> wrapEntities(final List<? extends Entity> entities) {
        final List<Entity> newList = new ArrayList<Entity>(entities.size());
        for (final Entity entity : entities) {
            newList.add(new TrackedEntity(entity));
        }
        return newList;
    }

    private class TrackedEntity implements Entity {
        private final Entity entity;

        private TrackedEntity(final Entity entity) {
            this.entity = entity;
        }

        @Override
        public BaseEntity getState() {
            return this.entity.getState();
        }

        @Override
        public Location getLocation() {
            return this.entity.getLocation();
        }

        @Override
        public Extent getExtent() {
            return this.entity.getExtent();
        }

        @Override
        public boolean remove() {
            final Location location = this.entity.getLocation();
            final BaseEntity state = this.entity.getState();
            final boolean success = this.entity.remove();
            if ((state != null) && success) {
                HistoryExtent.this.changeSet.add(new EntityRemove(location, state));
            }
            return success;
        }

        @Nullable
        @Override
        public <T> T getFacet(final Class<? extends T> cls) {
            return this.entity.getFacet(cls);
        }
    }
}

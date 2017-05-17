/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.boydti.fawe.nukkit.core;

import cn.nukkit.entity.Entity;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.entity.metadata.EntityType;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.NullWorld;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An adapter to adapt a Bukkit entity into a WorldEdit one.
 */
class NukkitEntity implements com.sk89q.worldedit.entity.Entity {

    private final WeakReference<cn.nukkit.entity.Entity> entityRef;

    /**
     * Create a new instance.
     *
     * @param entity the entity
     */
    NukkitEntity(cn.nukkit.entity.Entity entity) {
        checkNotNull(entity);
        this.entityRef = new WeakReference<Entity>(entity);
    }

    @Override
    public Extent getExtent() {
        Entity entity = entityRef.get();
        if (entity != null) {
            return new NukkitWorld(entity.getLevel());
        } else {
            return NullWorld.getInstance();
        }
    }

    @Override
    public Location getLocation() {
        Entity entity = entityRef.get();
        if (entity != null) {
            return NukkitUtil.toLocation(entity.getLocation());
        } else {
            return new Location(NullWorld.getInstance());
        }
    }

    @Override
    public BaseEntity getState() {
        Entity entity = entityRef.get();
        if (entity != null) {
            if (entity instanceof Player) {
                return null;
            }
            CompoundTag tag = NBTConverter.fromNative(entity.namedTag);
            return new BaseEntity(entity.getSaveId(), tag);
        } else {
            return null;
        }
    }

    @Override
    public boolean remove() {
        Entity entity = entityRef.get();
        if (entity != null) {
            entity.kill();
            return !entity.isAlive();
        } else {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        Entity entity = entityRef.get();
        if (entity != null && EntityType.class.isAssignableFrom(cls)) {
            return (T) new NukkitEntityType(entity);
        } else {
            return null;
        }
    }
}
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

package com.sk89q.worldedit.extent.clipboard;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.IntegerTrio;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores block data as a multi-dimensional array of {@link BaseBlock}s and
 * other data as lists or maps.
 */
public class BlockArrayClipboard implements Clipboard {

    private final Region region;
    private final short[][][] blocks;
    private final HashMap<IntegerTrio, CompoundTag> nbtMap;
    private final List<ClipboardEntity> entities = new ArrayList<ClipboardEntity>();
    private int mx;
    private int my;
    private int mz;
    private Vector origin;

    /**
     * Create a new instance.
     *
     * <p>The origin will be placed at the region's lowest minimum point.</p>
     *
     * @param region the bounding region
     */
    public BlockArrayClipboard(Region region) {
        checkNotNull(region);
        this.region = region.clone();
        Vector dimensions = getDimensions();
        blocks = new short[dimensions.getBlockX()][dimensions.getBlockY()][dimensions.getBlockZ()];
        nbtMap = new HashMap<>();
        this.origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
    }

    @Override
    public Region getRegion() {
        return region.clone();
    }

    @Override
    public Vector getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(Vector origin) {
        this.origin = origin;
    }

    @Override
    public Vector getDimensions() {
        return region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
    }

    @Override
    public Vector getMinimumPoint() {
        return region.getMinimumPoint();
    }

    @Override
    public Vector getMaximumPoint() {
        return region.getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        List<Entity> filtered = new ArrayList<Entity>();
        for (Entity entity : entities) {
            if (region.contains(entity.getLocation().toVector())) {
                filtered.add(entity);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        ClipboardEntity ret = new ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        if (region.contains(position)) {
            int x = position.getBlockX();
            int y = position.getBlockY();
            int z = position.getBlockZ();
            short combined = blocks[x - mx][y - my][z - mz];
            int id = combined >> 4;
            int data = combined & 0xF;
            BaseBlock block = new BaseBlock(id, data);
            if (FaweCache.hasNBT(id)) {
                CompoundTag nbt = nbtMap.get(new IntegerTrio(x, y, z));
                if (nbt != null) {
                    block.setNbtData(nbt);
                }
            }
            return block;
        }

        return new BaseBlock(BlockID.AIR);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return getBlock(position);
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        if (region.contains(location)) {
            final int id = block.getId();
            final int x = location.getBlockX();
            final int y = location.getBlockY();
            final int z = location.getBlockZ();
            switch (id) {
                case 54:
                case 130:
                case 142:
                case 27:
                case 137:
                case 52:
                case 154:
                case 84:
                case 25:
                case 144:
                case 138:
                case 176:
                case 177:
                case 63:
                case 119:
                case 68:
                case 323:
                case 117:
                case 116:
                case 28:
                case 66:
                case 157:
                case 61:
                case 62:
                case 140:
                case 146:
                case 149:
                case 150:
                case 158:
                case 23:
                case 123:
                case 124:
                case 29:
                case 33:
                case 151:
                case 178:
                    if (block.hasNbtData()) {
                        nbtMap.put(new IntegerTrio(x, y, z), block.getNbtData());
                    }
                    blocks[x - mx][y - my][z - mz] = (short) ((id << 4) + (block.getData()));
                    return true;
                case 0:
                case 2:
                case 4:
                case 13:
                case 14:
                case 15:
                case 20:
                case 21:
                case 22:
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
                case 56:
                case 57:
                case 58:
                case 60:
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
                case 85:
                case 87:
                case 88:
                case 101:
                case 102:
                case 103:
                case 110:
                case 112:
                case 113:
                case 121:
                case 122:
                case 129:
                case 133:
                case 165:
                case 166:
                case 169:
                case 170:
                case 172:
                case 173:
                case 174:
                case 181:
                case 182:
                case 188:
                case 189:
                case 190:
                case 191:
                case 192: {
                    blocks[x - mx][y - my][z - mz] = (short) (id << 4);
                    return true;
                }
                default: {
                    blocks[x - mx][y - my][z - mz] = (short) ((id << 4) + (block.getData()));
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return new BaseBiome(0);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    /**
     * Stores entity data.
     */
    private class ClipboardEntity implements Entity {
        private final Location location;
        private final BaseEntity entity;

        ClipboardEntity(Location location, BaseEntity entity) {
            checkNotNull(location);
            checkNotNull(entity);
            this.location = location;
            this.entity = new BaseEntity(entity);
        }

        @Override
        public boolean remove() {
            return entities.remove(this);
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
            return location;
        }

        @Override
        public Extent getExtent() {
            return location.getExtent();
        }

    }

    public static Class<?> inject() {
        return BlockArrayClipboard.class;
    }
}
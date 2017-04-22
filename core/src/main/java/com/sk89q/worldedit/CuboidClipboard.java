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

package com.sk89q.worldedit;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.DataException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The clipboard remembers the state of a cuboid region.
 *
 * @deprecated This is slowly being replaced with {@link Clipboard}, which is
 *             far more versatile. Transforms are supported using affine
 *             transformations and full entity support is provided because
 *             the clipboard properly implements {@link Extent}. However,
 *             the new clipboard class is only available in WorldEdit 6.x and
 *             beyond. We intend on keeping this deprecated class in WorldEdit
 *             for an extended amount of time so there is no rush to
 *             switch (but new features will not be supported). To copy between
 *             a clipboard and a world (or between any two {@code Extent}s),
 *             one can use {@link ForwardExtentCopy}. See
 *             {@link ClipboardCommands} and {@link SchematicCommands} for
 *             more information.
 */
@Deprecated
public class CuboidClipboard {

    /**
     * An enum of possible flip directions.
     */
    public enum FlipDirection {
        NORTH_SOUTH,
        WEST_EAST,
        UP_DOWN
    }

    public byte[][] ids;
    public byte[][] datas;
    public HashMap<IntegerTrio, CompoundTag> nbtMap;
    public List<CopiedEntity> entities = new ArrayList<>();

    public Vector size;
    private int dx;
    private int dxz;
    private Vector offset;
    private Vector origin;

    /**
     * Constructs the clipboard.
     *
     * @param size the dimensions of the clipboard (should be at least 1 on every dimension)
     */
    public CuboidClipboard(Vector size) {
        checkNotNull(size);
        MainUtil.warnDeprecated(BlockArrayClipboard.class, ClipboardFormat.class);
        origin = new Vector();
        offset = new Vector();
        this.size = size;
        this.dx = size.getBlockX();
        this.dxz = dx * size.getBlockZ();
        ids = new byte[dx * size.getBlockZ() * ((size.getBlockY() + 15) >>  4)][];
        nbtMap = new HashMap<>();
    }

    /**
     * Constructs the clipboard.
     *
     * @param size the dimensions of the clipboard (should be at least 1 on every dimension)
     * @param origin the origin point where the copy was made, which must be the
     *               {@link CuboidRegion#getMinimumPoint()} relative to the copy
     */
    public CuboidClipboard(Vector size, Vector origin) {
        checkNotNull(size);
        checkNotNull(origin);
        MainUtil.warnDeprecated(BlockArrayClipboard.class, ClipboardFormat.class);
        this.origin = origin;
        this.offset = new Vector();
        this.size = size;
        this.dx = size.getBlockX();
        this.dxz = dx * size.getBlockZ();
        ids = new byte[dx * size.getBlockZ() * ((size.getBlockY() + 15) >>  4)][];
        nbtMap = new HashMap<>();
    }

    /**
     * Constructs the clipboard.
     *
     * @param size the dimensions of the clipboard (should be at least 1 on every dimension)
     * @param origin the origin point where the copy was made, which must be the
     *               {@link CuboidRegion#getMinimumPoint()} relative to the copy
     * @param offset the offset from the minimum point of the copy where the user was
     */
    public CuboidClipboard(Vector size, Vector origin, Vector offset) {
        checkNotNull(size);
        checkNotNull(origin);
        checkNotNull(offset);
        MainUtil.warnDeprecated(BlockArrayClipboard.class, ClipboardFormat.class);
        this.origin = origin;
        this.offset = offset;
        this.size = size;
        this.dx = size.getBlockX();
        this.dxz = dx * size.getBlockZ();
        ids = new byte[dx * size.getBlockZ() * ((size.getBlockY() + 15) >>  4)][];
        nbtMap = new HashMap<>();
    }

    public BaseBlock getBlock(Vector position) {
        return getBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    public BaseBlock getBlock(int x, int y, int z) {
        int i = x + z * dx + (y >> 4) * dxz;
        byte[] idArray = ids[i];
        if (idArray == null) {
            return FaweCache.CACHE_BLOCK[0];
        }
        int y2 = y & 0xF;
        int id = idArray[y2] & 0xFF;
        BaseBlock block;
        if (!FaweCache.hasData(id) || datas == null) {
            block = FaweCache.CACHE_BLOCK[id << 4];
        } else {
            byte[] dataArray = datas[i];
            if (dataArray == null) {
                block = FaweCache.CACHE_BLOCK[id << 4];
            } else {
                block = FaweCache.CACHE_BLOCK[(id << 4) + dataArray[y2]];
            }
        }
        if (FaweCache.hasNBT(id)) {
            CompoundTag nbt = nbtMap.get(new IntegerTrio(x, y, z));
            if (nbt != null) {
                block = new BaseBlock(block.getId(), block.getData());
                block.setNbtData(nbt);
            }
        }
        return block;
    }

    public BaseBlock getLazyBlock(Vector position) {
        return getBlock(position);
    }

    public void setBlock(Vector location, BaseBlock block) {
        setBlock(location.getBlockX(),location.getBlockY(),location.getBlockZ(), block);
    }

    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        if (block == null) {
            int i = x + z * dx + (y >> 4) * dxz;
            int y2 = y & 0xF;
            byte[] idArray = ids[i];
            if (idArray == null) {
                return true;
            }
            idArray[y2] = (byte) 0;
            if (datas != null) {
                byte[] dataArray = datas[i];
                if (dataArray != null) {
                    dataArray[y2] = (byte) 0;
                }
            }
            nbtMap.remove(new IntegerTrio(x, y, z));
            return true;
        }
        final int id = block.getId();
        switch (id) {
            case 0:
                return true;
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
            case 209:
            case 210:
            case 211:
            case 255:
            case 219:
            case 220:
            case 221:
            case 222:
            case 223:
            case 224:
            case 225:
            case 226:
            case 227:
            case 228:
            case 229:
            case 230:
            case 231:
            case 232:
            case 233:
            case 234: {
                if (block.hasNbtData()) {
                    nbtMap.put(new IntegerTrio(x, y, z), block.getNbtData());
                }
                int i = x + z * dx + (y >> 4) * dxz;
                int y2 = y & 0xF;
                byte[] idArray = ids[i];
                if (idArray == null) {
                    idArray = new byte[16];
                    ids[i] = idArray;
                }
                idArray[y2] = (byte) id;
                if (FaweCache.hasData(id)) {
                    int data = block.getData();
                    if (data == 0) {
                        return true;
                    }
                    if (datas == null) {
                        datas = new byte[dx * size.getBlockZ() * ((size.getBlockY() + 15) >> 4)][];
                    }
                    byte[] dataArray = datas[i];
                    if (dataArray == null) {
                        dataArray = datas[i] = new byte[16];
                    }
                    dataArray[y2] = (byte) data;
                }
                return true;
            }
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
            case 11:
            case 73:
            case 74:
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
            case 172:
            case 173:
            case 174:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192: {
                int i = x + z * dx + (y >> 4) * dxz;
                int y2 = y & 0xF;
                byte[] idArray = ids[i];
                if (idArray == null) {
                    idArray = new byte[16];
                    ids[i] = idArray;
                }
                idArray[y2] = (byte) id;
                return true;
            }
            default: {
                int i = x + z * dx + (y >> 4) * dxz;
                int y2 = y & 0xF;
                byte[] idArray = ids[i];
                if (idArray == null) {
                    idArray = new byte[16];
                    ids[i] = idArray;
                }
                idArray[y2] = (byte) id;
                int data = block.getData();
                if (data == 0) {
                    return true;
                }
                if (datas == null) {
                    datas = new byte[dx * size.getBlockZ() * ((size.getBlockY() + 15) >> 4)][];
                }
                byte[] dataArray = datas[i];
                if (dataArray == null) {
                    dataArray = datas[i] = new byte[16];
                }
                dataArray[y2] = (byte) data;
                return true;
            }
        }
    }

    /**
     * Get the width (X-direction) of the clipboard.
     *
     * @return width
     */
    public int getWidth() {
        return size.getBlockX();
    }

    /**
     * Get the length (Z-direction) of the clipboard.
     *
     * @return length
     */
    public int getLength() {
        return size.getBlockZ();
    }

    /**
     * Get the height (Y-direction) of the clipboard.
     *
     * @return height
     */
    public int getHeight() {
        return size.getBlockY();
    }

    /**
     * Rotate the clipboard in 2D. It can only rotate by angles divisible by 90.
     *
     * @param angle in degrees
     */
    @SuppressWarnings("deprecation")
    public void rotate2D(int angle) {
        angle = angle % 360;
        if (angle % 90 != 0) { // Can only rotate 90 degrees at the moment
            return;
        }
        if (angle == 0) {
            return;
        }
        if (angle > 180) {
            angle -= 360;
        }
        final boolean reverse = angle < 0;
        final int numRotations = Math.abs((int) Math.floor(angle / 90.0));

        final int width = getWidth();
        final int length = getLength();
        final int height = getHeight();
        final Vector sizeRotated = size.transform2D(angle, 0, 0, 0, 0).round();
        final int shiftX = sizeRotated.getX() < 0 ? -sizeRotated.getBlockX() - 1 : 0;
        final int shiftZ = sizeRotated.getZ() < 0 ? -sizeRotated.getBlockZ() - 1 : 0;

        CuboidClipboard cloned = new CuboidClipboard(sizeRotated.positive().round());

        BaseBlock air = FaweCache.CACHE_BLOCK[0];

        for (int x = 0; x < width; ++x) {
            for (int z = 0; z < length; ++z) {
                final Vector2D v = new Vector2D(x, z).transform2D(angle, 0, 0, shiftX, shiftZ).round();
                final int newX = v.getBlockX();
                final int newZ = v.getBlockZ();
                for (int y = 0; y < height; ++y) {
                    BaseBlock block = getBlock(x, y, z);
                    if (block == air || block == null) {
                        continue;
                    }
                    if (reverse) {
                        for (int i = 0; i < numRotations; ++i) {
                            if (block.isImmutable()) {
                                block = new BaseBlock(block);
                            }
                            block.rotate90Reverse();
                        }
                    } else {
                        for (int i = 0; i < numRotations; ++i) {
                            if (block.isImmutable()) {
                                block = new BaseBlock(block);
                            }
                            block.rotate90();
                        }
                    }
                    cloned.setBlock(newX,y,newZ, block);
                }
            }
        }
        this.ids = cloned.ids;
        this.datas = cloned.datas;
        this.nbtMap = cloned.nbtMap;
        size = new Vector(Math.abs(sizeRotated.getBlockX()),
                Math.abs(sizeRotated.getBlockY()),
                Math.abs(sizeRotated.getBlockZ()));
        offset = offset.transform2D(angle, 0, 0, 0, 0)
                .subtract(shiftX, 0, shiftZ);
        dx = cloned.dx;
        dxz = cloned.dxz;
    }

    /**
     * Flip the clipboard.
     *
     * @param dir direction to flip
     */
    public void flip(FlipDirection dir) {
        flip(dir, false);
    }

    /**
     * Flip the clipboard.
     *
     * @param dir direction to flip
     * @param aroundPlayer flip the offset around the player
     */
    @SuppressWarnings("deprecation")
    public void flip(FlipDirection dir, boolean aroundPlayer) {
        checkNotNull(dir);

        final int width = getWidth();
        final int length = getLength();
        final int height = getHeight();

        switch (dir) {
            case WEST_EAST:
                final int wid = (int) Math.ceil(width / 2.0f);
                for (int xs = 0; xs < wid; ++xs) {
                    for (int z = 0; z < length; ++z) {
                        for (int y = 0; y < height; ++y) {
                            BaseBlock block1 = getBlock(xs,y,z);
                            if (block1 != null) {
                                block1.flip(dir);
                            }
                            // Skip the center plane
                            if (xs == width - xs - 1) {
                                continue;
                            }
                            BaseBlock block2 = getBlock(width - xs - 1,y,z);
                            if (block2 != null) {
                                block2.flip(dir);
                            }
                            setBlock(xs,y,z, block2);
                            setBlock(width - xs - 1,y,z, block1);
                        }
                    }
                }

                if (aroundPlayer) {
                    offset.mutX(1 - offset.getX() - width);
                }

                break;

            case NORTH_SOUTH:
                final int len = (int) Math.ceil(length / 2.0f);
                for (int zs = 0; zs < len; ++zs) {
                    for (int x = 0; x < width; ++x) {
                        for (int y = 0; y < height; ++y) {
                            BaseBlock block1 = getBlock(x,y,zs);
                            if (block1 != null) {
                                block1.flip(dir);
                            }

                            // Skip the center plane
                            if (zs == length - zs - 1) {
                                continue;
                            }

                            BaseBlock block2 = getBlock(x,y,length - zs - 1);
                            if (block2 != null) {
                                block2.flip(dir);
                            }

                            setBlock(x,y,zs,block2);
                            setBlock(x,y,length - zs - 1,block1);


                        }
                    }
                }

                if (aroundPlayer) {
                    offset.mutZ(1 - offset.getZ() - length);
                }

                break;

            case UP_DOWN:
                final int hei = (int) Math.ceil(height / 2.0f);
                for (int ys = 0; ys < hei; ++ys) {
                    for (int x = 0; x < width; ++x) {
                        for (int z = 0; z < length; ++z) {
                            BaseBlock block1 = getBlock(x,ys,z);
                            if (block1 != null) {
                                block1.flip(dir);
                            }

                            // Skip the center plane
                            if (ys == height - ys - 1) {
                                continue;
                            }

                            BaseBlock block2 = getBlock(x,height - ys - 1,z);
                            if (block2 != null) {
                                block2.flip(dir);
                            }

                            setBlock(x,ys,z, block2);
                            setBlock(x,height - ys - 1,z, block1);
                        }
                    }
                }

                if (aroundPlayer) {
                    offset.mutY(1 - offset.getY() - height);
                }

                break;
        }
    }

    /**
     * Copies blocks to the clipboard.
     *
     * @param editSession the EditSession from which to take the blocks
     */
    public void copy(EditSession editSession) {
        for (int x = 0; x < size.getBlockX(); ++x) {
            for (int y = 0; y < size.getBlockY(); ++y) {
                for (int z = 0; z < size.getBlockZ(); ++z) {
                    setBlock(x, y, z, editSession.getBlock(new Vector(x, y, z).add(getOrigin())));
                }
            }
        }
    }

    /**
     * Copies blocks to the clipboard.
     *
     * @param editSession The EditSession from which to take the blocks
     * @param region A region that further constrains which blocks to take.
     */
    public void copy(EditSession editSession, Region region) {
        for (int x = 0; x < size.getBlockX(); ++x) {
            for (int y = 0; y < size.getBlockY(); ++y) {
                for (int z = 0; z < size.getBlockZ(); ++z) {
                    final Vector pt = new Vector(x, y, z).add(getOrigin());
                    if (region.contains(pt)) {
                        setBlock(x, y, z, editSession.getBlock(pt));
                    } else {
                        setBlock(x, y, z, null);
                    }
                }
            }
        }
    }

    /**
     * Paste the clipboard at the given location using the given {@code EditSession}.
     *
     * <p>This method blocks the server/game until the entire clipboard is
     * pasted. In the future, {@link ForwardExtentCopy} will be recommended,
     * which, if combined with the proposed operation scheduler framework,
     * will not freeze the game/server.</p>
     *
     * @param editSession the EditSession to which blocks are to be copied to
     * @param newOrigin the new origin point (must correspond to the minimum point of the cuboid)
     * @param noAir true to not copy air blocks in the source
     * @throws MaxChangedBlocksException thrown if too many blocks were changed
     */
    public void paste(EditSession editSession, Vector newOrigin, boolean noAir) throws MaxChangedBlocksException {
        paste(editSession, newOrigin, noAir, false);
    }

    /**
     * Paste the clipboard at the given location using the given {@code EditSession}.
     *
     * <p>This method blocks the server/game until the entire clipboard is
     * pasted. In the future, {@link ForwardExtentCopy} will be recommended,
     * which, if combined with the proposed operation scheduler framework,
     * will not freeze the game/server.</p>
     *
     * @param editSession the EditSession to which blocks are to be copied to
     * @param newOrigin the new origin point (must correspond to the minimum point of the cuboid)
     * @param noAir true to not copy air blocks in the source
     * @param entities true to copy entities
     * @throws MaxChangedBlocksException thrown if too many blocks were changed
     */
    public void paste(EditSession editSession, Vector newOrigin, boolean noAir, boolean entities) throws MaxChangedBlocksException {
        place(editSession, newOrigin.add(offset), noAir);
        if (entities) {
            pasteEntities(newOrigin.add(offset));
        }
    }

    /**
     * Paste the clipboard at the given location using the given {@code EditSession}.
     *
     * <p>This method blocks the server/game until the entire clipboard is
     * pasted. In the future, {@link ForwardExtentCopy} will be recommended,
     * which, if combined with the proposed operation scheduler framework,
     * will not freeze the game/server.</p>
     *
     * @param editSession the EditSession to which blocks are to be copied to
     * @param newOrigin the new origin point (must correspond to the minimum point of the cuboid)
     * @param noAir true to not copy air blocks in the source
     * @throws MaxChangedBlocksException thrown if too many blocks were changed
     */
    public void place(EditSession editSession, Vector newOrigin, boolean noAir) throws MaxChangedBlocksException {
        Vector v = new Vector(0,0,0);
        int ox = newOrigin.getBlockX();
        int oy = newOrigin.getBlockY();
        int oz = newOrigin.getBlockZ();
        for (int x = 0; x < size.getBlockX(); ++x) {
            v.mutX(x + ox);
            for (int y = 0; y < size.getBlockY(); ++y) {
                v.mutY(y + oy);
                for (int z = 0; z < size.getBlockZ(); ++z) {
                    v.mutZ(z + oz);
                    final BaseBlock block = getBlock(x,y,z);
                    if (block == null) {
                        continue;
                    }
                    if (noAir && block.isAir()) {
                        continue;
                    }
                    editSession.setBlockFast(new Vector(x, y, z).add(newOrigin), block);
                }
            }
        }
        editSession.flushQueue();
    }

    /**
     * Paste the stored entities to the given position.
     *
     * @param newOrigin the new origin
     * @return a list of entities that were pasted
     */
    public LocalEntity[] pasteEntities(Vector newOrigin) {
        LocalEntity[] entities = new LocalEntity[this.entities.size()];
        for (int i = 0; i < this.entities.size(); ++i) {
            CopiedEntity copied = this.entities.get(i);
            if (copied.entity.spawn(copied.entity.getPosition().setPosition(copied.relativePosition.add(newOrigin)))) {
                entities[i] = copied.entity;
            }
        }
        return entities;
    }

    /**
     * Store an entity.
     *
     * @param entity the entity
     */
    public void storeEntity(LocalEntity entity) {
        this.entities.add(new CopiedEntity(entity));
    }

    /**
     * Get the block at the given position.
     *
     * <p>If the position is out of bounds, air will be returned.</p>
     *
     * @param position the point, relative to the origin of the copy (0, 0, 0) and not to the actual copy origin
     * @return air, if this block was outside the (non-cuboid) selection while copying
     * @throws ArrayIndexOutOfBoundsException if the position is outside the bounds of the CuboidClipboard
     * @deprecated use {@link #getBlock(Vector)} instead
     */
    @Deprecated
    public BaseBlock getPoint(Vector position) throws ArrayIndexOutOfBoundsException {
        final BaseBlock block = getBlock(position);
        if (block == null) {
            return new BaseBlock(BlockID.AIR);
        }

        return block;
    }

    /**
     * Get the dimensions of the clipboard.
     *
     * @return the dimensions, where (1, 1, 1) is 1 wide, 1 across, 1 deep
     */
    public Vector getSize() {
        return size;
    }

    /**
     * Saves the clipboard data to a .schematic-format file.
     *
     * @param path the path to the file to save
     * @throws IOException thrown on I/O error
     * @throws DataException thrown on error writing the data for other reasons
     * @deprecated use {@link SchematicFormat#MCEDIT}
     */
    @Deprecated
    public void saveSchematic(File path) throws IOException, DataException {
        checkNotNull(path);
        SchematicFormat.MCEDIT.save(this, path);
    }

    /**
     * Load a .schematic file into a clipboard.
     *
     * @param path the path to the file to load
     * @return a clipboard
     * @throws IOException thrown on I/O error
     * @throws DataException thrown on error writing the data for other reasons
     * @deprecated use {@link SchematicFormat#MCEDIT}
     */
    @Deprecated
    public static CuboidClipboard loadSchematic(File path) throws DataException, IOException {
        checkNotNull(path);
        return SchematicFormat.MCEDIT.load(path);
    }

    /**
     * Get the origin point, which corresponds to where the copy was
     * originally copied from. The origin is the lowest possible X, Y, and
     * Z components of the cuboid region that was copied.
     *
     * @return the origin
     */
    public Vector getOrigin() {
        return origin;
    }

    /**
     * Set the origin point, which corresponds to where the copy was
     * originally copied from. The origin is the lowest possible X, Y, and
     * Z components of the cuboid region that was copied.
     *
     * @param origin the origin to set
     */
    public void setOrigin(Vector origin) {
        checkNotNull(origin);
        this.origin = origin;
    }

    /**
     * Get the offset of the player to the clipboard's minimum point
     * (minimum X, Y, Z coordinates).
     *
     * <p>The offset is inverse (multiplied by -1).</p>
     *
     * @return the offset the offset
     */
    public Vector getOffset() {
        return offset;
    }

    /**
     * Set the offset of the player to the clipboard's minimum point
     * (minimum X, Y, Z coordinates).
     *
     * <p>The offset is inverse (multiplied by -1).</p>
     *
     * @param offset the new offset
     */
    public void setOffset(Vector offset) {
        this.offset = offset;
    }

    /**
     * Get the block distribution inside a clipboard.
     *
     * @return a block distribution
     */
    public List<Countable<Integer>> getBlockDistribution() {
        List<Countable<Integer>> distribution = new ArrayList<Countable<Integer>>();
        Map<Integer, Countable<Integer>> map = new HashMap<Integer, Countable<Integer>>();

        int maxX = getWidth();
        int maxY = getHeight();
        int maxZ = getLength();

        for (int x = 0; x < maxX; ++x) {
            for (int y = 0; y < maxY; ++y) {
                for (int z = 0; z < maxZ; ++z) {
                    final BaseBlock block = getBlock(x,y,z);
                    if (block == null) {
                        continue;
                    }

                    int id = block.getId();

                    if (map.containsKey(id)) {
                        map.get(id).increment();
                    } else {
                        Countable<Integer> c = new Countable<Integer>(id, 1);
                        map.put(id, c);
                        distribution.add(c);
                    }
                }
            }
        }

        Collections.sort(distribution);
        // Collections.reverse(distribution);

        return distribution;
    }

    /**
     * Get the block distribution inside a clipboard with data values.
     *
     * @return a block distribution
     */
    // TODO reduce code duplication
    public List<Countable<BaseBlock>> getBlockDistributionWithData() {
        List<Countable<BaseBlock>> distribution = new ArrayList<Countable<BaseBlock>>();
        Map<BaseBlock, Countable<BaseBlock>> map = new HashMap<BaseBlock, Countable<BaseBlock>>();

        int maxX = getWidth();
        int maxY = getHeight();
        int maxZ = getLength();

        for (int x = 0; x < maxX; ++x) {
            for (int y = 0; y < maxY; ++y) {
                for (int z = 0; z < maxZ; ++z) {
                    final BaseBlock block = getBlock(x,y,z);
                    if (block == null) {
                        continue;
                    }

                    // Strip the block from metadata that is not part of our key
                    final BaseBlock bareBlock = new BaseBlock(block.getId(), block.getData());

                    if (map.containsKey(bareBlock)) {
                        map.get(bareBlock).increment();
                    } else {
                        Countable<BaseBlock> c = new Countable<BaseBlock>(bareBlock, 1);
                        map.put(bareBlock, c);
                        distribution.add(c);
                    }
                }
            }
        }

        Collections.sort(distribution);
        // Collections.reverse(distribution);

        return distribution;
    }

    /**
     * Stores a copied entity.
     */
    private class CopiedEntity {
        private final LocalEntity entity;
        private final Vector relativePosition;

        private CopiedEntity(LocalEntity entity) {
            this.entity = entity;
            this.relativePosition = entity.getPosition().getPosition().subtract(getOrigin());
        }
    }

    public static Class<?> inject() {
        return CuboidClipboard.class;
    }
}
package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import java.util.List;
import java.util.Map;

public abstract class ReadOnlyClipboard extends FaweClipboard {
    private final Region region;

    public ReadOnlyClipboard(Region region) {
        this.region = region;
    }

    public static ReadOnlyClipboard of(final EditSession editSession, final Region region) {
        final Vector origin = region.getMinimumPoint();
        final int mx = origin.getBlockX();
        final int my = origin.getBlockY();
        final int mz = origin.getBlockZ();
        return new ReadOnlyClipboard(region) {
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
                Vector min = region.getMinimumPoint();
                Vector max = region.getMaximumPoint();
                Vector pos = new Vector();
                if (region instanceof CuboidRegion) {
                    if (air) {
                        for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                                for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                                    BaseBlock block = getBlockAbs(x, y, z);
                                    pos.x = x - mx;
                                    pos.y = y - my;
                                    pos.z = z - mz;
                                    CompoundTag tag = block.getNbtData();
                                    if (tag != null) {
                                        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                                        values.put("x", new IntTag((int) pos.x));
                                        values.put("y", new IntTag((int) pos.y));
                                        values.put("z", new IntTag((int) pos.z));
                                    }
                                    task.run(pos, block);
                                }
                            }
                        }
                    } else {
                        for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                                for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                                    BaseBlock block = getBlockAbs(x, y, z);
                                    if (block == EditSession.nullBlock) {
                                        continue;
                                    }
                                    pos.x = x - mx;
                                    pos.y = y - my;
                                    pos.z = z - mz;
                                    CompoundTag tag = block.getNbtData();
                                    if (tag != null) {
                                        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                                        values.put("x", new IntTag((int) pos.x));
                                        values.put("y", new IntTag((int) pos.y));
                                        values.put("z", new IntTag((int) pos.z));
                                    }
                                    task.run(pos, block);
                                }
                            }
                        }
                    }
                } else {
                    for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                                pos.x = x;
                                pos.y = y;
                                pos.z = z;
                                if (region.contains(pos)) {
                                    BaseBlock block = getBlockAbs(x, y, z);
                                    if (!air && block == EditSession.nullBlock) {
                                        continue;
                                    }
                                    pos.x -= mx;
                                    pos.y -= my;
                                    pos.z -= mz;
                                    CompoundTag tag = block.getNbtData();
                                    if (tag != null) {
                                        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                                        values.put("x", new IntTag((int) pos.x));
                                        values.put("y", new IntTag((int) pos.y));
                                        values.put("z", new IntTag((int) pos.z));
                                    }
                                    task.run(pos, block);
                                } else if (air) {
                                    pos.x -= mx;
                                    pos.y -= my;
                                    pos.z -= mz;
                                    task.run(pos, EditSession.nullBlock);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public Vector getDimensions() {
        return region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
    }

    @Override
    public void setDimensions(Vector dimensions) {
        throw new UnsupportedOperationException("Clipboard is immutable");
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

    @Override
    public void setId(int index, int id) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public void setData(int index, int data) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public void setAdd(int index, int id) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }
}

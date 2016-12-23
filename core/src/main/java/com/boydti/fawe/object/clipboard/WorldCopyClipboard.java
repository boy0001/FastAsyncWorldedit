package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import java.util.List;
import java.util.Map;

public class WorldCopyClipboard extends ReadOnlyClipboard {

    public final int mx,my,mz;
    public final EditSession editSession;

    public WorldCopyClipboard(EditSession editSession, Region region) {
        super(region);
        final Vector origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
        this.editSession = editSession;
    }

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
}

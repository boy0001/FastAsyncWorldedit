package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
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
    public void forEach(final RunnableVal2<Vector, BaseBlock> task, boolean air) {
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        final Vector pos = new Vector();
        if (region instanceof CuboidRegion) {
            if (air) {
                RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                    @Override
                    public boolean apply(Vector pos) throws WorldEditException {
                        int x = pos.getBlockX();
                        int y = pos.getBlockY();
                        int z = pos.getBlockZ();
                        BaseBlock block = getBlockAbs(x, y, z);
                        pos.mutX(x - mx);
                        pos.mutY(y - my);
                        pos.mutZ(z - mz);
                        CompoundTag tag = block.getNbtData();
                        if (tag != null) {
                            Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                            values.put("x", new IntTag(pos.getBlockX()));
                            values.put("y", new IntTag(pos.getBlockY()));
                            values.put("z", new IntTag(pos.getBlockZ()));
                        }
                        task.run(pos, block);
                        return true;
                    }
                }, editSession);
                Operations.completeBlindly(visitor);
            } else {
                RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                    @Override
                    public boolean apply(Vector pos) throws WorldEditException {
                        int x = pos.getBlockX();
                        int y = pos.getBlockY();
                        int z = pos.getBlockZ();
                        BaseBlock block = getBlockAbs(x, y, z);
                        if (block == EditSession.nullBlock) {
                            return false;
                        }
                        pos.mutX(x - mx);
                        pos.mutY(y - my);
                        pos.mutZ(z - mz);
                        CompoundTag tag = block.getNbtData();
                        if (tag != null) {
                            Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                            values.put("x", new IntTag(pos.getBlockX()));
                            values.put("y", new IntTag(pos.getBlockY()));
                            values.put("z", new IntTag(pos.getBlockZ()));
                        }
                        task.run(pos, block);
                        return true;
                    }
                }, editSession);
                Operations.completeBlindly(visitor);
            }
        } else {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                        pos.mutX(x);
                        pos.mutY(y);
                        pos.mutZ(z);
                        if (region.contains(pos)) {
                            BaseBlock block = getBlockAbs(x, y, z);
                            if (!air && block == EditSession.nullBlock) {
                                continue;
                            }
                            pos.mutX(pos.getX() - mx);
                            pos.mutY(pos.getY() - my);
                            pos.mutZ(pos.getZ() - mz);
                            CompoundTag tag = block.getNbtData();
                            if (tag != null) {
                                Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                                values.put("x", new IntTag(pos.getBlockX()));
                                values.put("y", new IntTag(pos.getBlockY()));
                                values.put("z", new IntTag(pos.getBlockZ()));
                            }
                            task.run(pos, block);
                        } else if (air) {
                            pos.mutX(pos.getX() - mx);
                            pos.mutY(pos.getY() - my);
                            pos.mutZ(pos.getZ() - mz);
                            task.run(pos, EditSession.nullBlock);
                        }
                    }
                }
            }
        }
    }
}

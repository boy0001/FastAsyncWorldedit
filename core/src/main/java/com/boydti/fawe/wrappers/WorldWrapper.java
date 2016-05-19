package com.boydti.fawe.wrappers;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.extent.FaweRegionExtent;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.WorldData;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public class WorldWrapper extends AbstractWorld {

    private final AbstractWorld parent;

    public WorldWrapper(AbstractWorld parent) {
        this.parent = parent;
    }

    public AbstractWorld getParent() {
        return parent instanceof WorldWrapper ? ((WorldWrapper) parent).getParent() : parent;
    }

    @Override
    public boolean useItem(Vector position, BaseItem item, Direction face) {
        return parent.useItem(position, item, face);
    }

    @Override
    public int getMaxY() {
        return parent.getMaxY();
    }

    @Override
    public boolean isValidBlockType(int type) {
        return parent.isValidBlockType(type);
    }

    @Override
    public boolean usesBlockData(int type) {
        return parent.usesBlockData(type);
    }

    @Override
    public Mask createLiquidMask() {
        return parent.createLiquidMask();
    }

    @Override
    public int getBlockType(Vector pt) {
        return parent.getBlockType(pt);
    }

    @Override
    public int getBlockData(Vector pt) {
        return parent.getBlockData(pt);
    }

    @Override
    public void dropItem(Vector pt, BaseItemStack item, int times) {
        parent.dropItem(pt, item, times);
    }

    @Override
    public void simulateBlockMine(Vector pt) {
        parent.simulateBlockMine(pt);
    }

    @Override
    public boolean generateTree(final EditSession editSession, final Vector pt) throws MaxChangedBlocksException {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean ignore) {
                try {
                    this.value = parent.generateTree(editSession, pt);
                } catch (MaxChangedBlocksException e) {
                    MainUtil.handleError(e);
                }
            }
        });
    }

    @Override
    public boolean generateBigTree(final EditSession editSession, final Vector pt) throws MaxChangedBlocksException {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean ignore) {
                try {
                    this.value = parent.generateBigTree(editSession, pt);
                } catch (MaxChangedBlocksException e) {
                    MainUtil.handleError(e);
                }
            }
        });
    }

    @Override
    public boolean generateBirchTree(final EditSession editSession, final Vector pt) throws MaxChangedBlocksException {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean ignore) {
                try {
                    this.value = parent.generateBirchTree(editSession, pt);
                } catch (MaxChangedBlocksException e) {
                    MainUtil.handleError(e);
                }
            }
        });
    }

    @Override
    public boolean generateRedwoodTree(final EditSession editSession, final Vector pt) throws MaxChangedBlocksException {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean ignore) {
                try {
                    this.value = parent.generateRedwoodTree(editSession, pt);
                } catch (MaxChangedBlocksException e) {
                    MainUtil.handleError(e);
                }
            }
        });
    }

    @Override
    public boolean generateTallRedwoodTree(final EditSession editSession, final Vector pt) throws MaxChangedBlocksException {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean ignore) {
                try {
                    this.value = parent.generateTallRedwoodTree(editSession, pt);
                } catch (MaxChangedBlocksException e) {
                    MainUtil.handleError(e);
                }
            }
        });
    }

    @Override
    public void checkLoadedChunk(Vector pt) {
        parent.checkLoadedChunk(pt);
    }

    @Override
    public void fixAfterFastMode(Iterable<BlockVector2D> chunks) {
        parent.fixAfterFastMode(chunks);
    }

    @Override
    public void fixLighting(Iterable<BlockVector2D> chunks) {
        parent.fixLighting(chunks);
    }

    @Override
    public boolean playEffect(Vector position, int type, int data) {
        return parent.playEffect(position, type, data);
    }

    @Override
    public boolean queueBlockBreakEffect(Platform server, Vector position, int blockId, double priority) {
        return parent.queueBlockBreakEffect(server, position, blockId, priority);
    }

    @Override
    public Vector getMinimumPoint() {
        return parent.getMinimumPoint();
    }

    @Override
    public Vector getMaximumPoint() {
        return parent.getMaximumPoint();
    }

    @Override
    @Nullable
    public Operation commit() {
        return parent.commit();
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block, boolean notifyAndLight) throws WorldEditException {
        return parent.setBlock(position, block, notifyAndLight);
    }


    @Override
    public int getBlockLightLevel(Vector position) {
        return parent.getBlockLightLevel(position);
    }

    @Override
    public boolean clearContainerBlockContents(Vector position) {
        return parent.clearContainerBlockContents(position);
    }

    @Override
    public void dropItem(Vector position, BaseItemStack item) {
        parent.dropItem(position, item);
    }

    @Override
    public boolean regenerate(final Region region, final EditSession session) {
        final FaweQueue queue = session.getQueue();
        queue.setChangeTask(null);
        final FaweChangeSet fcs = (FaweChangeSet) session.getChangeSet();
        final FaweRegionExtent fe = session.getRegionExtent();
        session.setChangeSet(fcs);
        final boolean cuboid = region instanceof CuboidRegion;
        Set<Vector2D> chunks = region.getChunks();
        TaskManager.IMP.objectTask(chunks, new RunnableVal<Vector2D>() {
            @Override
            public void run(Vector2D chunk) {
                int cx = chunk.getBlockX();
                int cz = chunk.getBlockZ();
                int bx = cx << 4;
                int bz = cz << 4;
                Vector cmin = new Vector(bx, 0, bz);
                Vector cmax = cmin.add(15, getMaxY(), 15);
                boolean containsBot1 = (fe == null || fe.contains(cmin.getBlockX(), cmin.getBlockY(), cmin.getBlockZ()));
                boolean containsBot2 = region.contains(cmin);
                boolean containsTop1 = (fe == null || fe.contains(cmax.getBlockX(), cmax.getBlockY(), cmax.getBlockZ()));
                boolean containsTop2 = region.contains(cmax);
                if ((containsBot2 && containsTop2 && !containsBot1 && !containsTop1)) {
                    return;
                }
                if (cuboid && containsBot1 && containsBot2 && containsTop1 && containsTop2) {
                    if (fcs != null) {
                        for (int x = 0; x < 16; x++) {
                            int xx = x + bx;
                            for (int z = 0; z < 16; z++) {
                                int zz = z + bz;
                                for (int y = 0; y < getMaxY() + 1; y++) {
                                    int from = queue.getCombinedId4DataDebug(xx, y, zz, 0, session);
                                    if (!FaweCache.hasNBT(from >> 4)) {
                                        fcs.add(xx, y, zz, from, 0);
                                    } else {
                                        try {
                                            Vector loc = new Vector(xx, y, zz);
                                            BaseBlock block = getLazyBlock(loc);
                                            fcs.add(loc, block, FaweCache.CACHE_BLOCK[0]);
                                        } catch (Throwable e) {
                                            fcs.add(xx, y, zz, from, 0);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Vector mutable = new Vector(0,0,0);
                    for (int x = 0; x < 16; x++) {
                        int xx = x + bx;
                        mutable.x = xx;
                        for (int z = 0; z < 16; z++) {
                            int zz = z + bz;
                            mutable.z = zz;
                            for (int y = 0; y < getMaxY() + 1; y++) {
                                mutable.y = y;
                                int from = queue.getCombinedId4Data(xx, y, zz);
                                boolean contains = (fe == null || fe.contains(xx, y, zz)) && region.contains(mutable);
                                if (contains) {
                                    if (fcs != null) {
                                        if (!FaweCache.hasNBT(from >> 4)) {
                                            fcs.add(xx, y, zz, from, 0);
                                        } else {
                                            try {
                                                BaseBlock block = getLazyBlock(mutable);
                                                fcs.add(mutable, block, FaweCache.CACHE_BLOCK[0]);
                                            } catch (Throwable e) {
                                                fcs.add(xx, y, zz, from, 0);
                                            }
                                        }
                                    }
                                } else {
                                    short id = (short) (from >> 4);
                                    byte data = (byte) (from & 0xf);
                                    queue.setBlock(xx, y, zz, id, data);
                                    if (FaweCache.hasNBT(id)) {
                                        BaseBlock block = getBlock(new Vector(xx, y, zz));
                                        if (block.hasNbtData()) {
                                            queue.setTile(xx, y, zz, block.getNbtData());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                queue.regenerateChunk(cx, cz);
            }
        }, new Runnable() {
            @Override
            public void run() {
                queue.enqueue();
            }
        });
        return false;
    }

    @Override
    public boolean generateTree(final TreeGenerator.TreeType type, final EditSession editSession, final Vector position) throws MaxChangedBlocksException {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean ignore) {
                try {
                    this.value = parent.generateTree(editSession, position);
                } catch (MaxChangedBlocksException e) {
                    MainUtil.handleError(e);
                }
            }
        });
    }

    @Override
    public WorldData getWorldData() {
        return parent.getWorldData();
    }

    @Override
    public boolean equals(Object other) {
        return parent.equals(other);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return parent.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return parent.getEntities();
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return parent.createEntity(location, entity);
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return parent.getBlock(position);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return parent.getLazyBlock(position);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return parent.getBiome(position);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return parent.setBiome(position, biome);
    }
}

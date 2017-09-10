package com.boydti.fawe.wrappers;

import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
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
import com.sk89q.worldedit.internal.LocalWorldAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.WorldData;
import java.util.List;
import javax.annotation.Nullable;

public class WorldWrapper extends LocalWorld {

    private final World parent;

    public static WorldWrapper wrap(World world) {
        if (world == null) {
            return null;
        }
        if (world instanceof WorldWrapper) {
            return (WorldWrapper) world;
        }
        return new WorldWrapper(world);
    }

    public static World unwrap(World world) {
        if (world instanceof WorldWrapper) {
            return unwrap(((WorldWrapper) world).getParent());
        }
        if (world instanceof LocalWorldAdapter) {
            return unwrap(LocalWorldAdapter.unwrap(world));
        }
        else if (world instanceof EditSession) {
            return unwrap(((EditSession) world).getWorld());
        }
        return world;
    }

    private WorldWrapper(World parent) {
        this.parent = parent;
    }

    public World getParent() {
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
    public void simulateBlockMine(final Vector pt) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.simulateBlockMine(pt);
            }
        });
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
        try {
            return setBlock(position, EditSession.nullBlock, true);
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }
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
        return session.regenerate(region);
    }

    @Override
    public boolean generateTree(final TreeGenerator.TreeType type, final EditSession editSession, final Vector position) throws MaxChangedBlocksException {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean ignore) {
                try {
                    this.value = parent.generateTree(type, editSession, position);
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
    public List<? extends Entity> getEntities(final Region region) {
        return TaskManager.IMP.sync(new RunnableVal<List<? extends Entity>>() {
            @Override
            public void run(List<? extends Entity> value) {
                this.value = parent.getEntities(region);
            }
        });
    }

    @Override
    public List<? extends Entity> getEntities() {
        return TaskManager.IMP.sync(new RunnableVal<List<? extends Entity>>() {
            @Override
            public void run(List<? extends Entity> value) {
                this.value = parent.getEntities();
            }
        });
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

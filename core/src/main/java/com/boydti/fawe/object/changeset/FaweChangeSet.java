package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.BytePair;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.world.World;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FaweChangeSet implements ChangeSet {

    private final World world;

    private final boolean mainThread;

    public static FaweChangeSet getDefaultChangeSet(World world, UUID uuid) {
        if (Settings.HISTORY.USE_DISK) {
            return new DiskStorageHistory(world, uuid);
        } else {
            return new MemoryOptimizedHistory(world);
        }
    }

    public FaweChangeSet(World world) {
        this.world = world;
        this.mainThread = Fawe.get().isMainThread();
    }

    public World getWorld() {
        return world;
    }

    public boolean flush() {
        try {
            while (waiting.get() > 0) {
                synchronized (lock) {
                    lock.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            MainUtil.handleError(e);
        }
        return true;
    }

    public abstract void add(int x, int y, int z, int combinedFrom, int combinedTo);

    @Override
    public Iterator<Change> backwardIterator() {
        return getIterator(false);
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return getIterator(true);
    }

    public abstract void addTileCreate(CompoundTag tag);
    public abstract void addTileRemove(CompoundTag tag);
    public abstract void addEntityRemove(CompoundTag tag);
    public abstract void addEntityCreate(CompoundTag tag);
    public abstract Iterator<Change> getIterator(boolean redo);

    public void delete() {};

    public EditSession toEditSession(FawePlayer player) {
        EditSession editSession = new EditSessionBuilder(world).player(player).autoQueue(false).fastmode(false).checkMemory(false).changeSet(this).allowedRegions(RegionWrapper.GLOBAL().toArray()).build();
        editSession.setSize(1);
        return editSession;
    }

    public void add(EntityCreate change) {
        CompoundTag tag = change.state.getNbtData();
        MainUtil.setEntityInfo(tag, change.entity);
        addEntityCreate(tag);
    }

    public void add(EntityRemove change) {
        CompoundTag tag = change.state.getNbtData();
        MainUtil.setEntityInfo(tag, change.entity);
        addEntityRemove(tag);
    }

    public void add(Change change) {
        if (change.getClass() == BlockChange.class) {
            add((BlockChange) change);
        } else if (change.getClass() == EntityCreate.class) {
            add((EntityCreate) change);
        } else if (change.getClass() == EntityRemove.class) {
            add((EntityRemove) change);
        } else {
            Fawe.debug("Unknown change: " + change.getClass());
        }
    }

    public void add(BlockChange change) {
        try {
            BlockVector loc = change.getPosition();
            BaseBlock from = change.getPrevious();
            BaseBlock to = change.getCurrent();
            add(loc, from, to);
        } catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    public void add(Vector loc, BaseBlock from, BaseBlock to) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        add(x, y, z, from, to);
    }

    public void add(int x, int y, int z, BaseBlock from, BaseBlock to) {
        try {
            if (from.hasNbtData()) {
                CompoundTag nbt = from.getNbtData();
                MainUtil.setPosition(nbt, x, y, z);
                addTileRemove(nbt);
            }
            if (to.hasNbtData()) {
                CompoundTag nbt = to.getNbtData();
                MainUtil.setPosition(nbt, x, y, z);
                addTileRemove(nbt);
            }
            int combinedFrom = (from.getId() << 4) + from.getData();
            int combinedTo = (to.getId() << 4) + to.getData();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    public void add(int x, int y, int z, int combinedFrom, BaseBlock to) {
        try {
            if (to.hasNbtData()) {
                CompoundTag nbt = to.getNbtData();
                MainUtil.setPosition(nbt, x, y, z);
                addTileRemove(nbt);
            }
            int combinedTo = (to.getId() << 4) + to.getData();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    private AtomicInteger waiting = new AtomicInteger(0);
    private Object lock = new Object();

    public void addChangeTask(FaweQueue queue) {
        queue.setChangeTask(new RunnableVal2<FaweChunk, FaweChunk>() {
            @Override
            public void run(final FaweChunk previous, final FaweChunk next) {
                waiting.incrementAndGet();
                Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int cx = previous.getX();
                            int cz = previous.getZ();
                            int bx = cx << 4;
                            int bz = cz << 4;
                            // Biome changes
                            {
                                // TODO
                            }
                            // Block changes
                            {
                                // Current blocks
                                char[][] currentIds = next.getCombinedIdArrays();
                                // Previous blocks in modified sections (i.e. we skip sections that weren't modified)
                                char[][] previousIds = previous.getCombinedIdArrays();
                                for (int layer = 0; layer < currentIds.length; layer++) {
                                    char[] currentLayer = currentIds[layer];
                                    char[] previousLayer = previousIds[layer];
                                    if (currentLayer == null) {
                                        continue;
                                    }
                                    int startY = layer << 4;
                                    for (int y = 0; y < 16; y++) {
                                        short[][] i1 = FaweCache.CACHE_J[y];
                                        int yy = y + startY;
                                        for (int z = 0; z < 16; z++) {
                                            int zz = z + bz;
                                            short[] i2 = i1[z];
                                            for (int x = 0; x < 16; x++) {
                                                int xx = x + bx;
                                                int index = i2[x];
                                                int combinedIdCurrent = currentLayer[index];
                                                switch (combinedIdCurrent) {
                                                    case 0:
                                                        continue;
                                                    case 1:
                                                        combinedIdCurrent = 0;
                                                    default:
                                                        char combinedIdPrevious = previousLayer != null ? previousLayer[index] : 0;
                                                        if (combinedIdCurrent != combinedIdPrevious) {
                                                            synchronized (lock) {
                                                                add(xx, yy, zz, combinedIdPrevious, combinedIdCurrent);
                                                            }
                                                        }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // Tile changes
                            {
                                // Tiles created
                                Map<BytePair, CompoundTag> tiles = next.getTiles();
                                for (Map.Entry<BytePair, CompoundTag> entry : tiles.entrySet()) {
                                    synchronized (lock) {
                                        addTileCreate(entry.getValue());
                                    }
                                }
                                // Tiles removed
                                tiles = previous.getTiles();
                                for (Map.Entry<BytePair, CompoundTag> entry : tiles.entrySet()) {
                                    synchronized (lock) {
                                        addTileRemove(entry.getValue());
                                    }
                                }
                            }
                            // Entity changes
                            {
                                // Entities created
                                Set<CompoundTag> entities = next.getEntities();
                                for (CompoundTag entityTag : entities) {
                                    synchronized (lock) {
                                        addEntityCreate(entityTag);
                                    }
                                }
                                // Entities removed
                                entities = previous.getEntities();
                                for (CompoundTag entityTag : entities) {
                                    synchronized (lock) {
                                        addEntityRemove(entityTag);
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            MainUtil.handleError(e);
                        } finally {
                            if (waiting.decrementAndGet() <= 0) {
                                synchronized (lock) {
                                    lock.notifyAll();
                                }
                            }
                        }
                    }
                };
                if (mainThread) {
                    new Thread(run).start();
                } else {
                    TaskManager.IMP.async(run);
                }
            }
        });
    }
}

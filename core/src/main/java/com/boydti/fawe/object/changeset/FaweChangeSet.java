package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.inventory.BlockBag;
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

    private World world;
    private final String worldName;
    private final boolean mainThread;
    private final int layers;
    protected AtomicInteger waitingCombined = new AtomicInteger(0);
    protected AtomicInteger waitingAsync = new AtomicInteger(0);

    public static FaweChangeSet getDefaultChangeSet(World world, UUID uuid) {
        if (Settings.IMP.HISTORY.USE_DISK) {
            return new DiskStorageHistory(world, uuid);
        } else {
            return new MemoryOptimizedHistory(world);
        }
    }

    public FaweChangeSet(String world) {
        this.worldName = world;
        this.mainThread = (Fawe.get() != null) ? Fawe.isMainThread() : true;
        this.layers = FaweChunk.HEIGHT >> 4;
    }

    public FaweChangeSet(World world) {
        this.world = world;
        this.worldName = Fawe.imp().getWorldName(world);
        this.mainThread = Fawe.isMainThread();
        this.layers = (this.world.getMaxY() + 1) >> 4;
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        if (world == null && worldName != null) world = FaweAPI.getWorld(worldName);
        return world;
    }

    public boolean flushAsync() {
        waitingAsync.incrementAndGet();
        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {
                waitingAsync.decrementAndGet();
                synchronized (waitingAsync) {
                    waitingAsync.notifyAll();
                }
                flush();
            }
        });
        return true;
    }

    public boolean flush() {
        try {
            if (!Fawe.isMainThread()) {
                while (waitingAsync.get() > 0) {
                    synchronized (waitingAsync) {
                        waitingAsync.wait(1000);
                    }
                }
            }
            while (waitingCombined.get() > 0) {
                synchronized (waitingCombined) {
                    waitingCombined.wait(1000);
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
    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return getIterator(redo);
    }
    public abstract Iterator<Change> getIterator(boolean redo);

    public void delete() {};

    public EditSession toEditSession(FawePlayer player) {
        EditSession editSession = new EditSessionBuilder(getWorld()).player(player).autoQueue(false).fastmode(false).checkMemory(false).changeSet(this).limitUnlimited().allowedRegionsEverywhere().build();
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
                addTileCreate(nbt);
            }
            int combinedFrom = (from.getId() << 4) + from.getData();
            int combinedTo = (to.getId() << 4) + to.getData();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void add(int x, int y, int z, int combinedFrom, BaseBlock to) {
        try {
            if (to.hasNbtData()) {
                CompoundTag nbt = to.getNbtData();
                MainUtil.setPosition(nbt, x, y, z);
                addTileCreate(nbt);
            }
            int combinedTo = (to.getId() << 4) + to.getData();
            add(x, y, z, combinedFrom, combinedTo);

        } catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    public void addChangeTask(FaweQueue queue) {
        queue.setChangeTask(new RunnableVal2<FaweChunk, FaweChunk>() {
            @Override
            public void run(final FaweChunk previous, final FaweChunk next) {
                FaweChangeSet.this.waitingCombined.incrementAndGet();
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
                            // Current blocks
//                                char[][] currentIds = next.getCombinedIdArrays();
                            // Previous blocks in modified sections (i.e. we skip sections that weren't modified)
//                                char[][] previousIds = previous.getCombinedIdArrays();
                            for (int layer = 0; layer < layers; layer++) {
                                char[] currentLayer = next.getIdArray(layer);
                                char[] previousLayer = previous.getIdArray(layer);
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
                                                        synchronized (FaweChangeSet.this) {
                                                            add(xx, yy, zz, combinedIdPrevious, combinedIdCurrent);
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
                                Map<Short, CompoundTag> tiles = next.getTiles();
                                for (Map.Entry<Short, CompoundTag> entry : tiles.entrySet()) {
                                    synchronized (FaweChangeSet.this) {
                                        addTileCreate(entry.getValue());
                                    }
                                }
                                // Tiles removed
                                tiles = previous.getTiles();
                                for (Map.Entry<Short, CompoundTag> entry : tiles.entrySet()) {
                                    synchronized (FaweChangeSet.this) {
                                        addTileRemove(entry.getValue());
                                    }
                                }
                            }
                            // Entity changes
                            {
                                // Entities created
                                Set<CompoundTag> entities = next.getEntities();
                                for (CompoundTag entityTag : entities) {
                                    synchronized (FaweChangeSet.this) {
                                        addEntityCreate(entityTag);
                                    }
                                }
                                // Entities removed
                                entities = previous.getEntities();
                                for (CompoundTag entityTag : entities) {
                                    synchronized (FaweChangeSet.this) {
                                        addEntityRemove(entityTag);
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            MainUtil.handleError(e);
                        } finally {
                            if (FaweChangeSet.this.waitingCombined.decrementAndGet() <= 0) {
                                synchronized (FaweChangeSet.this.waitingAsync) {
                                    FaweChangeSet.this.waitingAsync.notifyAll();
                                }
                                synchronized (FaweChangeSet.this.waitingCombined) {
                                    FaweChangeSet.this.waitingCombined.notifyAll();
                                }
                            }
                        }
                    }
                };
                if (mainThread) {
                    run.run();
                } else {
                    TaskManager.IMP.async(run);
                }
            }
        });
    }
}
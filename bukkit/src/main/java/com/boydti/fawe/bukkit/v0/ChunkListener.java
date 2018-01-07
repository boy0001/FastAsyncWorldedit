package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import sun.misc.SharedSecrets;

public class ChunkListener implements Listener {

    private int rateLimit = 0;
    private int[] badLimit = new int[]{Settings.IMP.TICK_LIMITER.PHYSICS, Settings.IMP.TICK_LIMITER.FALLING, Settings.IMP.TICK_LIMITER.ITEMS};

    public ChunkListener() {
        if (Settings.IMP.TICK_LIMITER.ENABLED) {
            Bukkit.getPluginManager().registerEvents(ChunkListener.this, Fawe.<FaweBukkit>imp().getPlugin());
            TaskManager.IMP.repeat(new Runnable() {
                @Override
                public void run() {
                    physSkip = 0;
                    physCancelPair = Long.MIN_VALUE;
                    physCancel = false;
                }
            }, 1);
            TaskManager.IMP.repeat(new Runnable() {
                @Override
                public void run() {
                    rateLimit--;
                    physicsFreeze = false;
                    itemFreeze = false;
                    lastZ = Integer.MIN_VALUE;

                    counter.clear();
                    for (Long2ObjectMap.Entry<Boolean> entry : badChunks.long2ObjectEntrySet()) {
                        long key = entry.getLongKey();
                        int x = MathMan.unpairIntX(key);
                        int z = MathMan.unpairIntY(key);
                        counter.put(key, badLimit);
                    }
                    badChunks.clear();
                }
            }, Settings.IMP.TICK_LIMITER.INTERVAL);
        }
    }

    public static boolean physicsFreeze = false;
    public static boolean itemFreeze = false;

    private Long2ObjectOpenHashMap<Boolean> badChunks = new Long2ObjectOpenHashMap<>();
    private Long2ObjectOpenHashMap<int[]> counter = new Long2ObjectOpenHashMap<>();
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    private int[] lastCount;

    public int[] getCount(int cx, int cz) {
        if (lastX == cx && lastZ == cz) {
            return lastCount;
        }
        lastX = cx;
        lastZ = cz;
        long pair = MathMan.pairInt(cx, cz);
        int[] tmp = lastCount = counter.get(pair);
        if (tmp == null) {
            lastCount = tmp =  new int[3];
            counter.put(pair, tmp);
        }
        return tmp;
    }

    public void cleanup(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == EntityType.DROPPED_ITEM) {
                entity.remove();
            }
        }

    }

    private int physSkip;
    private boolean physCancel;
    private long physCancelPair;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (physCancel) {
            Block block = event.getBlock();
            long pair = MathMan.pairInt(block.getX() >> 4, block.getZ() >> 4);
            if (physCancelPair == pair) {
                event.setCancelled(true);
                return;
            }
            if (badChunks.containsKey(pair)) {
                physCancelPair = pair;
                event.setCancelled(true);
                return;
            }
        }
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        // For performance reasons, skip most checks
        if ((++physSkip & 2047) != 0) return;
        if (event.getChangedTypeId() == 0) return;
        Exception e = new Exception();
        int depth = SharedSecrets.getJavaLangAccess().getStackTraceDepth(e);
        if (depth >= Settings.IMP.TICK_LIMITER.PHYSICS) {
            for (int frame = 25; frame < 33; frame++) {
                StackTraceElement elem = SharedSecrets.getJavaLangAccess().getStackTraceElement(e, frame);
                String methodName = elem.getMethodName();
                // setAir (hacky, but this needs to be efficient)
                if (methodName.charAt(0) == 's' && methodName.length() == 6) {
                    Block block = event.getBlock();
                    int cx = block.getX() >> 4;
                    int cz = block.getZ() >> 4;
                    if (rateLimit <= 0) {
                        rateLimit = 20;
                        Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled physics  lag source at " + block.getLocation());
                    }
                    cancelNearby(cx, cz);
                    event.setCancelled(true);
                    physCancel = true;
                    return;
                }
            }
        }
        physSkip = 1;
        physCancel = false;
    }

    private void cancelNearby(int cx, int cz) {
        cancel(cx, cz);
        cancel(cx + 1, cz);
        cancel(cx - 1, cz);
        cancel(cx, cz + 1);
        cancel(cx, cz - 1);
    }

    private void cancel(int cx, int cz) {
        long key = MathMan.pairInt(cx, cz);
        badChunks.put(key, (Boolean) true);
        counter.put(key, badLimit);
    }

    // Falling
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        Block block = event.getBlock();
        int x = block.getX();
        int z = block.getZ();
        int cx = x >> 4;
        int cz = z >> 4;
        int[] count = getCount(cx, cz);
        if (count[1] >= Settings.IMP.TICK_LIMITER.FALLING) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            if (++count[1] >= Settings.IMP.TICK_LIMITER.FALLING) {

                // Only cancel falling blocks when it's lagging
                if (Fawe.get().getTimer().getTPS() < 18) {
                    cancelNearby(cx, cz);
                    if (rateLimit <= 0) {
                        rateLimit = 20;
                        Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled falling block lag source at " + block.getLocation());
                    }
                    event.setCancelled(true);
                    return;
                } else {
                    count[1] = 0;
                }
            }
        }
    }

    /**
     * Prevent FireWorks from loading chunks
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        Entity[] entities = chunk.getEntities();
        World world = chunk.getWorld();

        Exception e = new Exception();
        int start = 14;
        int end = 22;
        int depth = Math.min(end, SharedSecrets.getJavaLangAccess().getStackTraceDepth(e));

        for (int frame = start; frame < depth; frame++) {
            StackTraceElement elem = SharedSecrets.getJavaLangAccess().getStackTraceElement(e, frame);
            String fileName = elem.getFileName();
            if (fileName.charAt(0) == 'E' && fileName.equals("EntityFireworks.java")) {
                int chunkRange = 2;
                for (int ocx = -chunkRange; ocx <= chunkRange; ocx++) {
                    for (int ocz = -chunkRange; ocz <= chunkRange; ocz++) {
                        int cx = chunk.getX() + ocx;
                        int cz = chunk.getZ() + ocz;
                        if (world.isChunkLoaded(cx, cz)) {
                            Chunk relativeChunk = world.getChunkAt(cx, cz);
                            Entity[] ents = relativeChunk.getEntities();
                            for (Entity ent : ents) {
                                switch (ent.getType()) {
                                    case FIREWORK:
                                        Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled rogue FireWork at " + ent.getLocation());
                                        ent.remove();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        Location loc = event.getLocation();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        int[] count = getCount(cx, cz);
        if (count[2] >= Settings.IMP.TICK_LIMITER.ITEMS) {
            event.setCancelled(true);
            return;
        }
        if (++count[2] >= Settings.IMP.TICK_LIMITER.ITEMS) {
            cleanup(loc.getChunk());
            cancelNearby(cx, cz);
            if (rateLimit <= 0) {
                rateLimit = 20;
                Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled item lag source at " + loc);
            }
            event.setCancelled(true);
            return;
        }
    }
}
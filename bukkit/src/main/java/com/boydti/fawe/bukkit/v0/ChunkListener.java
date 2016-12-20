package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ChunkListener implements Listener {

    int rateLimit = 0;

    public ChunkListener() {
        if (Settings.TICK_LIMITER.ENABLED) {
            Bukkit.getPluginManager().registerEvents(ChunkListener.this, Fawe.<FaweBukkit>imp().getPlugin());
            TaskManager.IMP.repeat(new Runnable() {
                @Override
                public void run() {
                    rateLimit--;
                    physicsFreeze = false;
                    itemFreeze = false;
                    counter.clear();
                    lastZ = Integer.MIN_VALUE;
                    for (Long badChunk : badChunks) {
                        counter.put(badChunk, new IntegerTrio(Settings.TICK_LIMITER.PHYSICS, Settings.TICK_LIMITER.ITEMS, Settings.TICK_LIMITER.FALLING));
                    }
                    badChunks.clear();
                }
            }, Settings.TICK_LIMITER.INTERVAL);
        }
    }

    public static boolean physicsFreeze = false;
    public static boolean itemFreeze = false;

    private HashSet<Long> badChunks = new HashSet<>();
    private HashMap<Long, IntegerTrio> counter = new HashMap<>();
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    private IntegerTrio lastCount;

    public IntegerTrio getCount(int cx, int cz) {
        if (lastX == cx && lastZ == cz) {
            return lastCount;
        }
        lastX = cx;
        lastZ = cz;
        long pair = MathMan.pairInt(cx, cz);
        lastCount = counter.get(pair);
        if (lastCount == null) {
            lastCount = new IntegerTrio();
            counter.put(pair, lastCount);
        }
        return lastCount;
    }

    public void cleanup(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == EntityType.DROPPED_ITEM) {
                entity.remove();
            }
        }

    }

    private int lastPhysY = 0;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPhysics(BlockPhysicsEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        Block block = event.getBlock();
        int x = block.getX();
        int z = block.getZ();
        int cx = x >> 4;
        int cz = z >> 4;
        IntegerTrio count = getCount(cx, cz);
        if (count.x >= Settings.TICK_LIMITER.PHYSICS) {
            event.setCancelled(true);
            return;
        }
        if (event.getChangedType() == block.getType()) {
            int y = block.getY();
            if (y != lastPhysY) {
                lastPhysY = y;
                if (++count.x == Settings.TICK_LIMITER.PHYSICS) {
                    badChunks.add(MathMan.pairInt(cx, cz));
                    if (rateLimit <= 0) {
                        rateLimit = 120;
                        Fawe.debug("[Tick Limiter] Detected and cancelled physics  lag source at " + block.getLocation());
                    }
                }
                return;
            }
            lastPhysY = y;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        Material to = event.getTo();
        if (to == Material.AIR) {
            Block block = event.getBlock();
            int x = block.getX();
            int z = block.getZ();
            int cx = x >> 4;
            int cz = z >> 4;
            IntegerTrio count = getCount(cx, cz);
            if (++count.y >= Settings.TICK_LIMITER.FALLING) {
                if (count.y == Settings.TICK_LIMITER.FALLING) {
                    count.x = Settings.TICK_LIMITER.PHYSICS;
                    badChunks.add(MathMan.pairInt(cx, cz));
                    Fawe.debug("[Tick Limiter] Detected and cancelled falling block lag source at " + block.getLocation());
                }
                event.setCancelled(true);
                return;
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
        IntegerTrio count = getCount(cx, cz);
        if (++count.z >= Settings.TICK_LIMITER.ITEMS) {
            if (count.z == Settings.TICK_LIMITER.ITEMS) {
                count.x = Settings.TICK_LIMITER.PHYSICS;
                cleanup(loc.getChunk());
                badChunks.add(MathMan.pairInt(cx, cz));
                if (rateLimit <= 0) {
                    rateLimit = 120;
                    Fawe.debug("[Tick Limiter] Detected and cancelled item lag source at " + loc);
                }
            }
            event.setCancelled(true);
            return;
        }
    }
}
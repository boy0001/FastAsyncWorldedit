package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ChunkListener implements Listener {

    public ChunkListener() {
        if (Settings.TICK_LIMITER.ENABLED) {
            Bukkit.getPluginManager().registerEvents(ChunkListener.this, Fawe.<FaweBukkit>imp().getPlugin());
            TaskManager.IMP.repeat(new Runnable() {
                @Override
                public void run() {
                    physicsFreeze = false;
                    itemFreeze = false;
                    counter.clear();
                    lastZ = Integer.MIN_VALUE;
                    for (Long badChunk : badChunks) {
                        counter.put(badChunk, new IntegerPair(Settings.TICK_LIMITER.PHYSICS, Settings.TICK_LIMITER.ITEMS));
                    }
                    badChunks.clear();
                }
            }, 1);
        }
    }

    public static boolean physicsFreeze = false;
    public static boolean itemFreeze = false;

    private HashSet<Long> badChunks = new HashSet<>();
    private HashMap<Long, IntegerPair> counter = new HashMap<>();
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    private IntegerPair lastCount;

    public IntegerPair getCount(int cx, int cz) {
        if (lastX == cx && lastZ == cz) {
            return lastCount;
        }
        lastX = cx;
        lastZ = cz;
        long pair = MathMan.pairInt(cx, cz);
        lastCount = counter.get(pair);
        if (lastCount == null) {
            lastCount = new IntegerPair(0,0);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPhysics(BlockPhysicsEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        int id = event.getChangedTypeId();
        switch (id) {
            case 23: // dispensor
            case 158: // dropper
            case 25:
                // piston
            case 29:
            case 33:
                // tnt
            case 44:
                // wire
            case 55:
                // door
            case 96: // trapdoor
            case 167:
            case 107: // fence
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 64: // door
            case 71:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
                // diode
            case 93:
            case 94:
                // torch
            case 75:
            case 76:
                // comparator
            case 149:
            case 150:
                // lamp
            case 123:
            case 124:
                // rail
            case 27:
                // BUD
            case 73: // ore
            case 74:
            case 8: // water
            case 9:
            case 34: // piston
                return;
        }
        Block block = event.getBlock();
        int cx = block.getX() >> 4;
        int cz = block.getZ() >> 4;
        IntegerPair count = getCount(cx, cz);
        if (++count.x >= Settings.TICK_LIMITER.PHYSICS) {
            if (count.x == Settings.TICK_LIMITER.PHYSICS) {
                badChunks.add(MathMan.pairInt(cx, cz));
                Fawe.debug("[Tick Limiter] Detected and cancelled lag source at " + block.getLocation());
            }
            event.setCancelled(true);
            return;
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
        IntegerPair count = getCount(cx, cz);
        if (++count.z >= Settings.TICK_LIMITER.ITEMS) {
            if (count.z == Settings.TICK_LIMITER.ITEMS) {
                cleanup(loc.getChunk());
                badChunks.add(MathMan.pairInt(cx, cz));
                Fawe.debug("[Tick Limiter] Detected and cancelled lag source at " + loc);
            }
            event.setCancelled(true);
            return;
        }
    }
}

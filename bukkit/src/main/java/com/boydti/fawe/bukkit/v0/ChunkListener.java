package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.blocks.BlockID;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ChunkListener implements Listener {

    private int rateLimit = 0;
    private int[] badLimit = new int[]{Settings.IMP.TICK_LIMITER.PHYSICS, Settings.IMP.TICK_LIMITER.FALLING, Settings.IMP.TICK_LIMITER.ITEMS};

    public ChunkListener() {
        if (Settings.IMP.TICK_LIMITER.ENABLED) {
            Bukkit.getPluginManager().registerEvents(ChunkListener.this, Fawe.<FaweBukkit>imp().getPlugin());
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
        int[] count = getCount(cx, cz);
        if (count[0] >= Settings.IMP.TICK_LIMITER.PHYSICS) {
            event.setCancelled(true);
            return;
        }
        int blockId = block.getTypeId();
        if (event.getChangedTypeId() == blockId) {
            int y = block.getY();
            int tmpLastY = lastPhysY;
            lastPhysY = y;
            int amount;
            switch (blockId) {
                case BlockID.REDSTONE_BLOCK:
                case BlockID.REDSTONE_LAMP_OFF:
                case BlockID.REDSTONE_LAMP_ON:
                case BlockID.REDSTONE_ORE:
                case BlockID.REDSTONE_REPEATER_OFF:
                case BlockID.REDSTONE_REPEATER_ON:
                case BlockID.REDSTONE_TORCH_OFF:
                case BlockID.REDSTONE_TORCH_ON:
                case BlockID.REDSTONE_WIRE:
                case BlockID.GLOWING_REDSTONE_ORE:
                case BlockID.TRIPWIRE:
                case BlockID.TRIPWIRE_HOOK:
                case 218: // Observer
                case BlockID.PISTON_BASE:
                case BlockID.PISTON_STICKY_BASE:
                case BlockID.IRON_DOOR:
                case BlockID.ACACIA_DOOR:
                case BlockID.BIRCH_DOOR:
                case BlockID.DARK_OAK_DOOR:
                case BlockID.IRON_TRAP_DOOR:
                case BlockID.JUNGLE_DOOR:
                case BlockID.SPRUCE_DOOR:
                case BlockID.TRAP_DOOR:
                case BlockID.WOODEN_DOOR:
                case BlockID.FENCE_GATE:
                case BlockID.ACACIA_FENCE_GATE:
                case BlockID.BIRCH_FENCE_GATE:
                case BlockID.DARK_OAK_FENCE_GATE:
                case BlockID.JUNGLE_FENCE_GATE:
                case BlockID.SPRUCE_FENCE_GATE:
                case BlockID.LEVER:
                case BlockID.WOODEN_BUTTON:
                case BlockID.STONE_BUTTON:
                case BlockID.STONE_PRESSURE_PLATE:
                case BlockID.WOODEN_PRESSURE_PLATE:
                case BlockID.PRESSURE_PLATE_HEAVY:
                case BlockID.PRESSURE_PLATE_LIGHT:
                case BlockID.POWERED_RAIL:
                case BlockID.ACTIVATOR_RAIL:
                case BlockID.DETECTOR_RAIL:
                case BlockID.WATER:
                case BlockID.STATIONARY_WATER:
                case BlockID.LAVA:
                case BlockID.STATIONARY_LAVA:
                    if (y == tmpLastY) {
                        return;
                    }
                    // Should cancel if excess, but need to be careful
                    amount = 1;
                    break;
                case BlockID.SAND:
                case BlockID.GRAVEL:
                case BlockID.DRAGON_EGG:
                case BlockID.ANVIL:
                case BlockID.FIRE:
                case BlockID.TORCH:
                    // If there's lots of this, it's usually from abuse
                    amount = 16;
                    break;
                default:
                    // Uncategorized, but not redstone
                    amount = 4;
                    break;
            }
            if ((count[0] += amount) >= Settings.IMP.TICK_LIMITER.PHYSICS) {
                cancelNearby(cx, cz);
                if (rateLimit <= 0) {
                    rateLimit = 20;
                    Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled physics  lag source at " + block.getLocation());
                }
                event.setCancelled(true);
            }
        }
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
                cancelNearby(cx, cz);
                if (rateLimit <= 0) {
                    rateLimit = 20;
                    Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled falling block lag source at " + block.getLocation());
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
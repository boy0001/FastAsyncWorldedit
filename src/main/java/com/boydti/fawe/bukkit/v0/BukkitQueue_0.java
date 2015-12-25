package com.boydti.fawe.bukkit.v0;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.ChunkLoc;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.SetBlockQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.world.biome.BaseBiome;

public abstract class BukkitQueue_0 extends FaweQueue implements Listener {
    
    private final HashMap<ChunkLoc, FaweChunk<Chunk>> toLight = new HashMap<>();
    
    public BukkitQueue_0() {
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().registerEvents(BukkitQueue_0.this, (Plugin) Fawe.imp());
            }
        });
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location loc = event.getTo();
        if (!loc.getChunk().equals(event.getFrom().getChunk())) {
            Chunk chunk = loc.getChunk();
            ChunkLoc cl = new ChunkLoc(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        }
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (toLight.size() == 0) {
            return;
        }
        Chunk chunk = event.getChunk();
        ChunkLoc loc = new ChunkLoc(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                ChunkLoc a = new ChunkLoc(loc.world, loc.x + x, loc.z + z);
                if (toLight.containsKey(a)) {
                    if (fixLighting(toLight.get(a), Settings.FIX_ALL_LIGHTING)) {
                        toLight.remove(a);
                        return;
                    }
                }
            }
        }
    }

    private final ConcurrentHashMap<ChunkLoc, FaweChunk<Chunk>> blocks = new ConcurrentHashMap<>();
    
    @Override
    public boolean setBlock(final String world, int x, final int y, int z, final short id, final byte data) {
        if ((y > 255) || (y < 0)) {
            return false;
        }
        final ChunkLoc wrap = new ChunkLoc(world, x >> 4, z >> 4);
        x = x & 15;
        z = z & 15;
        FaweChunk<Chunk> result = blocks.get(wrap);
        if (result == null) {
            result = getChunk(wrap);
            result.setBlock(x, y, z, id, data);
            final FaweChunk<Chunk> previous = blocks.put(wrap, result);
            if (previous == null) {
                return true;
            }
            blocks.put(wrap, previous);
            result = previous;
        }
        result.setBlock(x, y, z, id, data);
        return true;
    }
    
    @Override
    public boolean setBiome(String world, int x, int z, BaseBiome biome) {
        final ChunkLoc wrap = new ChunkLoc(world, x >> 4, z >> 4);
        x = x & 15;
        z = z & 15;
        FaweChunk<Chunk> result = blocks.get(wrap);
        if (result == null) {
            result = getChunk(wrap);
            final FaweChunk<Chunk> previous = blocks.put(wrap, result);
            if (previous != null) {
                blocks.put(wrap, previous);
                result = previous;
            }
        }
        result.setBiome(x, z, biome);
        return true;
    }
    
    @Override
    public FaweChunk<Chunk> next() {
        try {
            if (blocks.size() == 0) {
                return null;
            }
            final Iterator<Entry<ChunkLoc, FaweChunk<Chunk>>> iter = blocks.entrySet().iterator();
            final FaweChunk<Chunk> toReturn = iter.next().getValue();
            if (SetBlockQueue.IMP.isWaiting()) {
                return null;
            }
            iter.remove();
            execute(toReturn);
            return toReturn;
        } catch (final Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private final ArrayDeque<FaweChunk<Chunk>> toUpdate = new ArrayDeque<>();
    
    public boolean execute(final FaweChunk<Chunk> fc) {
        if (fc == null) {
            return false;
        }
        // Load chunk
        final Chunk chunk = fc.getChunk();
        chunk.load(true);
        // Set blocks / entities / biome
        if (!setComponents(fc)) {
            return false;
        }
        toUpdate.add(fc);
        // Fix lighting
        SetBlockQueue.IMP.addTask(new Runnable() {
            
            @Override
            public void run() {
                if (toUpdate.size() == 0) {
                    return;
                }
                for (FaweChunk<Chunk> fc : sendChunk(toUpdate)) {
                    toLight.put(fc.getChunkLoc(), fc);
                }
                toUpdate.clear();
            }
        });
        return true;
    }
    
    @Override
    public void clear() {
        blocks.clear();
    }
    
    @Override
    public void setChunk(FaweChunk<?> chunk) {
        blocks.put(chunk.getChunkLoc(), (FaweChunk<Chunk>) chunk);
    }

    public abstract Collection<FaweChunk<Chunk>> sendChunk(final Collection<FaweChunk<Chunk>> fcs);
    
    public abstract boolean setComponents(final FaweChunk<Chunk> fc);
    
    @Override
    public abstract FaweChunk<Chunk> getChunk(final ChunkLoc wrap);
    
    @Override
    public abstract boolean fixLighting(FaweChunk<?> fc, boolean fixAll);
}

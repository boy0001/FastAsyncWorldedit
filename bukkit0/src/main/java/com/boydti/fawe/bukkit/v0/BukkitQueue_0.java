package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.FaweChunk;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

public abstract class BukkitQueue_0<CHUNK, CHUNKSECTIONS, SECTION> extends NMSMappedFaweQueue<World, CHUNK, CHUNKSECTIONS, SECTION> {

    public Object adapter;
    public Method methodToNative;
    public Method methodFromNative;

    public BukkitQueue_0(final String world) {
        super(world);
        setupAdapter(null);
    }

    public void checkVersion(String supported) {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        if (!version.contains(supported)) {
            Fawe.debug("This version of FAWE is for: " + supported);
            throw new IllegalStateException("Unsupported version: " + version + " (supports: " + supported + ")");
        }
    }

    public void setupAdapter(BukkitImplAdapter adapter) {
        try {
            WorldEditPlugin instance = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
            Field fieldAdapter = WorldEditPlugin.class.getDeclaredField("bukkitAdapter");
            fieldAdapter.setAccessible(true);
            if ((this.adapter = adapter) != null) {
                fieldAdapter.set(instance, adapter);
            } else {
                this.adapter = fieldAdapter.get(instance);
            }
            for (Method method : this.adapter.getClass().getDeclaredMethods()) {
                switch (method.getName()) {
                    case "toNative":
                        methodToNative = method;
                        methodToNative.setAccessible(true);
                        break;
                    case "fromNative":
                        methodFromNative = method;
                        methodFromNative.setAccessible(true);
                        break;
                }
            }
        } catch (Throwable e) {
            Fawe.debug("====== NO NATIVE WORLDEDIT ADAPTER ======");
            Fawe.debug("Try updating WorldEdit: ");
            Fawe.debug(" - http://builds.enginehub.org/job/worldedit?branch=master");
            Fawe.debug("See also: http://wiki.sk89q.com/wiki/WorldEdit/Bukkit_adapters");
            Fawe.debug("=========================================");
        }
    }

    @Override
    public World getWorld(String world) {
        return Bukkit.getWorld(world);
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public void refreshChunk(World world, CHUNK chunk) {
        return;
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z) {
        return world.regenerateChunk(x, z);
    }


    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, CHUNKSECTIONS sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception {
        return fs;
    }

    @Override
    public boolean fixLighting(FaweChunk fc, RelightMode mode) {
        // Not implemented
        return true;
    }

    @Override
    public boolean loadChunk(World impWorld, int x, int z, boolean generate) {
        return impWorld.loadChunk(x, z, generate);
    }

    private volatile boolean timingsEnabled;

    @Override
    public void startSet(boolean parallel) {
        ChunkListener.physicsFreeze = true;
        if (parallel) {
            try {
                Field fieldEnabled = Class.forName("co.aikar.timings.Timings").getDeclaredField("timingsEnabled");
                fieldEnabled.setAccessible(true);
                timingsEnabled = (boolean) fieldEnabled.get(null);
                if (timingsEnabled) {
                    fieldEnabled.set(null, false);
                    Method methodCheck = Class.forName("co.aikar.timings.TimingsManager").getDeclaredMethod("recheckEnabled");
                    methodCheck.setAccessible(true);
                    methodCheck.invoke(null);
                }
            } catch (Throwable ignore) {}
            try { Class.forName("org.spigotmc.AsyncCatcher").getField("enabled").set(null, false); } catch (Throwable ignore) {}
        }
    }

    @Override
    public void endSet(boolean parallel) {
        ChunkListener.physicsFreeze = false;
        if (parallel) {
            try {Field fieldEnabled = Class.forName("co.aikar.timings.Timings").getDeclaredField("timingsEnabled");fieldEnabled.setAccessible(true);fieldEnabled.set(null, timingsEnabled);
            } catch (Throwable ignore) {}
            try { Class.forName("org.spigotmc.AsyncCatcher").getField("enabled").set(null, true); } catch (Throwable ignore) {}
        }
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return new CharFaweChunk<Chunk>(this, x, z) {
            @Override
            public Chunk getNewChunk() {
                return BukkitQueue_0.this.getWorld().getChunkAt(getX(), getZ());
            }
        };
    }
}

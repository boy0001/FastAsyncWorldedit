package com.boydti.fawe.nukkit.optimization;

import cn.nukkit.Player;
import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.nukkit.core.NukkitTaskManager;
import com.boydti.fawe.nukkit.core.NukkitWorldEdit;
import com.boydti.fawe.nukkit.optimization.queue.NukkitQueue;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

public class FaweNukkit implements IFawe {

    private final NukkitWorldEdit plugin;

    public FaweNukkit(NukkitWorldEdit mod) {
        this.plugin = mod;
        FaweChunk.HEIGHT = 128;
    }

    @Override
    public void debug(String s) {
        plugin.getWELogger().log(Level.INFO, s);
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public void setupCommand(String label, final FaweCommand cmd) {
        plugin.getServer().getCommandMap().register(label, new NukkitCommand(label, cmd));
    }

    @Override
    public FawePlayer wrap(Object obj) {
        if (obj.getClass() == String.class) {
            String name = (String) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(name);
            if (existing != null) {
                return existing;
            }
            return new FaweNukkitPlayer(getPlugin().getServer().getPlayer(name));
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(player.getName());
            return existing != null ? existing : new FaweNukkitPlayer(player);
        } else {
            return null;
        }
    }

    public NukkitWorldEdit getPlugin() {
        return plugin;
    }

    @Override
    public void setupVault() {

    }

    @Override
    public TaskManager getTaskManager() {
        return new NukkitTaskManager(plugin);
    }

    @Override
    public FaweQueue getNewQueue(String world, boolean fast) {
        return new NukkitQueue(this, world);
    }

    @Override
    public String getWorldName(World world) {
        return world.getName();
    }

    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        return new ArrayList<>();
    }

    @Override
    public void startMetrics() {
        Metrics metrics = new Metrics(plugin);
        metrics.start();
    }

    @Override
    public String getPlatform() {
        return "Nukkit-" + plugin.getServer().getNukkitVersion();
    }

    @Override
    public UUID getUUID(String name) {
        try {
            return UUID.fromString(name);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getName(UUID uuid) {
        return uuid.toString();
    }

    @Override
    public Object getBlocksHubApi() {
        return null;
    }
}

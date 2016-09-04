package com.boydti.fawe.nukkit;

import com.boydti.fawe.IFawe;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.Collection;
import java.util.UUID;

public class FaweNukkit implements IFawe {
    @Override
    public void debug(String s) {

    }

    @Override
    public File getDirectory() {
        return null;
    }

    @Override
    public void setupCommand(String label, FaweCommand cmd) {

    }

    @Override
    public FawePlayer wrap(Object obj) {
        return null;
    }

    @Override
    public void setupVault() {

    }

    @Override
    public TaskManager getTaskManager() {
        return null;
    }

    @Override
    public FaweQueue getNewQueue(String world, boolean fast) {
        return null;
    }

    @Override
    public String getWorldName(World world) {
        return null;
    }

    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        return null;
    }

    @Override
    public void startMetrics() {

    }

    @Override
    public String getPlatform() {
        return null;
    }

    @Override
    public UUID getUUID(String name) {
        return null;
    }

    @Override
    public String getName(UUID uuid) {
        return null;
    }

    @Override
    public Object getBlocksHubApi() {
        return null;
    }
}

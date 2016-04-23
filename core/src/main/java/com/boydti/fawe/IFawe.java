package com.boydti.fawe;

import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface IFawe {
    public void debug(final String s);

    public File getDirectory();

    public void setupCommand(final String label, final FaweCommand cmd);

    public FawePlayer wrap(final Object obj);

    public void setupWEListener();

    public void setupVault();

    public TaskManager getTaskManager();

    public int[] getVersion();

    public FaweQueue getNewQueue(String world);

    public String getWorldName(World world);

    public EditSessionWrapper getEditSessionWrapper(final EditSession session);

    public Collection<FaweMaskManager> getMaskManagers();

    public void startMetrics();

    public String getPlatform();

    public UUID getUUID(String name);

    public String getName(UUID uuid);
}

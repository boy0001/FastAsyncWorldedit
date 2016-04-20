package com.boydti.fawe;

import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import java.io.File;
import java.util.Collection;
import java.util.Set;

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

    public EditSessionWrapper getEditSessionWrapper(final EditSession session);

    public Collection<FaweMaskManager> getMaskManagers();

    public void startMetrics();

    public Set<FawePlayer> getPlayers();

    public String getPlatform();
}

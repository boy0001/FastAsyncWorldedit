package com.boydti.fawe;

import java.io.File;
import java.util.Collection;

import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;

public interface IFawe {
    public void debug(final String s);

    public File getDirectory();

    public void setupCommand(final String label, final FaweCommand cmd);

    public FawePlayer wrap(final Object obj);

    public void setupWEListener();

    public void setupVault();

    public TaskManager getTaskManager();

    public int[] getVersion();

    public FaweQueue getQueue();

    public EditSessionWrapper getEditSessionWrapper(final EditSession session);

    public Collection<FaweMaskManager> getMaskManagers();

    public void startMetrics();
}

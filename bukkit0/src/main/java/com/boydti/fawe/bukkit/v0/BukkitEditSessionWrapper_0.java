package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.bukkit.logging.BlocksHubHook;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.util.FaweQueue;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;

public class BukkitEditSessionWrapper_0 extends EditSessionWrapper {

    private BlocksHubHook hook;

    public BukkitEditSessionWrapper_0(final EditSession session) {
        super(session);
        try {
            // Try to hook into BlocksHub
            this.hook = new BlocksHubHook();
        } catch (final Throwable e) {}
    }

    @Override
    public FaweChangeSet wrapChangeSet(EditSession session, FaweLimit limit, Extent parent, FaweChangeSet set, FaweQueue queue, FawePlayer<?> player) {
        if (this.hook != null) {
            // If we are doing logging, use a custom logging ChangeSet
            return hook.getLoggingChangeSet(session, limit, parent, set, queue, player);
        }
        return set;
    }
}

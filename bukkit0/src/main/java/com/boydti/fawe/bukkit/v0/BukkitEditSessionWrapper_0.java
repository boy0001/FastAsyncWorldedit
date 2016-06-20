package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.bukkit.logging.BlocksHubHook;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.worldedit.EditSession;

public class BukkitEditSessionWrapper_0 extends EditSessionWrapper {

    private BlocksHubHook hook;

    public BukkitEditSessionWrapper_0(final EditSession session) {
        super(session);
        try {
            // Try to hook into BlocksHub
            this.hook = new BlocksHubHook();
        } catch (final Throwable ignore) {}
    }

    @Override
    public FaweChangeSet wrapChangeSet(FaweChangeSet set, FawePlayer<?> player) {
        if (this.hook != null) {
            // If we are doing logging, use a custom logging ChangeSet
            return hook.getLoggingChangeSet(set, player);
        }
        return set;
    }
}

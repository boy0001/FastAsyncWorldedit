package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.bukkit.logging.BlocksHubHook;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.FaweChangeSet;
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
    public Extent getHistoryExtent(final Extent parent, final FaweChangeSet set, final FawePlayer<?> player) {
        if (this.hook != null) {
            // If we are doing logging, return a custom logging extent
            return this.hook.getLoggingExtent(parent, set, player);
        }
        // Otherwise return the normal history extent
        return super.getHistoryExtent(parent, set, player);
    }

}

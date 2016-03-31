package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.logging.BlocksHubHook;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.changeset.ChangeSet;

public class BukkitEditSessionWrapper_0 extends EditSessionWrapper {

    private BlocksHubHook hook;

    public BukkitEditSessionWrapper_0(final EditSession session) {
        super(session);
        try {
            this.hook = new BlocksHubHook();
        } catch (final Throwable e) {

        }
    }

    @Override
    public Extent getHistoryExtent(final Extent parent, final ChangeSet set, final FawePlayer<?> player) {
        if (this.hook != null) {
            return this.hook.getLoggingExtent(parent, set, player);
        }
        return super.getHistoryExtent(parent, set, player);
    }

}

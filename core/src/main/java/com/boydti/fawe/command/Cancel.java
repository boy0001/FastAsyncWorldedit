package com.boydti.fawe.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.extent.FaweExtent;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.world.World;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Cancel extends FaweCommand {

    public Cancel() {
        super("fawe.cancel", false);
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUUID();
        List<FaweQueue> queues = SetQueue.IMP.getAllQueues();
        int cancelled = 0;
        for (FaweQueue queue : queues) {
            Set<EditSession> sessions = queue.getEditSessions();
            for (EditSession session : sessions) {
                Actor actor = session.actor;
                if (actor == null) {
                    continue;
                }
                if (uuid.equals(actor.getUniqueId())) {
                    // Cancel this
                    FaweExtent fe = session.getFaweExtent();
                    if (fe != null) {
                        cancelled++;
                        try {
                            WEManager.IMP.cancelEdit(fe, BBC.WORLDEDIT_CANCEL_REASON_MANUAL);
                        } catch (Throwable ignore) {}
                        World world = session.getWorld();
                        NullExtent nullExtent = new NullExtent(world, BBC.WORLDEDIT_CANCEL_REASON_MANUAL);
                        session.bypassHistory = nullExtent;
                        session.bypassNone = nullExtent;
                        session.bypassReorderHistory = nullExtent;
                        session.faweExtent = nullExtent;
                        queue.clear();
                    }
                }
            }
        }
        BBC.WORLDEDIT_CANCEL_COUNT.send(player,cancelled);
        return true;
    }

}

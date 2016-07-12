package com.boydti.fawe.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.worldedit.EditSession;
import java.util.Collection;
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
        Collection<FaweQueue> queues = SetQueue.IMP.getAllQueues();
        int cancelled = 0;
        for (FaweQueue queue : queues) {
            Set<EditSession> sessions = queue.getEditSessions();
            for (EditSession session : sessions) {
                FawePlayer currentPlayer = session.getPlayer();
                if (currentPlayer == player) {
                    if (session.cancel()) {
                        cancelled++;
                    }
                }
            }
        }
        BBC.WORLDEDIT_CANCEL_COUNT.send(player,cancelled);
        return true;
    }

}

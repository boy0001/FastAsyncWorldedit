package com.boydti.fawe.object;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.TaskManager;

public abstract class FaweCommand<T> {
    public final String perm;

    public FaweCommand(final String perm) {
        this.perm = perm;
    }

    public String getPerm() {
        return this.perm;
    }

    public boolean executeSafe(final FawePlayer<T> player, final String... args) {
        if (player == null) {
            execute(player, args);
            return true;
        } else {
            if (player.getMeta("fawe_action") != null) {
                BBC.WORLDEDIT_COMMAND_LIMIT.send(player);
                return true;
            }
            player.setMeta("fawe_action", true);
            TaskManager.IMP.async(new Runnable() {
                @Override
                public void run() {
                    execute(player, args);
                    player.deleteMeta("fawe_action");
                }
            });
        }
        return true;
    }

    public abstract boolean execute(final FawePlayer<T> player, final String... args);
}

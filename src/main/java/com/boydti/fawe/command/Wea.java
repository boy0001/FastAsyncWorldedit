package com.boydti.fawe.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;

public class Wea extends FaweCommand {
    
    public Wea() {
        super("fawe.admin");
    }
    
    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        if (player == null) {
            return false;
        }
        if (toggle(player)) {
            BBC.WORLDEDIT_BYPASSED.send(player);
        } else {
            BBC.WORLDEDIT_RESTRICTED.send(player);
        }
        return true;
    }
    
    private boolean toggle(FawePlayer player) {
        if (player.hasPermission("fawe.bypass")) {
            player.setPermission("fawe.bypass", false);
            return false;
        } else {
            player.setPermission("fawe.bypass", true);
            return true;
        }
    }
}

package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;

public class Reload extends FaweCommand {

    public Reload() {
        super("fawe.reload");
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        Fawe.get().setupConfigs();
        player.sendMessage("&d[FAWE] Reloaded configuration");
        return true;
    }
}

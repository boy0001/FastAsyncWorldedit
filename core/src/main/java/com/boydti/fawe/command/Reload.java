package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MainUtil;

public class Reload extends FaweCommand {

    public Reload() {
        super("fawe.reload");
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        Fawe.get().setupConfigs();
        MainUtil.sendMessage(player, "Reloaded configuration");
        return true;
    }
}

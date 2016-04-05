package com.boydti.fawe.command;

import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;

public class Undolist extends FaweCommand {

    public Undolist() {
        super("fawe.undolist");
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        return false;
    }
}

package com.boydti.fawe.command;

import java.io.File;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;

public class Stream extends FaweCommand {

    public Stream() {
        super("fawe.stream");
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        if (player == null) {
            return false;
        }
        if (args.length != 1) {
            BBC.COMMAND_SYNTAX.send(player, "/stream <file>");
            return false;
        }
        if (!args[0].endsWith(".schematic")) {
            args[0] += ".schematic";
        }
        final File file = Fawe.get().getWorldEdit().getWorkingDirectoryFile(Fawe.get().getWorldEdit().getConfiguration().saveDir + File.separator + args[0]);
        if (!file.exists()) {
            BBC.SCHEMATIC_NOT_FOUND.send(player, args);
            return false;
        }
        FaweAPI.streamSchematicAsync(file, player.getLocation());
        BBC.SCHEMATIC_PASTING.send(player);
        return true;
    }
}

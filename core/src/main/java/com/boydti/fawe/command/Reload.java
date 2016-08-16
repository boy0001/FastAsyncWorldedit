package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweVersion;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MainUtil;
import java.util.Date;

public class Reload extends FaweCommand {

    public Reload() {
        super("fawe.reload");
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        if (args.length != 1) {
            BBC.COMMAND_SYNTAX.send(player, "/fawe [reload|version");
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "version": {
                FaweVersion version = Fawe.get().getVersion();
                if (version == null) {
                    MainUtil.sendMessage(player, "No version information available.");
                    return false;
                }
                MainUtil.sendMessage(player, "Version Date: " + new Date(version.year, version.month, version.day).toLocaleString());
                MainUtil.sendMessage(player, "Version Commit: " + Integer.toHexString(version.hash));
                MainUtil.sendMessage(player, "Version Build: #" + version.build);
                return true;
            }
            case "reload": {
                Fawe.get().setupConfigs();
                MainUtil.sendMessage(player, "Reloaded (" + Fawe.get().getVersion() + ").");
                return true;
            }
            default:
                BBC.COMMAND_SYNTAX.send(player, "/fawe [reload|version]");
                return false;
        }
    }
}

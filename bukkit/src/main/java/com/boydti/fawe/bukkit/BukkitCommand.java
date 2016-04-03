package com.boydti.fawe.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;

public class BukkitCommand implements CommandExecutor {

    private final FaweCommand cmd;

    public BukkitCommand(final FaweCommand cmd) {
        this.cmd = cmd;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        final FawePlayer plr = Fawe.imp().wrap(sender);
        if (!sender.hasPermission(this.cmd.getPerm()) || sender.isOp()) {
            BBC.NO_PERM.send(plr, this.cmd.getPerm());
            return true;
        }
        this.cmd.execute(plr, args);
        return true;
    }

}

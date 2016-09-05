package com.boydti.fawe.nukkit.optimization;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;

public class NukkitCommand extends Command {

    private final FaweCommand cmd;

    public NukkitCommand(String lavel, final FaweCommand cmd) {
        super(lavel);
        this.cmd = cmd;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        final FawePlayer plr = Fawe.imp().wrap(sender);
        if (!sender.hasPermission(this.cmd.getPerm()) && !sender.isOp()) {
            BBC.NO_PERM.send(plr, this.cmd.getPerm());
            return true;
        }
        this.cmd.executeSafe(plr, args);
        return true;
    }
}

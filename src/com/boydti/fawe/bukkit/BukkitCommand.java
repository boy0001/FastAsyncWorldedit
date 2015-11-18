package com.boydti.fawe.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweCommand;

public class BukkitCommand implements CommandExecutor {
    
    private final FaweCommand cmd;
    
    public BukkitCommand(final FaweCommand cmd) {
        this.cmd = cmd;
    }
    
    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        this.cmd.execute(Fawe.imp().wrap(sender), args);
        return true;
    }
    
}

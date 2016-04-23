package com.boydti.fawe.forge;

import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

public class ForgeCommand extends CommandBase {

    private final String name;
    private final FaweCommand cmd;

    public ForgeCommand(String name, FaweCommand cmd) {
        this.name = name;
        this.cmd = cmd;
    }

    @Override
    public String getCommandName() {
        return name;
    }

    @Override
    public String getCommandUsage(ICommandSender iCommandSender) {
        return "/" + name;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if ((sender instanceof EntityPlayerMP)) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            if (player.worldObj.isRemote) {
                return;
            }
            FawePlayer<Object> fp = FawePlayer.wrap(player);
            cmd.executeSafe(fp, args);
        }
    }
}

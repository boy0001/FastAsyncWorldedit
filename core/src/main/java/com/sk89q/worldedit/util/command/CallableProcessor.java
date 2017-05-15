package com.sk89q.worldedit.util.command;

import com.plotsquared.general.commands.Command;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEditException;

public interface CallableProcessor<T> {
    public Object process(CommandLocals locals, T value) throws Command.CommandException, WorldEditException;

}

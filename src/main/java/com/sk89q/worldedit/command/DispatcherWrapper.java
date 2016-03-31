package com.sk89q.worldedit.command;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.boydti.fawe.util.TaskManager;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Description;
import com.sk89q.worldedit.util.command.Dispatcher;

public class DispatcherWrapper implements Dispatcher {
    private final Dispatcher parent;
    
    public final Dispatcher getParent() {
        return parent;
    }
    
    public DispatcherWrapper(Dispatcher parent) {
        this.parent = parent;
    }
    
    @Override
    public void registerCommand(CommandCallable callable, String... alias) {
        parent.registerCommand(callable, alias);
    }
    
    @Override
    public Set<CommandMapping> getCommands() {
        return parent.getCommands();
    }
    
    @Override
    public Collection<String> getPrimaryAliases() {
        return parent.getPrimaryAliases();
    }
    
    @Override
    public Collection<String> getAliases() {
        return parent.getAliases();
    }
    
    @Override
    public CommandMapping get(String alias) {
        return parent.get(alias);
    }
    
    @Override
    public boolean contains(String alias) {
        return parent.contains(alias);
    }
    
    
    
    @Override
    public Object call(final String arguments, final CommandLocals locals, final String[] parentCommands) throws CommandException {
        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {
                try {
                    parent.call(arguments, locals, parentCommands);
                } catch (CommandException e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }
    
    @Override
    public Description getDescription() {
        return parent.getDescription();
    }
    
    @Override
    public boolean testPermission(CommandLocals locals) {
        return parent.testPermission(locals);
    }

    @Override
    public List<String> getSuggestions(String arguments, CommandLocals locals) throws CommandException {
        return parent.getSuggestions(arguments, locals);
    }
    
    public static void inject() {
        // Delayed injection
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                try {
                    PlatformManager platform = WorldEdit.getInstance().getPlatformManager();
                    CommandManager command = platform.getCommandManager();
                    Class<? extends CommandManager> clazz = command.getClass();
                    Field field = clazz.getDeclaredField("dispatcher");
                    field.setAccessible(true);
                    Dispatcher parent = (Dispatcher) field.get(command);
                    DispatcherWrapper dispatcher = new DispatcherWrapper(parent);
                    field.set(command, dispatcher);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
}
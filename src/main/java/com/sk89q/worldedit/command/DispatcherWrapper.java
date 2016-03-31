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
        return this.parent;
    }

    public DispatcherWrapper(final Dispatcher parent) {
        this.parent = parent;
    }

    @Override
    public void registerCommand(final CommandCallable callable, final String... alias) {
        this.parent.registerCommand(callable, alias);
    }

    @Override
    public Set<CommandMapping> getCommands() {
        return this.parent.getCommands();
    }

    @Override
    public Collection<String> getPrimaryAliases() {
        return this.parent.getPrimaryAliases();
    }

    @Override
    public Collection<String> getAliases() {
        return this.parent.getAliases();
    }

    @Override
    public CommandMapping get(final String alias) {
        return this.parent.get(alias);
    }

    @Override
    public boolean contains(final String alias) {
        return this.parent.contains(alias);
    }

    @Override
    public Object call(final String arguments, final CommandLocals locals, final String[] parentCommands) throws CommandException {
        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {
                try {
                    DispatcherWrapper.this.parent.call(arguments, locals, parentCommands);
                } catch (final CommandException e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }

    @Override
    public Description getDescription() {
        return this.parent.getDescription();
    }

    @Override
    public boolean testPermission(final CommandLocals locals) {
        return this.parent.testPermission(locals);
    }

    @Override
    public List<String> getSuggestions(final String arguments, final CommandLocals locals) throws CommandException {
        return this.parent.getSuggestions(arguments, locals);
    }

    public static void inject() {
        // Delayed injection
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                try {
                    final PlatformManager platform = WorldEdit.getInstance().getPlatformManager();
                    final CommandManager command = platform.getCommandManager();
                    final Class<? extends CommandManager> clazz = command.getClass();
                    final Field field = clazz.getDeclaredField("dispatcher");
                    field.setAccessible(true);
                    final Dispatcher parent = (Dispatcher) field.get(command);
                    final DispatcherWrapper dispatcher = new DispatcherWrapper(parent);
                    field.set(command, dispatcher);
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

}

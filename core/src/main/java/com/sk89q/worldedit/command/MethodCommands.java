package com.sk89q.worldedit.command;

import com.boydti.fawe.config.Commands;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.Dispatcher;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;


import static com.google.common.base.Preconditions.checkNotNull;

public class MethodCommands {
    public final WorldEdit worldEdit;
    private ConcurrentHashMap<Method, CommandCallable> callables;
    private Dispatcher dispatcher;

    public MethodCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
        callables = new ConcurrentHashMap<>();
    }

    @Deprecated
    public MethodCommands() {
        this.worldEdit = WorldEdit.getInstance();
    }

    public void register(Method method, CommandCallable callable, Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.callables.put(method, callable);
    }

    public CommandCallable getCallable() {
        try {
            StackTraceElement[] stack = new Exception().getStackTrace();
            for (StackTraceElement elem : stack) {
                Class<?> clazz = Class.forName(elem.getClassName());
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(elem.getMethodName())) {
                        Command command = method.getAnnotation(Command.class);
                        if (command != null) return callables.get(method);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return dispatcher;
    }

    public Command getCommand() {
        try {
            StackTraceElement[] stack = new Exception().getStackTrace();
            for (StackTraceElement elem : stack) {
                Class<?> clazz = Class.forName(elem.getClassName());
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(elem.getMethodName())) {
                        Command command = method.getAnnotation(Command.class);
                        if (command != null) return Commands.translate(clazz, command);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}

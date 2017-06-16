/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extension.platform;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.command.AnvilCommands;
import com.boydti.fawe.command.MaskBinding;
import com.boydti.fawe.command.PatternBinding;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.FakePlayer;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.google.common.base.Joiner;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.BiomeCommands;
import com.sk89q.worldedit.command.BrushCommands;
import com.sk89q.worldedit.command.BrushOptionsCommands;
import com.sk89q.worldedit.command.BrushProcessor;
import com.sk89q.worldedit.command.ChunkCommands;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.GenerationCommands;
import com.sk89q.worldedit.command.HistoryCommands;
import com.sk89q.worldedit.command.NavigationCommands;
import com.sk89q.worldedit.command.OptionsCommands;
import com.sk89q.worldedit.command.RegionCommands;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.ScriptingCommands;
import com.sk89q.worldedit.command.SelectionCommands;
import com.sk89q.worldedit.command.SnapshotCommands;
import com.sk89q.worldedit.command.SnapshotUtilCommands;
import com.sk89q.worldedit.command.SuperPickaxeCommands;
import com.sk89q.worldedit.command.ToolCommands;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.command.WorldEditCommands;
import com.sk89q.worldedit.command.argument.ReplaceParser;
import com.sk89q.worldedit.command.argument.TreeGeneratorParser;
import com.sk89q.worldedit.command.composition.ApplyCommand;
import com.sk89q.worldedit.command.composition.DeformCommand;
import com.sk89q.worldedit.command.composition.PaintCommand;
import com.sk89q.worldedit.command.composition.ShapedBrushCommand;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.event.platform.CommandSuggestionEvent;
import com.sk89q.worldedit.function.factory.Deform;
import com.sk89q.worldedit.function.factory.Deform.Mode;
import com.sk89q.worldedit.internal.command.ActorAuthorizer;
import com.sk89q.worldedit.internal.command.CommandLoggingHandler;
import com.sk89q.worldedit.internal.command.UserCommandCompleter;
import com.sk89q.worldedit.internal.command.WorldEditBinding;
import com.sk89q.worldedit.internal.command.WorldEditExceptionConverter;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.InvalidUsageException;
import com.sk89q.worldedit.util.command.composition.ProvidedValue;
import com.sk89q.worldedit.util.command.fluent.CommandGraph;
import com.sk89q.worldedit.util.command.fluent.DispatcherNode;
import com.sk89q.worldedit.util.command.parametric.ExceptionConverter;
import com.sk89q.worldedit.util.command.parametric.LegacyCommandsHandler;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.util.formatting.ColorCodeBuilder;
import com.sk89q.worldedit.util.formatting.component.CommandUsageBox;
import com.sk89q.worldedit.util.logging.DynamicStreamHandler;
import com.sk89q.worldedit.util.logging.LogFormat;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.util.command.composition.LegacyCommandAdapter.adapt;

/**
 * Handles the registration and invocation of commands.
 *
 * <p>This class is primarily for internal usage.</p>
 */
public final class CommandManager {

    public static final Pattern COMMAND_CLEAN_PATTERN = Pattern.compile("^[/]+");
    private static final Logger log = Logger.getLogger(CommandManager.class.getCanonicalName());
    private static final Logger commandLog = Logger.getLogger(CommandManager.class.getCanonicalName() + ".CommandLog");
    private static final Pattern numberFormatExceptionPattern = Pattern.compile("^For input string: \"(.*)\"$");

    private final WorldEdit worldEdit;
    private final PlatformManager platformManager;
    private volatile Dispatcher dispatcher;
    private volatile Platform platform;
    private final DynamicStreamHandler dynamicHandler = new DynamicStreamHandler();
    private final ExceptionConverter exceptionConverter;


    private Map<Object, String[]> methodMap;

    private static CommandManager INSTANCE;

    /**
     * Create a new instance.
     *
     * @param worldEdit the WorldEdit instance
     */
    public CommandManager(final WorldEdit worldEdit, PlatformManager platformManager) {
        checkNotNull(worldEdit);
        checkNotNull(platformManager);
        INSTANCE = this;

        this.worldEdit = worldEdit;
        this.platformManager = platformManager;
        this.exceptionConverter = new WorldEditExceptionConverter(worldEdit);

        // Register this instance for command events
        worldEdit.getEventBus().register(this);

        // Setup the logger
        commandLog.addHandler(dynamicHandler);
        dynamicHandler.setFormatter(new LogFormat());

        this.methodMap = new ConcurrentHashMap<>();
    }

    /**
     * Register all the methods in the class as commands<br>
     *  - You should try to register commands during startup
     * @param clazz The class containing all the commands
     */
    public void registerCommands(Object clazz) {
        registerCommands(clazz, new String[0]);
    }

    /**
     * Create a command with the provided aliases and register all methods of the class as sub commands.<br>
     *  - You should try to register commands during startup
     * @param clazz The class containing all the sub command methods
     * @param aliases The aliases to give the command
     */
    public void registerCommands(Object clazz, String... aliases) {
        if (platform != null) {
            ParametricBuilder builder = new ParametricBuilder();
            builder.setAuthorizer(new ActorAuthorizer());
            builder.setDefaultCompleter(new UserCommandCompleter(platformManager));
            builder.addBinding(new WorldEditBinding(worldEdit));

            builder.addBinding(new PatternBinding(worldEdit), com.sk89q.worldedit.function.pattern.Pattern.class);
            builder.addBinding(new MaskBinding(worldEdit), com.sk89q.worldedit.function.mask.Mask.class);

            DispatcherNode graph = new CommandGraph().builder(builder).commands();
            if (aliases.length == 0) {
                graph = graph.registerMethods(clazz);
            } else {
                graph = graph.group(aliases).registerMethods(clazz).parent();
            }
            Dispatcher dispatcher = graph.graph().getDispatcher();
            platform.registerCommands(dispatcher);
        } else {
            methodMap.put(clazz, aliases);
        }
    }

    /**
     * Initialize the dispatcher
     */
    public void setupDispatcher() {
        ParametricBuilder builder = new ParametricBuilder();
        builder.setAuthorizer(new ActorAuthorizer());
        builder.setDefaultCompleter(new UserCommandCompleter(platformManager));
        builder.addBinding(new WorldEditBinding(worldEdit));

        builder.addBinding(new PatternBinding(worldEdit), com.sk89q.worldedit.function.pattern.Pattern.class);
        builder.addBinding(new MaskBinding(worldEdit), com.sk89q.worldedit.function.mask.Mask.class);

        builder.addInvokeListener(new LegacyCommandsHandler());
        builder.addInvokeListener(new CommandLoggingHandler(worldEdit, commandLog));
        DispatcherNode graph = new CommandGraph().builder(builder).commands();

        for (Map.Entry<Object, String[]> entry : methodMap.entrySet()) {
            // add  command
            String[] aliases = entry.getValue();
            if (aliases.length == 0) {
                graph = graph.registerMethods(entry.getKey());
            } else {
                graph = graph.group(aliases).registerMethods(entry.getKey()).parent();
            }
        }
        methodMap.clear();

        dispatcher = graph
                .group("/anvil")
                .describeAs("Anvil command")
                .registerMethods(new AnvilCommands(worldEdit)).parent()
                .registerMethods(new BiomeCommands(worldEdit))
                .registerMethods(new ChunkCommands(worldEdit))
                .registerMethods(new ClipboardCommands(worldEdit))
                .registerMethods(new OptionsCommands(worldEdit))
                .registerMethods(new GenerationCommands(worldEdit))
                .registerMethods(new HistoryCommands(worldEdit))
                .registerMethods(new NavigationCommands(worldEdit))
                .registerMethods(new RegionCommands(worldEdit))
                .registerMethods(new ScriptingCommands(worldEdit))
                .registerMethods(new SelectionCommands(worldEdit))
                .registerMethods(new SnapshotUtilCommands(worldEdit))
                .registerMethods(new BrushOptionsCommands(worldEdit))
                .registerMethods(new ToolCommands(worldEdit))
                .registerMethods(new UtilityCommands(worldEdit))
                .group("worldedit", "we", "fawe")
                .describeAs("FAWE commands")
                .registerMethods(new WorldEditCommands(worldEdit)).parent().group("schematic", "schem", "/schematic", "/schem")
                .describeAs("Schematic commands for saving/loading areas")
                .registerMethods(new SchematicCommands(worldEdit)).parent().group("snapshot", "snap")
                .describeAs("Schematic commands for saving/loading areas")
                .registerMethods(new SnapshotCommands(worldEdit)).parent().group("brush", "br", "/b", "tool").describeAs("Bind brushes and tools to items")
                .registerMethods(new ToolCommands(worldEdit))
                .registerMethods(new BrushOptionsCommands(worldEdit))
                .registerMethods(new BrushCommands(worldEdit), new BrushProcessor(worldEdit))
                .register(adapt(new ShapedBrushCommand(new DeformCommand(), "worldedit.brush.deform")), "deform")
                .register(adapt(new ShapedBrushCommand(new ApplyCommand(new ReplaceParser(), "Set all blocks within region"), "worldedit.brush.set")), "set")
                .register(adapt(new ShapedBrushCommand(new PaintCommand(), "worldedit.brush.paint")), "paint")
                .register(adapt(new ShapedBrushCommand(new ApplyCommand(), "worldedit.brush.apply")), "apply")
                .register(adapt(new ShapedBrushCommand(new PaintCommand(new TreeGeneratorParser("treeType")), "worldedit.brush.forest")), "forest")
                .register(adapt(new ShapedBrushCommand(ProvidedValue.create(new Deform("y-=1", Mode.RAW_COORD), "Raise one block"), "worldedit.brush.raise")), "raise")
                .register(adapt(new ShapedBrushCommand(ProvidedValue.create(new Deform("y+=1", Mode.RAW_COORD), "Lower one block"), "worldedit.brush.lower")), "lower").parent()
                .group("superpickaxe", "pickaxe", "sp").describeAs("Super-pickaxe commands")
                .registerMethods(new SuperPickaxeCommands(worldEdit))
                .parent().graph().getDispatcher();
        if (platform != null) {
            platform.registerCommands(dispatcher);
        }
    }

    public static CommandManager getInstance() {
        return INSTANCE;
    }

    public ExceptionConverter getExceptionConverter() {
        return exceptionConverter;
    }

    public void register(Platform platform) {
        log.log(Level.FINE, "Registering commands with " + platform.getClass().getCanonicalName());

        LocalConfiguration config = platform.getConfiguration();
        boolean logging = config.logCommands;
        String path = config.logFile;

        // Register log
        if (!logging || path.isEmpty()) {
            dynamicHandler.setHandler(null);
            commandLog.setLevel(Level.OFF);
        } else {
            File file = new File(config.getWorkingDirectory(), path);
            commandLog.setLevel(Level.ALL);

            log.log(Level.INFO, "Logging WorldEdit commands to " + file.getAbsolutePath());

            try {
                dynamicHandler.setHandler(new FileHandler(file.getAbsolutePath(), true));
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not use command log file " + path + ": " + e.getMessage());
            }
        }

        this.platform = platform;
        setupDispatcher();
    }

    public void unregister() {
        dynamicHandler.setHandler(null);
    }

    public String[] commandDetection(String[] split) {
        // Quick script shortcut
        if (split[0].matches("^[^/].*\\.js$")) {
            String[] newSplit = new String[split.length + 1];
            System.arraycopy(split, 0, newSplit, 1, split.length);
            newSplit[0] = "cs";
            newSplit[1] = newSplit[1];
            split = newSplit;
        }

        String searchCmd = split[0].toLowerCase();

        // Try to detect the command
        if (!dispatcher.contains(searchCmd)) {
            if (worldEdit.getConfiguration().noDoubleSlash && dispatcher.contains("/" + searchCmd)) {
                split[0] = "/" + split[0];
            } else if (searchCmd.length() >= 2 && searchCmd.charAt(0) == '/' && dispatcher.contains(searchCmd.substring(1))) {
                split[0] = split[0].substring(1);
            }
        }

        return split;
    }

    public void handleCommandOnCurrentThread(final CommandEvent event) {
        Actor actor = platformManager.createProxyActor(event.getActor());
        final String args = event.getArguments();
        final String[] split = commandDetection(args.split(" "));
        // No command found!
        if (!dispatcher.contains(split[0])) {
            return;
        }
        if (!actor.isPlayer()) {
            actor = FakePlayer.wrap(actor.getName(), actor.getUniqueId(), actor);
        }
        final LocalSession session = worldEdit.getSessionManager().get(actor);
        LocalConfiguration config = worldEdit.getConfiguration();
        final CommandLocals locals = new CommandLocals();
        final FawePlayer fp = FawePlayer.wrap(actor);
        if (fp == null) {
            throw new IllegalArgumentException("FAWE doesn't support: " + actor);
        }
        final Set<String> failedPermissions = new LinkedHashSet<>();
        if (actor instanceof Player) {
            actor = new LocationMaskedPlayerWrapper((Player) actor, ((Player) actor).getLocation(), true) {
                @Override
                public boolean hasPermission(String permission) {
                    if (!super.hasPermission(permission)) {
                        failedPermissions.add(permission);
                        return false;
                    }
                    return true;
                }

                @Override
                public void checkPermission(String permission) throws AuthorizationException {
                    try {
                        super.checkPermission(permission);
                    } catch (AuthorizationException e) {
                        failedPermissions.add(permission);
                        throw e;
                    }
                }
            };
        }
        locals.put(Actor.class, actor);
        final Actor finalActor = actor;

        locals.put("arguments", args);
        final long start = System.currentTimeMillis();
        try {
            // This is a bit of a hack, since the call method can only throw CommandExceptions
            // everything needs to be wrapped at least once. Which means to handle all WorldEdit
            // exceptions without writing a hook into every dispatcher, we need to unwrap these
            // exceptions and rethrow their converted form, if their is one.
            try {
                Object result = dispatcher.call(Joiner.on(" ").join(split), locals, new String[0]);
            } catch (Throwable t) {
                // Use the exception converter to convert the exception if any of its causes
                // can be converted, otherwise throw the original exception
                Throwable next = t;
                do {
                    exceptionConverter.convert(next);
                    next = next.getCause();
                } while (next != null);

                throw t;
            }
        } catch (CommandPermissionsException e) {
            BBC.NO_PERM.send(finalActor, StringMan.join(failedPermissions, " "));
        } catch (InvalidUsageException e) {
            if (e.isFullHelpSuggested()) {
                finalActor.printRaw(BBC.getPrefix() + ColorCodeBuilder.asColorCodes(new CommandUsageBox(e.getCommand(), e.getCommandUsed("", ""), locals)));
                String message = e.getMessage();
                if (message != null) {
                    finalActor.printError(message);
                }
            } else {
                String message = e.getMessage();
                finalActor.printRaw(BBC.getPrefix() + (message != null ? message : "The command was not used properly (no more help available)."));
                BBC.COMMAND_SYNTAX.send(finalActor, e.getSimpleUsageString("/"));
            }
        } catch (WrappedCommandException e) {
            Exception faweException = FaweException.get(e);
            String message = e.getMessage();
            if (faweException != null) {
                BBC.WORLDEDIT_CANCEL_REASON.send(finalActor, faweException.getMessage());
            } else {
                finalActor.printError("There was an error handling a FAWE command: [See console]");
                finalActor.printRaw(e.getClass().getName() + ": " + e.getMessage());
                log.log(Level.SEVERE, "An unexpected error occurred while handling a FAWE command", e);
            }
        } catch (CommandException e) {
            String message = e.getMessage();
            if (message != null) {
                actor.printError(e.getMessage());
            } else {
                actor.printError("An unknown FAWE error has occurred! Please see console.");
                log.log(Level.SEVERE, "An unknown FAWE error occurred", e);
            }
        } finally {
            final EditSession editSession = locals.get(EditSession.class);
            if (editSession != null) {
                editSession.flushQueue();
                worldEdit.flushBlockBag(finalActor, editSession);
                session.remember(editSession);
                final long time = System.currentTimeMillis() - start;
                if (time > 1000) {
                    BBC.ACTION_COMPLETE.send(finalActor, (time / 1000d));
                }
                Request.reset();
            }
        }
    }

    @Subscribe
    public void handleCommand(final CommandEvent event) {
        Request.reset();
        final FawePlayer<Object> fp = FawePlayer.wrap(event.getActor());
        TaskManager.IMP.taskNow(new Runnable() {
            @Override
            public void run() {
                if (!fp.runAction(new Runnable() {
                    @Override
                    public void run() {
                        handleCommandOnCurrentThread(event);
                    }
                }, true, false)) {
                    BBC.WORLDEDIT_COMMAND_LIMIT.send(fp);
                }
                event.setCancelled(true);
            }
        }, Fawe.isMainThread());
    }

    @Subscribe
    public void handleCommandSuggestion(CommandSuggestionEvent event) {
        try {
            CommandLocals locals = new CommandLocals();
            locals.put(Actor.class, event.getActor());
            event.setSuggestions(dispatcher.getSuggestions(event.getArguments(), locals));
        } catch (CommandException e) {
            event.getActor().printError(e.getMessage());
        }
    }

    /**
     * Get the command dispatcher instance.
     *
     * @return the command dispatcher
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public static Logger getLogger() {
        return commandLog;
    }

    public static Class<?> inject() {
        return CommandManager.class;
    }
}
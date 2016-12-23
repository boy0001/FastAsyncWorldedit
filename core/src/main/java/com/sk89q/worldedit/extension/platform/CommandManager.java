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
import com.sk89q.worldedit.command.ChunkCommands;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.GeneralCommands;
import com.sk89q.worldedit.command.GenerationCommands;
import com.sk89q.worldedit.command.HistoryCommands;
import com.sk89q.worldedit.command.NavigationCommands;
import com.sk89q.worldedit.command.RegionCommands;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.ScriptingCommands;
import com.sk89q.worldedit.command.SelectionCommands;
import com.sk89q.worldedit.command.SnapshotCommands;
import com.sk89q.worldedit.command.SnapshotUtilCommands;
import com.sk89q.worldedit.command.SuperPickaxeCommands;
import com.sk89q.worldedit.command.ToolCommands;
import com.sk89q.worldedit.command.ToolUtilCommands;
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
import java.util.Set;
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
    private final Dispatcher dispatcher;
    private final DynamicStreamHandler dynamicHandler = new DynamicStreamHandler();
    private final ExceptionConverter exceptionConverter;

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

        // Set up the commands manager
        ParametricBuilder builder = new ParametricBuilder();
        builder.setAuthorizer(new ActorAuthorizer());
        builder.setDefaultCompleter(new UserCommandCompleter(platformManager));
        builder.addBinding(new WorldEditBinding(worldEdit));

        builder.addBinding(new PatternBinding(worldEdit), com.sk89q.worldedit.function.pattern.Pattern.class);
        builder.addBinding(new MaskBinding(worldEdit), com.sk89q.worldedit.function.mask.Mask.class);

        builder.addInvokeListener(new LegacyCommandsHandler());
        builder.addInvokeListener(new CommandLoggingHandler(worldEdit, commandLog));
        dispatcher = new CommandGraph().builder(builder).commands()
                .registerMethods(new AnvilCommands(worldEdit)) // Added
                .registerMethods(new BiomeCommands(worldEdit))
                .registerMethods(new ChunkCommands(worldEdit))
                .registerMethods(new ClipboardCommands(worldEdit))
                .registerMethods(new GeneralCommands(worldEdit))
                .registerMethods(new GenerationCommands(worldEdit))
                .registerMethods(new HistoryCommands(worldEdit))
                .registerMethods(new NavigationCommands(worldEdit))
                .registerMethods(new RegionCommands(worldEdit))
                .registerMethods(new ScriptingCommands(worldEdit))
                .registerMethods(new SelectionCommands(worldEdit))
                .registerMethods(new SnapshotUtilCommands(worldEdit))
                .registerMethods(new ToolUtilCommands(worldEdit))
                .registerMethods(new ToolCommands(worldEdit))
                .registerMethods(new UtilityCommands(worldEdit))
                .group("worldedit", "we")
                .describeAs("WorldEdit commands")
                .registerMethods(new WorldEditCommands(worldEdit)).parent().group("schematic", "schem", "/schematic", "/schem")
                .describeAs("Schematic commands for saving/loading areas")
                .registerMethods(new SchematicCommands(worldEdit)).parent().group("snapshot", "snap")
                .describeAs("Schematic commands for saving/loading areas")
                .registerMethods(new SnapshotCommands(worldEdit)).parent().group("brush", "br").describeAs("Brushing commands")
                .registerMethods(new BrushCommands(worldEdit)).register(adapt(new ShapedBrushCommand(new DeformCommand(), "worldedit.brush.deform")), "deform")
                .register(adapt(new ShapedBrushCommand(new ApplyCommand(new ReplaceParser(), "Set all blocks within region"), "worldedit.brush.set")), "set")
                .register(adapt(new ShapedBrushCommand(new PaintCommand(), "worldedit.brush.paint")), "paint").register(adapt(new ShapedBrushCommand(new ApplyCommand(), "worldedit.brush.apply")), "apply")
                .register(adapt(new ShapedBrushCommand(new PaintCommand(new TreeGeneratorParser("treeType")), "worldedit.brush.forest")), "forest")
                .register(adapt(new ShapedBrushCommand(ProvidedValue.create(new Deform("y-=1", Mode.RAW_COORD), "Raise one block"), "worldedit.brush.raise")), "raise")
                .register(adapt(new ShapedBrushCommand(ProvidedValue.create(new Deform("y+=1", Mode.RAW_COORD), "Lower one block"), "worldedit.brush.lower")), "lower").parent()
                .group("superpickaxe", "pickaxe", "sp").describeAs("Super-pickaxe commands")
                .registerMethods(new SuperPickaxeCommands(worldEdit)).parent().group("tool")
                .describeAs("Bind functions to held items")
                .registerMethods(new ToolCommands(worldEdit)).parent().graph().getDispatcher();
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

        platform.registerCommands(dispatcher);
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

    public void handleCommandOnCurrentThread(final CommandEvent event, boolean checkLimit) {
        Actor actor = platformManager.createProxyActor(event.getActor());
        final String args = event.getArguments();
        final String[] split = commandDetection(args.split(" "));
        // No command found!
        if (!dispatcher.contains(split[0])) {
            return;
        }
        if (!actor.isPlayer()) {
            actor = new FakePlayer(actor.getName(), actor.getUniqueId(), actor);
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
        if (!fp.runAction(new Runnable() {
            @Override
            public void run() {
                locals.put("arguments", args);
                final long start = System.currentTimeMillis();
                try {
                    // This is a bit of a hack, since the call method can only throw CommandExceptions
                    // everything needs to be wrapped at least once. Which means to handle all WorldEdit
                    // exceptions without writing a hook into every dispatcher, we need to unwrap these
                    // exceptions and rethrow their converted form, if their is one.
                    try {
                        dispatcher.call(Joiner.on(" ").join(split), locals, new String[0]);
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
                        finalActor.printRaw(ColorCodeBuilder.asColorCodes(new CommandUsageBox(e.getCommand(), e.getCommandUsed("/", ""), locals)));
                        String message = e.getMessage();
                        if (message != null) {
                            finalActor.printError(message);
                        }
                    } else {
                        String message = e.getMessage();
                        finalActor.print(BBC.getPrefix() + (message != null ? message : "The command was not used properly (no more help available)."));
                        BBC.COMMAND_SYNTAX.send(finalActor, e.getSimpleUsageString("/"));
                    }
                } catch (WrappedCommandException e) {
                    FaweException faweException = FaweException.get(e);
                    if (faweException != null) {
                        BBC.WORLDEDIT_CANCEL_REASON.send(finalActor, faweException.getMessage());
                    } else {
                        Throwable t = e.getCause();
                        finalActor.printError("Please report this error: [See console]");
                        finalActor.printRaw(t.getClass().getName() + ": " + t.getMessage());
                        log.log(Level.SEVERE, "An unexpected error while handling a WorldEdit command", t);
                    }
                } catch (CommandException e) {
                    String message = e.getMessage();
                    if (message != null) {
                        finalActor.printError(e.getMessage());
                    } else {
                        finalActor.printError("An unknown error has occurred! Please see console.");
                        log.log(Level.SEVERE, "An unknown error occurred", e);
                    }
                } finally {
                    final EditSession editSession = locals.get(EditSession.class);
                    boolean hasSession = false;
                    if (editSession != null) {
                        editSession.flushQueue();
                        worldEdit.flushBlockBag(finalActor, editSession);
                        session.remember(editSession);
                    }
                }
            }
        }, checkLimit, false)) {
            BBC.WORLDEDIT_COMMAND_LIMIT.send(fp);
        }
    }

    @Subscribe
    public void handleCommand(final CommandEvent event) {
        Request.reset();
        TaskManager.IMP.taskNow(new Runnable() {
            @Override
            public void run() {
                handleCommandOnCurrentThread(event, true);
            }
        }, Fawe.get().isMainThread());
        event.setCancelled(true);
    }

    @Subscribe
    public void handleCommandSuggestion(CommandSuggestionEvent event) {
        try {
            CommandLocals locals = new CommandLocals();
            locals.put(Actor.class, event.getActor());
            locals.put("arguments", event.getArguments());
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
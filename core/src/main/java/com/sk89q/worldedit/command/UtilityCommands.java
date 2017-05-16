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

package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.AtomicDouble;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.DelegateCallable;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.PrimaryAliasComparator;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.command.parametric.ParametricCallable;
import com.sk89q.worldedit.util.formatting.ColorCodeBuilder;
import com.sk89q.worldedit.util.formatting.component.CommandUsageBox;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import static com.sk89q.minecraft.util.commands.Logging.LogMode.PLACEMENT;

/**
 * Utility commands.
 */
@Command(aliases = {}, desc = "Various utility commands: [More Info](http://wiki.sk89q.com/wiki/WorldEdit/Utilities)")
public class UtilityCommands extends MethodCommands {
    public UtilityCommands(WorldEdit we) {
        super(we);
    }


    @Command(
        aliases = { "/fill" },
        usage = "<block> <radius> [depth]",
        desc = "Fill a hole",
        min = 2,
        max = 3
    )
    @CommandPermissions("worldedit.fill")
    @Logging(PLACEMENT)
    public void fill(Player player, LocalSession session, EditSession editSession, Pattern pattern, double radius, @Optional("1") double depth) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        Vector pos = session.getPlacementPosition(player);
        int affected = 0;
        if (pattern instanceof BlockPattern) {
            affected = editSession.fillXZ(pos, ((BlockPattern) pattern).getBlock(), radius, (int) depth, false);
        } else {
            affected = editSession.fillXZ(pos, pattern, radius, (int) depth, false);
        }
        player.print(BBC.getPrefix() + affected + " block(s) have been created.");
    }

    @Command(
        aliases = { "/fillr" },
        usage = "<block> <radius> [depth]",
        desc = "Fill a hole recursively",
        min = 2,
        max = 3
    )
    @CommandPermissions("worldedit.fill.recursive")
    @Logging(PLACEMENT)
    public void fillr(Player player, LocalSession session, EditSession editSession, Pattern pattern, double radius, @Optional("1") double depth) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        Vector pos = session.getPlacementPosition(player);
        int affected = 0;
        if (pattern instanceof BlockPattern) {
            affected = editSession.fillXZ(pos, ((BlockPattern) pattern).getBlock(), radius, (int) depth, true);
        } else {
            affected = editSession.fillXZ(pos, pattern, radius, (int) depth, true);
        }
        player.print(BBC.getPrefix() + affected + " block(s) have been created.");
    }

    @Command(
        aliases = { "/drain" },
        usage = "<radius>",
        desc = "Drain a pool",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.drain")
    @Logging(PLACEMENT)
    public void drain(Player player, LocalSession session, EditSession editSession, double radius) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        int affected = editSession.drainArea(
                session.getPlacementPosition(player), radius);
        player.print(BBC.getPrefix() + affected + " block(s) have been changed.");
    }

    @Command(
        aliases = { "/fixlava", "fixlava" },
        usage = "<radius>",
        desc = "Fix lava to be stationary",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.fixlava")
    @Logging(PLACEMENT)
    public void fixLava(Player player, LocalSession session, EditSession editSession, double radius) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        int affected = editSession.fixLiquid(
                session.getPlacementPosition(player), radius, 10, 11);
        player.print(BBC.getPrefix() + affected + " block(s) have been changed.");
    }

    @Command(
        aliases = { "/fixwater", "fixwater" },
        usage = "<radius>",
        desc = "Fix water to be stationary",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.fixwater")
    @Logging(PLACEMENT)
    public void fixWater(Player player, LocalSession session, EditSession editSession, double radius) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        int affected = editSession.fixLiquid(
                session.getPlacementPosition(player), radius, 8, 9);
        player.print(BBC.getPrefix() + affected + " block(s) have been changed.");
    }

    @Command(
        aliases = { "/removeabove", "removeabove" },
        usage = "[size] [height]",
        desc = "Remove blocks above your head.",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.removeabove")
    @Logging(PLACEMENT)
    public void removeAbove(Player player, LocalSession session, EditSession editSession, @Optional("1") double size, @Optional("256") double height) throws WorldEditException {
        worldEdit.checkMaxRadius(size);
        int affected = editSession.removeAbove(session.getPlacementPosition(player), (int) size, (int) height);
        player.print(BBC.getPrefix() + affected + " block(s) have been removed.");
    }

    @Command(
        aliases = { "/removebelow", "removebelow" },
        usage = "[size] [height]",
        desc = "Remove blocks below you.",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.removebelow")
    @Logging(PLACEMENT)
    public void removeBelow(Player player, LocalSession session, EditSession editSession, @Optional("1") double size, @Optional("256") double height) throws WorldEditException {
        worldEdit.checkMaxRadius(size);
        int affected = editSession.removeBelow(session.getPlacementPosition(player), (int) size, (int) height);
        player.print(BBC.getPrefix() + affected + " block(s) have been removed.");
    }

    @Command(
        aliases = { "/removenear", "removenear" },
        usage = "<block> [size]",
        desc = "Remove blocks near you.",
        min = 1,
        max = 2
    )
    @CommandPermissions("worldedit.removenear")
    @Logging(PLACEMENT)
    public void removeNear(Player player, LocalSession session, EditSession editSession, BaseBlock block, @Optional("50") double size) throws WorldEditException {
        worldEdit.checkMaxRadius(size);
        int affected = editSession.removeNear(session.getPlacementPosition(player), block.getId(), (int) size);
        player.print(BBC.getPrefix() + affected + " block(s) have been removed.");
    }

    @Command(
        aliases = { "/replacenear", "replacenear" },
        usage = "<size> <from-id> <to-id>",
        desc = "Replace nearby blocks",
        flags = "f",
        min = 3,
        max = 3
    )
    @CommandPermissions("worldedit.replacenear")
    @Logging(PLACEMENT)
    public void replaceNear(Player player, LocalSession session, EditSession editSession, double size, @Optional Mask from, Pattern to) throws WorldEditException {
        if (from == null) {
            from = new ExistingBlockMask(editSession);
        }
        Vector base = session.getPlacementPosition(player);
        Vector min = base.subtract(size, size, size);
        Vector max = base.add(size, size, size);
        Region region = new CuboidRegion(player.getWorld(), min, max);

        int affected = editSession.replaceBlocks(region, from, to);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
        aliases = { "/snow", "snow" },
        usage = "[radius]",
        desc = "Simulates snow",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.snow")
    @Logging(PLACEMENT)
    public void snow(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;

        int affected = editSession.simulateSnow(session.getPlacementPosition(player), size);
        player.print(BBC.getPrefix() + affected + " surfaces covered. Let it snow~");
    }

    @Command(
        aliases = {"/thaw", "thaw"},
        usage = "[radius]",
        desc = "Thaws the area",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.thaw")
    @Logging(PLACEMENT)
    public void thaw(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;

        int affected = editSession.thaw(session.getPlacementPosition(player), size);
        player.print(BBC.getPrefix() + affected + " surfaces thawed.");
    }

    @Command(
        aliases = { "/green", "green" },
        usage = "[radius]",
        desc = "Greens the area",
        flags = "f",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.green")
    @Logging(PLACEMENT)
    public void green(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        final double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;
        final boolean onlyNormalDirt = !args.hasFlag('f');

        final int affected = editSession.green(session.getPlacementPosition(player), size, onlyNormalDirt);
        player.print(BBC.getPrefix() + affected + " surfaces greened.");
    }

    @Command(
            aliases = { "/ex", "/ext", "/extinguish", "ex", "ext", "extinguish" },
            usage = "[radius]",
            desc = "Extinguish nearby fire",
            min = 0,
            max = 1
        )
    @CommandPermissions("worldedit.extinguish")
    @Logging(PLACEMENT)
    public void extinguish(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = worldEdit.getConfiguration();

        int defaultRadius = config.maxRadius != -1 ? Math.min(40, config.maxRadius) : 40;
        int size = args.argsLength() > 0 ? Math.max(1, args.getInteger(0))
                : defaultRadius;
        worldEdit.checkMaxRadius(size);

        int affected = editSession.removeNear(session.getPlacementPosition(player), 51, size);
        player.print(BBC.getPrefix() + affected + " block(s) have been removed.");
    }

    @Command(
        aliases = { "butcher" },
        usage = "[radius]",
        flags = "plangbtfr",
        desc = "Kill all or nearby mobs",
        help =
            "Kills nearby mobs, based on radius, if none is given uses default in configuration.\n" +
            "Flags:\n" +
            "  -p also kills pets.\n" +
            "  -n also kills NPCs.\n" +
            "  -g also kills Golems.\n" +
            "  -a also kills animals.\n" +
            "  -b also kills ambient mobs.\n" +
            "  -t also kills mobs with name tags.\n" +
            "  -f compounds all previous flags.\n" +
            "  -r also destroys armor stands.\n" +
            "  -l currently does nothing.",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.butcher")
    @Logging(PLACEMENT)
    public void butcher(Actor actor, CommandContext args) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();
        Player player = actor instanceof Player ? (Player) actor : null;

        // technically the default can be larger than the max, but that's not my problem
        int radius = config.butcherDefaultRadius;

        // there might be a better way to do this but my brain is fried right now
        if (args.argsLength() > 0) { // user inputted radius, override the default
            radius = args.getInteger(0);
            if (radius < -1) {
                actor.printError("Use -1 to remove all mobs in loaded chunks");
                return;
            }
            if (config.butcherMaxRadius != -1) { // clamp if there is a max
                if (radius == -1) {
                    radius = config.butcherMaxRadius;
                } else { // Math.min does not work if radius is -1 (actually highest possible value)
                    radius = Math.min(radius, config.butcherMaxRadius);
                }
            }
        }

        CreatureButcher flags = new CreatureButcher(actor);
        flags.fromCommand(args);

        List<EntityVisitor> visitors = new ArrayList<EntityVisitor>();
        LocalSession session = null;
        EditSession editSession = null;

        if (player != null) {
            session = worldEdit.getSessionManager().get(player);
            Vector center = session.getPlacementPosition(player);
            editSession = session.createEditSession(player);
            List<? extends Entity> entities;
            if (radius >= 0) {
                CylinderRegion region = CylinderRegion.createRadius(editSession, center, radius);
                entities = editSession.getEntities(region);
            } else {
                entities = editSession.getEntities();
            }
            visitors.add(new EntityVisitor(entities.iterator(), flags.createFunction(editSession.getWorld().getWorldData().getEntityRegistry())));
        } else {
            Platform platform = worldEdit.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
            for (World world : platform.getWorlds()) {
                List<? extends Entity> entities = world.getEntities();
                visitors.add(new EntityVisitor(entities.iterator(), flags.createFunction(world.getWorldData().getEntityRegistry())));
            }
        }

        int killed = 0;
        for (EntityVisitor visitor : visitors) {
            Operations.completeLegacy(visitor);
            killed += visitor.getAffected();
        }

        BBC.KILL_SUCCESS.send(actor, killed, radius);

        if (editSession != null) {
            session.remember(editSession);
            editSession.flushQueue();
        }
    }

    @Command(
        aliases = { "remove", "rem", "rement" },
        usage = "<type> <radius>",
        desc = "Remove all entities of a type",
        min = 2,
        max = 2
    )
    @CommandPermissions("worldedit.remove")
    @Logging(PLACEMENT)
    public void remove(Actor actor, CommandContext args) throws WorldEditException, CommandException {
        String typeStr = args.getString(0);
        int radius = args.getInteger(1);
        Player player = actor instanceof Player ? (Player) actor : null;

        if (radius < -1) {
            actor.printError("Use -1 to remove all entities in loaded chunks");
            return;
        }

        EntityRemover remover = new EntityRemover();
        remover.fromString(typeStr);

        List<EntityVisitor> visitors = new ArrayList<EntityVisitor>();
        LocalSession session = null;
        EditSession editSession = null;

        if (player != null) {
            session = worldEdit.getSessionManager().get(player);
            Vector center = session.getPlacementPosition(player);
            editSession = session.createEditSession(player);
            List<? extends Entity> entities;
            if (radius >= 0) {
                CylinderRegion region = CylinderRegion.createRadius(editSession, center, radius);
                entities = editSession.getEntities(region);
            } else {
                entities = editSession.getEntities();
            }
            visitors.add(new EntityVisitor(entities.iterator(), remover.createFunction(editSession.getWorld().getWorldData().getEntityRegistry())));
        } else {
            Platform platform = worldEdit.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
            for (World world : platform.getWorlds()) {
                List<? extends Entity> entities = world.getEntities();
                visitors.add(new EntityVisitor(entities.iterator(), remover.createFunction(world.getWorldData().getEntityRegistry())));
            }
        }

        int removed = 0;
        for (EntityVisitor visitor : visitors) {
            Operations.completeLegacy(visitor);
            removed += visitor.getAffected();
        }

        BBC.KILL_SUCCESS.send(actor, removed, radius);

        if (editSession != null) {
            session.remember(editSession);
            editSession.flushQueue();
        }
    }

    @Command(
        aliases = { "/calc", "/calculate", "/eval", "/evaluate", "/solve" },
        usage = "<expression>",
        desc = "Evaluate a mathematical expression"
    )
    @CommandPermissions("worldedit.calc")
    public void calc(final Actor actor, @Text String input) throws CommandException {
        try {
            FaweLimit limit = FawePlayer.wrap(actor).getLimit();
            final Expression expression = Expression.compile(input);
            final AtomicDouble result = new AtomicDouble(Double.NaN);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.invokeAll(Arrays.asList(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        result.set(expression.evaluate());
                        return null;
                    }
                }), limit.MAX_EXPRESSION_MS, TimeUnit.MILLISECONDS); // Default timeout of 50 milliseconds to prevent abuse.
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executor.shutdown();
            actor.print(BBC.getPrefix() + "= " + result);
        } catch (EvaluationException e) {
            actor.printError(String.format(
                    "'%s' could not be parsed as a valid expression", input));
        } catch (ExpressionException e) {
            actor.printError(String.format(
                    "'%s' could not be evaluated (error: %s)", input, e.getMessage()));
        }
    }

    @Command(
        aliases = { "/help" },
        usage = "[<command>]",
        desc = "Displays help for WorldEdit commands",
        min = 0,
        max = -1
    )
    @CommandPermissions("worldedit.help")
    public void help(Actor actor, CommandContext args) throws WorldEditException {
        help(args, worldEdit, actor);
    }

    private static CommandMapping detectCommand(Dispatcher dispatcher, String command, boolean isRootLevel) {
        CommandMapping mapping;

        // First try the command as entered
        mapping = dispatcher.get(command);
        if (mapping != null) {
            return mapping;
        }

        // Then if we're looking at root commands and the user didn't use
        // any slashes, let's try double slashes and then single slashes.
        // However, be aware that there exists different single slash
        // and double slash commands in WorldEdit
        if (isRootLevel && !command.contains("/")) {
            mapping = dispatcher.get("//" + command);
            if (mapping != null) {
                return mapping;
            }

            mapping = dispatcher.get("/" + command);
            if (mapping != null) {
                return mapping;
            }
        }

        return null;
    }

    public static void list(File dir, Actor actor, CommandContext args, int page, String formatName, boolean playerFolder) {
        List<File> fileList = new ArrayList<>();
        int len = args.argsLength();
        List<String> filters = new ArrayList<>();
        boolean mine = false;
        if (len > 0) {
            int max = len;
            if (MathMan.isInteger(args.getString(len - 1))) {
                page = args.getInteger(--len);
            }
            for (int i = 0; i < len; i++) {
                switch (args.getString(i).toLowerCase()) {
                    case "me":
                    case "mine":
                        mine = true;
                        break;
                    default:
                        filters.add(args.getString(i));
                        break;
                }
            }
        }
        if (playerFolder) {
            File playerDir = new File(dir, actor.getUniqueId().toString());
            if (playerDir.exists()) {
                fileList.addAll(allFiles(playerDir, true));
            }
            if (!mine) {
                fileList.addAll(allFiles(dir, false));
            }
        } else {
            fileList.addAll(allFiles(dir, true));
        }
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                fileList.removeIf(file -> !file.getPath().contains(filter));
            }
        }
        if (fileList.isEmpty()) {
            BBC.SCHEMATIC_NONE.send(actor);
            return;
        }
        if (formatName != null) {
            final ClipboardFormat cf = ClipboardFormat.findByAlias(formatName);
            fileList = fileList.stream()
                    .filter(file -> cf.isFormat(file))
                    .collect(Collectors.toList());

        }
        File[] files = new File[fileList.size()];
        fileList.toArray(files);
        final int perPage = actor instanceof Player ? 8 : 20; // More pages for console
        int pageCount = (files.length + perPage - 1) / perPage;
        if (page < 1) {
            BBC.SCHEMATIC_PAGE.send(actor, ">0");
            return;
        }
        if (page > pageCount) {
            BBC.SCHEMATIC_PAGE.send(actor, "<" + (pageCount + 1));
            return;
        }

        final int sortType = args.hasFlag('d') ? -1 : args.hasFlag('n') ? 1 : 0;
        // cleanup file list
        Arrays.sort(files, new Comparator<File>(){
            @Override
            public int compare(File f1, File f2) {
                int res;
                if (sortType == 0) { // use name by default
                    int p = f1.getParent().compareTo(f2.getParent());
                    if (p == 0) { // same parent, compare names
                        res = f1.getName().compareTo(f2.getName());
                    } else { // different parent, sort by that
                        res = p;
                    }
                } else {
                    res = Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()); // use date if there is a flag
                    if (sortType == 1) res = -res; // flip date for newest first instead of oldest first
                }
                return res;
            }
        });

        List<String> schematics = listFiles(dir, files, playerFolder ? actor.getUniqueId() : null);
        int offset = (page - 1) * perPage;

        StringBuilder build = new StringBuilder();
        int limit = Math.min(offset + perPage, schematics.size());
        for (int i = offset; i < limit;) {
            build.append(schematics.get(i));
            if (++i != limit) {
                build.append("\n");
            }
        }
        String heading = BBC.SCHEMATIC_LIST.f(page, pageCount);
        actor.print(BBC.getPrefix() + heading + "\n" + build.toString());
    }


    private static List<File> allFiles(File root, boolean recursive) {
        File[] files = root.listFiles();
        if (files == null) return new ArrayList<>();
        List<File> fileList = new ArrayList<File>();
        for (File f : files) {
            if (f.isDirectory()) {
                if (recursive) {
                    List<File> subFiles = allFiles(f, recursive);
                    if (subFiles == null || subFiles.isEmpty()) continue; // empty subdir
                    fileList.addAll(subFiles);
                }
            } else {
                fileList.add(f);
            }
        }
        return fileList;
    }

    private static List<String> listFiles(File root, File[] files, UUID uuid) {
        File dir;
        if (uuid != null) {
            dir = new File(root, uuid.toString());
        } else {
            dir = root;
        }
        List<String> result = new ArrayList<String>();
        for (File file : files) {
            ClipboardFormat format = ClipboardFormat.findByFile(file);
            URI relative = dir.toURI().relativize(file.toURI());
            String name = "";
            if (relative.isAbsolute()) {
                relative = root.toURI().relativize(file.toURI());
                name += "../";
            }
            name += relative.getPath();
            String formatName;
            if (format == null) {
                String[] split = file.getName().split("\\.");
                formatName = split.length > 1 ? split[split.length - 1].toUpperCase() : "Unknown";
            } else {
                formatName = format.toString();
            }
            result.add(BBC.SCHEMATIC_LIST_ELEM.f(name, formatName));
        }
        return result;
    }

    public static void help(CommandContext args, WorldEdit we, Actor actor) {
        help(args, we, actor, "/", null);
    }

    public static void help(CommandContext args, WorldEdit we, Actor actor, String prefix, CommandCallable callable) {
        try {
            if (callable == null) {
                callable = we.getPlatformManager().getCommandManager().getDispatcher();
            }
            CommandLocals locals = args.getLocals();

            int page = -1;
            String category = null;
            final int perPage = actor instanceof Player ? 8 : 20; // More pages for console
            int effectiveLength = args.argsLength();

            // Detect page from args
            try {
                if (effectiveLength > 0) {
                    page = args.getInteger(args.argsLength() - 1);
                    if (page <= 0) {
                        page = 1;
                    } else {
                        page--;
                    }
                    effectiveLength--;
                }
            } catch (NumberFormatException ignored) {
            }

            boolean isRootLevel = true;
            List<String> visited = new ArrayList<String>();

            // Create the message
            if (callable instanceof Dispatcher) {
                Dispatcher dispatcher = (Dispatcher) callable;

                // Get a list of aliases
                List<CommandMapping> aliases = new ArrayList<CommandMapping>(dispatcher.getCommands());
                // Group by callable

                if (page == -1 || effectiveLength > 0) {
                    Map<String, Set<CommandMapping>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    for (CommandMapping mapping : aliases) {
                        CommandCallable c = mapping.getCallable();
                        String group;
                        if (c instanceof DelegateCallable) {
                            c = ((DelegateCallable) c).getParent();
                        }
                        if (c instanceof ParametricCallable) {
                            Object obj = ((ParametricCallable) c).getObject();
                            Command command = obj.getClass().getAnnotation(Command.class);
                            if (command != null && command.aliases().length != 0) {
                                group = command.aliases()[0];
                            } else {
                                group = obj.getClass().getSimpleName().replaceAll("Commands", "").replaceAll("Util$", "");
                            }
                        } else if (c instanceof Dispatcher) {
                            group = mapping.getPrimaryAlias();
                        } else {
                            group = "Unsorted";
                        }
                        group = group.replace("/", "");
                        group = StringMan.toProperCase(group);
                        Set<CommandMapping> queue = grouped.get(group);
                        if (queue == null) {
                            queue = new LinkedHashSet<>();
                            grouped.put(group, queue);
                        }
                        if (c instanceof Dispatcher) {
                            queue.addAll(((Dispatcher) c).getCommands());
                        } else {
                            queue.add(mapping);
                        }
                    }
                    if (effectiveLength > 0) {
                        String cat = args.getString(0);
                        Set<CommandMapping> mappings = effectiveLength == 1 ? grouped.get(cat) : null;
                        if (mappings == null) {
                            // Drill down to the command
                            for (int i = 0; i < effectiveLength; i++) {
                                String command = args.getString(i);

                                if (callable instanceof Dispatcher) {
                                    // Chop off the beginning / if we're are the root level
                                    if (isRootLevel && command.length() > 1 && command.charAt(0) == '/') {
                                        command = command.substring(1);
                                    }

                                    CommandMapping mapping = detectCommand((Dispatcher) callable, command, isRootLevel);
                                    if (mapping != null) {
                                        callable = mapping.getCallable();
                                    } else {
                                        if (isRootLevel) {
                                            Set<String> found = new HashSet<>();
                                            String arg = args.getString(i).toLowerCase();
                                            String closest = null;
                                            int distance = Integer.MAX_VALUE;
                                            for (CommandMapping map : aliases) {
                                                String desc = map.getDescription().getDescription();
                                                if (desc == null) desc = map.getDescription().getHelp();
                                                if (desc == null) desc = "";
                                                String[] descSplit = desc.replaceAll("[^A-Za-z0-9]", "").toLowerCase().split(" ");
                                                for (String alias : map.getAllAliases()) {
                                                    if (alias.equals(arg)) {
                                                        closest = map.getPrimaryAlias();
                                                        distance = 0;
                                                        found.add(map.getPrimaryAlias());
                                                    } else if (alias.contains(arg)) {
                                                        closest = map.getPrimaryAlias();
                                                        distance = 1;
                                                        found.add(map.getPrimaryAlias());
                                                    } else if (StringMan.isEqualIgnoreCaseToAny(arg, descSplit)) {
                                                        closest = map.getPrimaryAlias();
                                                        distance = 1;
                                                        found.add(map.getPrimaryAlias());
                                                    } else {
                                                        int currentDist = StringMan.getLevenshteinDistance(alias, arg);
                                                        if (currentDist < distance) {
                                                            distance = currentDist;
                                                            closest = map.getPrimaryAlias();
                                                        }
                                                    }
                                                }
                                            }
                                            found.add(closest);
                                            BBC.HELP_SUGGEST.send(actor, arg, StringMan.join(found, ", "));
                                            return;
                                        } else {
                                            actor.printError(String.format("The sub-command '%s' under '%s' could not be found.",
                                                    command, Joiner.on(" ").join(visited)));
                                            return;
                                        }
                                    }
                                    visited.add(args.getString(i));
                                    isRootLevel = false;
                                } else {
                                    actor.printError(String.format("'%s' has no sub-commands. (Maybe '%s' is for a parameter?)",
                                            Joiner.on(" ").join(visited), command));
                                    return;
                                }
                            }
                            if (!(callable instanceof Dispatcher)) {
                                actor.printRaw(BBC.getPrefix() + ColorCodeBuilder.asColorCodes(new CommandUsageBox(callable, Joiner.on(" ").join(visited))));
                                return;
                            }
                            dispatcher = (Dispatcher) callable;
                            aliases = new ArrayList<CommandMapping>(dispatcher.getCommands());
                        } else {
                            aliases = new ArrayList<>(mappings);
                        }
                        page = Math.max(0, page);
                    } else if (grouped.size() > 1) {
                        StringBuilder message = new StringBuilder();
                        message.append(BBC.getPrefix() + BBC.HELP_HEADER_CATEGORIES.s() + "\n");
                        StringBuilder builder = new StringBuilder();
                        boolean first = true;
                        for (Map.Entry<String, Set<CommandMapping>> entry : grouped.entrySet()) {
                            String s1 = "&a//help " + entry.getKey();
                            String s2 = entry.getValue().size() + "";
                            message.append(BBC.HELP_ITEM_ALLOWED.format(s1, s2) + "\n");
                        }
                        message.append(BBC.HELP_HEADER_FOOTER.s());
                        actor.print(BBC.color(message.toString()));
                        return;
                    }
                }
//            else
                {
                    Collections.sort(aliases, new PrimaryAliasComparator(CommandManager.COMMAND_CLEAN_PATTERN));

                    // Calculate pagination
                    int offset = perPage * Math.max(0, page);
                    int pageTotal = (int) Math.ceil(aliases.size() / (double) perPage);

                    // Box
                    StringBuilder message = new StringBuilder();

                    if (offset >= aliases.size()) {
                        message.append("&c").append(String.format("There is no page %d (total number of pages is %d).", page + 1, pageTotal));
                    } else {
                        message.append(BBC.getPrefix() + BBC.HELP_HEADER.format(page + 1, pageTotal) + "\n");
                        List<CommandMapping> list = aliases.subList(offset, Math.min(offset + perPage, aliases.size()));

                        boolean first = true;
                        // Add each command
                        for (CommandMapping mapping : list) {
                            CommandCallable c = mapping.getCallable();
                            StringBuilder s1 = new StringBuilder();
                            s1.append(prefix);
                            if (!visited.isEmpty()) {
                                s1.append(Joiner.on(" ").join(visited));
                                s1.append(" ");
                            }
                            s1.append(mapping.getPrimaryAlias());
                            String s2 = mapping.getDescription().getDescription();
                            if (c.testPermission(locals)) {
                                message.append(BBC.HELP_ITEM_ALLOWED.format(s1, s2) + "\n");
                            } else {
                                message.append(BBC.HELP_ITEM_DENIED.format(s1, s2) + "\n");
                            }
                        }
                        message.append(BBC.HELP_HEADER_FOOTER.f());
                    }
                    actor.print(BBC.color(message.toString()));
                }
            } else {
                actor.printRaw(BBC.getPrefix() + ColorCodeBuilder.asColorCodes(new CommandUsageBox(callable, Joiner.on(" ").join(visited))));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Class<UtilityCommands> inject() {
        return UtilityCommands.class;
    }
}
